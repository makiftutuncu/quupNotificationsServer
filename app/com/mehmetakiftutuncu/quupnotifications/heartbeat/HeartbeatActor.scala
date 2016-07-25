package com.mehmetakiftutuncu.quupnotifications.heartbeat

import akka.actor.Actor
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.heartbeat.HeartbeatActor.Beep
import com.mehmetakiftutuncu.quupnotifications.utilities.{ConfBase, Log, Loggable}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
case class HeartbeatActor @Inject() (conf: ConfBase, wsClient: WSClient) extends HeartbeatActorBase

@ImplementedBy(classOf[HeartbeatActor])
trait HeartbeatActorBase extends Actor with Loggable {
  protected val conf: ConfBase
  protected val wsClient: WSClient

  override def receive: Receive = {
    case Beep =>
      wsClient.url(conf.Heartbeat.host).withRequestTimeout(conf.Common.wsTimeout).get().map {
        wsResponse: WSResponse =>
          Log.debug("I am still alive!")
      }

    case m @ _ =>
      Log.error("Heartbeat failed!", Errors(CommonError.invalidData.reason("Received unknown message!").data(m.toString)))
  }
}

object HeartbeatActor {
  val actorName: String = "heartbeat"

  case object Beep
}
