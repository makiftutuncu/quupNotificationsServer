package com.mehmetakiftutuncu.quupnotifications.notifications

import java.util.concurrent.TimeUnit

import akka.actor._
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.{Maybe, _}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationCheckerActor.CheckNotifications
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationPusherActor.PushNotifications
import com.mehmetakiftutuncu.quupnotifications.utilities._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

class NotificationCheckerActor(private val conf: Conf,
                               private val database: Database,
                               private val gcmClient: GCMClient,
                               private val quupClient: QuupClient,
                               private val registrationId: String) extends Actor with Loggable {
  private var cancellable: Option[Cancellable] = None

  override def preStart(): Unit = {
    Log.debug(s"""Scheduling actor for registration id "$registrationId"...""")

    cancellable = Option(context.system.scheduler.scheduleOnce(conf.Notifications.interval, self, CheckNotifications))
  }

  override def receive: Receive = {
    case NotificationCheckerActor.CheckNotifications =>
      checkNotifications()

    case m @ _ =>
      Log.error("Failed to check notifications!", Errors(CommonError.invalidData.reason("Received unknown message!").data(m.toString)))
  }


  override def postStop(): Unit = {
    Log.debug(s"""Cancelling actor for registration id "$registrationId"...""")

    cancellable.foreach(_.cancel())
    cancellable = None
  }

  def checkNotifications(): Unit = {
    Log.debug(s"""Checking notifications for registration id "$registrationId"...""")

    val maybeRegistration: Maybe[Registration] = Registration.getRegistration(database, registrationId)

    if (maybeRegistration.hasValue) {
      val registration: Registration = maybeRegistration.value

      quupClient.getNotifications(registration).map {
        maybeNotificationList: Maybe[List[Notification]] =>
          if (maybeNotificationList.hasValue) {
            val notifications: List[Notification] = maybeNotificationList.value.filter(_.when > registration.lastNotification)

            NotificationPusherActor.create(context, conf, database, gcmClient, registration) ! PushNotifications(notifications)
          }
      }.recover {
        case t: Throwable =>
          Log.error("Failed to check notifications with exception!", t)
      }
    }
  }
}

object NotificationCheckerActor extends Loggable {
  def name(registrationId: String): String = s"$registrationId-checker"

  def create(factory: ActorRefFactory, conf: Conf, database: Database, gcmClient: GCMClient, quupClient: QuupClient, registrationId: String): ActorRef = {
    factory.actorOf(Props[NotificationCheckerActor](new NotificationCheckerActor(conf, database, gcmClient, quupClient, registrationId)), name(registrationId))
  }

  def cancel(system: ActorSystem, registrationId: String): Unit = {
    system.actorSelection(name(registrationId)).resolveOne(FiniteDuration(1, TimeUnit.SECONDS)).map {
      actor: ActorRef =>
        system.stop(actor)
    }
  }

  case object CheckNotifications
}
