package utilities

import akka.actor.Actor
import models.{GCM, Request}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

class TickActor extends Actor {
  override def receive: Receive = {
    case _ =>
      Logger.info("Tick! Wake up beauty.")
      Request.url("https://quupnotifications.herokuapp.com").get() map {
        ws =>
          // Do this in the map to be sure request got a result and server is up
          GCM.pushAll()
      } recover {
        case t: Throwable =>
          Logger.error("Tick failed!", t)
      }
  }
}
