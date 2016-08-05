package com.mehmetakiftutuncu.quupnotifications.notifications

import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import com.google.inject.{ImplementedBy, Inject}
import com.mehmetakiftutuncu.quupnotifications.firebase.{CloudMessagingClient, CloudMessagingClientBase}
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationCheckerActor.CheckNotifications
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext.Implicits.global

case class NotificationCheckerSystem @Inject()(ApplicationLifecycle: ApplicationLifecycle,
                                               Conf: ConfBase,
                                               CloudMessagingClient: CloudMessagingClient,
                                               QuupClient: QuupClient,
                                               Registrations: RegistrationsBase) extends NotificationCheckerSystemBase

@ImplementedBy(classOf[NotificationCheckerSystem])
trait NotificationCheckerSystemBase extends Loggable {
  protected val ApplicationLifecycle: ApplicationLifecycle
  protected val Conf: ConfBase
  protected val CloudMessagingClient: CloudMessagingClientBase
  protected val QuupClient: QuupClientBase
  protected val Registrations: RegistrationsBase

  Log.warn("Starting NotificationCheckerSystem...")

  val system: ActorSystem = ActorSystem("notification-checker-system")
  val actor: ActorRef     = system.actorOf(Props(new NotificationCheckerActor(CloudMessagingClient, QuupClient, Registrations)), NotificationCheckerActor.actorName)

  private val cancellable: Cancellable = system.scheduler.schedule(
    Conf.Notifications.interval,
    Conf.Notifications.interval,
    actor,
    CheckNotifications
  )

  ApplicationLifecycle.addStopHook {
    () =>
      Log.warn("Shutting down NotificationCheckerSystem...")

      cancellable.cancel()
      system.terminate()
  }
}
