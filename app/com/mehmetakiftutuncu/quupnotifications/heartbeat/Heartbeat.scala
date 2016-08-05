package com.mehmetakiftutuncu.quupnotifications.heartbeat

import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import com.google.inject.{ImplementedBy, Inject}
import com.mehmetakiftutuncu.quupnotifications.heartbeat.HeartbeatActor.Beep
import com.mehmetakiftutuncu.quupnotifications.utilities.{ConfBase, Log, Loggable}
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global

case class Heartbeat @Inject()(ActorSystem: ActorSystem,
                               ApplicationLifecycle: ApplicationLifecycle,
                               Conf: ConfBase,
                               WSClient: WSClient) extends HeartbeatBase

@ImplementedBy(classOf[Heartbeat])
trait HeartbeatBase extends Loggable {
  protected val ActorSystem: ActorSystem
  protected val ApplicationLifecycle: ApplicationLifecycle
  protected val Conf: ConfBase
  protected val WSClient: WSClient

  Log.warn("Starting Heartbeat...")

  val actor: ActorRef = ActorSystem.actorOf(Props(new HeartbeatActor(Conf, WSClient)), HeartbeatActor.actorName)

  private val cancellable: Cancellable = ActorSystem.scheduler.schedule(
    Conf.Heartbeat.interval,
    Conf.Heartbeat.interval,
    actor,
    Beep
  )

  ApplicationLifecycle.addStopHook {
    () =>
      Log.warn("Shutting down Heartbeat...")

      cancellable.cancel()
      ActorSystem.terminate()
  }
}
