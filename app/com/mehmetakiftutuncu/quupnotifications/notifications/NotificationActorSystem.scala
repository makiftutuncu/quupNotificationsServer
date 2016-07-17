package com.mehmetakiftutuncu.quupnotifications.notifications

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import com.mehmetakiftutuncu.quupnotifications.utilities.{Log, Loggable}
import play.api.inject.ApplicationLifecycle

@Singleton
class NotificationActorSystem @Inject() (applicationLifecycle: ApplicationLifecycle) extends Loggable {
  lazy val system: ActorSystem = ActorSystem("notification-system")

  applicationLifecycle.addStopHook {
    () =>
      Log.debug("Shutting down NotificationActorSystem...")

      system.terminate()
  }
}
