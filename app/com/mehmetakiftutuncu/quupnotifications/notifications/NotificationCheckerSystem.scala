package com.mehmetakiftutuncu.quupnotifications.notifications

import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationCheckerActor.CheckNotifications
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.inject.ApplicationLifecycle

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
case class NotificationCheckerSystem @Inject() (applicationLifecycle: ApplicationLifecycle,
                                                conf: ConfBase,
                                                database: Database,
                                                fcmClient: FCMClient,
                                                quupClient: QuupClient) extends NotificationCheckerSystemBase

@ImplementedBy(classOf[NotificationCheckerSystem])
trait NotificationCheckerSystemBase extends Loggable {
  protected val applicationLifecycle: ApplicationLifecycle
  protected val conf: ConfBase
  protected val database: DatabaseBase
  protected val fcmClient: FCMClientBase
  protected val quupClient: QuupClientBase

  Log.warn("Starting NotificationCheckerSystem...")

  val system: ActorSystem = ActorSystem("notification-checker-system")
  val actor: ActorRef     = system.actorOf(Props(new NotificationCheckerActor(database, fcmClient, quupClient)), NotificationCheckerActor.actorName)

  private val cancellable: Cancellable = system.scheduler.schedule(
    conf.Notifications.interval,
    conf.Notifications.interval,
    actor,
    CheckNotifications
  )

  applicationLifecycle.addStopHook {
    () =>
      Log.warn("Shutting down NotificationCheckerSystem...")

      cancellable.cancel()
      system.terminate()
  }
}
