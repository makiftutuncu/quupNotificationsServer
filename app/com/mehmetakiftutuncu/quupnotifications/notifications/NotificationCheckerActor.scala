package com.mehmetakiftutuncu.quupnotifications.notifications

import akka.actor.Actor
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.google.inject.{ImplementedBy, Inject}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.{Maybe, _}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import com.mehmetakiftutuncu.quupnotifications.utilities._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class NotificationCheckerActor @Inject() (database: DatabaseBase,
                                               fcmClient: FCMClientBase,
                                               quupClient: QuupClientBase) extends NotificationCheckerActorBase

@ImplementedBy(classOf[NotificationCheckerActor])
trait NotificationCheckerActorBase extends Actor with Loggable {
  protected val database: DatabaseBase
  protected val fcmClient: FCMClientBase
  protected val quupClient: QuupClientBase

  override def receive: Receive = {
    case NotificationCheckerActor.CheckNotifications =>
      checkNotifications()

    case m @ _ =>
      Log.error("Failed to check notifications!", Errors(CommonError.invalidData.reason("Received unknown message!").data(m.toString)))
  }


  def checkNotifications(): Unit = {
    val maybeRegistrations: Maybe[List[Registration]] = Registration.getRegistrations(database)

    if (maybeRegistrations.hasValue) {
      val registrations: List[Registration] = maybeRegistrations.value

      val resultingErrorsFutureList: List[Future[Errors]] = registrations.map {
        registration: Registration =>
          Log.debug(s"""Checking notifications for registration id "${registration.registrationId}"...""")

          quupClient.getNotifications(registration).flatMap {
            maybeNotificationList: Maybe[List[Notification]] =>
              if (maybeNotificationList.hasValue) {
                val notifications: List[Notification] = maybeNotificationList.value.filter(_.when > registration.lastNotification)

                if (notifications.nonEmpty) {
                  fcmClient.push(registration, notifications).map {
                    pushErrors: Errors =>
                      if (pushErrors.isEmpty) {
                        Log.warn(s"""Pushed ${notifications.size} notifications to registration id "${registration.registrationId}"!""")

                        Registration.saveRegistration(database, registration.copy(lastNotification = notifications.head.when))
                      } else {
                        Errors.empty
                      }
                  }
                } else {
                  Future.successful(Errors.empty)
                }
              } else {
                Future.successful(maybeNotificationList.errors)
              }
          }.recover {
            case t: Throwable =>
              Log.error(s"""Failed to check notifications for registration id "${registration.registrationId}" with exception!""", t)
              Errors(CommonError.requestFailed)
          }
      }

      Future.sequence(resultingErrorsFutureList).map {
        resultingErrorsList: List[Errors] =>
          Log.debug(s"""Checking notifications completed for "${resultingErrorsList.size}" registrations, "${resultingErrorsList.count(_.isEmpty)}" of them succeeded, "${resultingErrorsList.count(_.hasErrors)}" of them failed.""")
      }
    }
  }
}

object NotificationCheckerActor extends Loggable {
  val actorName: String = "notification-checker-actor"

  case object CheckNotifications
}
