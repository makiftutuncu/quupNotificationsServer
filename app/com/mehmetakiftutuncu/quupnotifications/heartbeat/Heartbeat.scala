package com.mehmetakiftutuncu.quupnotifications.heartbeat

import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import com.google.inject.{ImplementedBy, Inject}
import com.mehmetakiftutuncu.quupnotifications.heartbeat.HeartbeatActor.Beep
import com.mehmetakiftutuncu.quupnotifications.utilities.{ConfBase, Log, Loggable}
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global

case class Heartbeat @Inject() (actorSystem: ActorSystem,
                                applicationLifecycle: ApplicationLifecycle,
                                conf: ConfBase,
                                wsClient: WSClient) extends HeartbeatBase

@ImplementedBy(classOf[Heartbeat])
trait HeartbeatBase extends Loggable {
  protected val actorSystem: ActorSystem
  protected val applicationLifecycle: ApplicationLifecycle
  protected val conf: ConfBase
  protected val wsClient: WSClient

  Log.warn("Starting Heartbeat...")

  val actor: ActorRef = actorSystem.actorOf(Props(new HeartbeatActor(conf, wsClient)), HeartbeatActor.actorName)

  private val cancellable: Cancellable = actorSystem.scheduler.schedule(
    conf.Heartbeat.interval,
    conf.Heartbeat.interval,
    actor,
    Beep
  )

  applicationLifecycle.addStopHook {
    () =>
      Log.warn("Shutting down Heartbeat...")

      cancellable.cancel()
      actorSystem.terminate()
  }
}
