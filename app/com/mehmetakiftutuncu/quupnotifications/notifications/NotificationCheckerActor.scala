package com.mehmetakiftutuncu.quupnotifications.notifications

import akka.actor.Actor
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.google.inject.{ImplementedBy, Inject}
import com.mehmetakiftutuncu.quupnotifications.firebase.CloudMessagingClientBase
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import com.mehmetakiftutuncu.quupnotifications.utilities._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class NotificationCheckerActor @Inject()(CloudMessagingClient: CloudMessagingClientBase,
                                              QuupClient: QuupClientBase,
                                              Registrations: RegistrationsBase) extends NotificationCheckerActorBase

@ImplementedBy(classOf[NotificationCheckerActor])
trait NotificationCheckerActorBase extends Actor with Loggable {
  protected val CloudMessagingClient: CloudMessagingClientBase
  protected val QuupClient: QuupClientBase
  protected val Registrations: RegistrationsBase

  override def receive: Receive = {
    case NotificationCheckerActor.CheckNotifications =>
      checkNotifications()

    case m @ _ =>
      Log.error("Failed to check notifications!", Errors(CommonError.invalidData.reason("Received unknown message!").data(m.toString)))
  }

  private def checkNotifications(): Unit = {
    Registrations.getAll.map {
      maybeRegistrations: Maybe[List[Registration]] =>
        if (maybeRegistrations.hasValue) {
          val registrations: List[Registration] = maybeRegistrations.value

          val resultingErrorsFutureList: List[Future[Errors]] = registrations.map {
            registration: Registration =>
              Log.debug(s"""Checking notifications for registration id "${registration.registrationId}"...""")

              QuupClient.getNotifications(registration).flatMap {
                maybeNotificationList: Maybe[List[Notification]] =>
                  if (maybeNotificationList.hasValue) {
                    val notifications: List[Notification] = maybeNotificationList.value.filter(_.when > registration.lastNotification)

                    if (notifications.nonEmpty) {
                      push(registration, notifications)
                    } else {
                      Future.successful(Errors.empty)
                    }
                  } else {
                    Future.successful(maybeNotificationList.errors)
                  }
              }.recover {
                case t: Throwable =>
                  val errors: Errors = Errors(CommonError.requestFailed.reason(t.getMessage))

                  Log.error(s"""Failed to check notifications for registration id "${registration.registrationId}" with exception!""", errors, t)

                  errors
              }
          }

          Future.sequence(resultingErrorsFutureList).map {
            resultingErrorsList: List[Errors] =>
              Log.debug(s"""Checking notifications completed for "${resultingErrorsList.size}" registrations, "${resultingErrorsList.count(_.isEmpty)}" of them succeeded, "${resultingErrorsList.count(_.hasErrors)}" of them failed.""")
          }
        }
    }
  }

  private def push(registration: Registration, notifications: List[Notification]): Future[Errors] = {
    CloudMessagingClient.push(registration, notifications).flatMap {
      pushErrors: Errors =>
        if (pushErrors.isEmpty) {
          Log.warn(s"""Pushed ${notifications.size} notifications to registration id "${registration.registrationId}"!""")

          Registrations.save(registration.copy(lastNotification = notifications.head.when))
        } else {
          Future.successful(Errors.empty)
        }
    }
  }
}

object NotificationCheckerActor extends Loggable {
  val actorName: String = "notification-checker-actor"

  case object CheckNotifications
}
