package com.mehmetakiftutuncu.quupnotifications.notifications

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props}
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationCheckerActor.CheckNotifications
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationPusherActor.PushNotifications
import com.mehmetakiftutuncu.quupnotifications.utilities._

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationPusherActor(private val conf: Conf,
                              private val database: Database,
                              private val gcmClient: GCMClient,
                              private val registration: Registration) extends Actor with Loggable {
  override def receive: Receive = {
    case PushNotifications(notifications) =>
      val s: ActorRef = sender

      pushNotifications(notifications, s)

    case m @ _ =>
      Log.error("Failed to push notifications!", Errors(CommonError.invalidData.reason("Received unknown message!").data(m.toString)))
  }

  def pushNotifications(notifications: List[Notification], sender: ActorRef): Unit = {
    if (notifications.nonEmpty) {
      gcmClient.push(registration, notifications).map {
        pushErrors: Errors =>
          if (pushErrors.isEmpty) {
            Log.warn(s"""Pushed ${notifications.size} notifications to registration id "${registration.registrationId}"!""")

            Registration.saveRegistration(database, registration.copy(lastNotification = notifications.head.when))
          }
      }
    }

    context.system.scheduler.scheduleOnce(conf.Notifications.interval) {
      sender ! CheckNotifications
    }
    self ! PoisonPill
  }
}

object NotificationPusherActor {
  def name(registration: Registration): String = s"${registration.registrationId}-pusher"

  def create(factory: ActorRefFactory, conf: Conf, database: Database, gcmClient: GCMClient, registration: Registration): ActorRef = {
    factory.actorOf(Props[NotificationPusherActor](new NotificationPusherActor(conf, database, gcmClient, registration)), name(registration))
  }

  case class PushNotifications(notifications: List[Notification])
}
