package utilities

import akka.actor.Actor
import models.Notifications
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global

class TickActor extends Actor {
  override def receive: Receive = {
    case _ =>
      Logger.info("Tick! Wake up beauty.")
      WS.url("https://quupnotifications.herokuapp.com").get() map {
        ws =>
          // Do this in the map to be sure request got a result and server is up
          Notifications.push()
      }
  }
}
