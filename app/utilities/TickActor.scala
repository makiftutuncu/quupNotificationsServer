package utilities

import akka.actor.Actor
import play.api.Logger
import play.api.libs.ws.WS

class TickActor extends Actor {
  override def receive: Receive = {
    case _ =>
      Logger.info("Tick! Wake up beauty.")
      WS.url("https://quupnotifications.herokuapp.com").get()
  }
}
