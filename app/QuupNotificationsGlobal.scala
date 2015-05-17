import akka.actor.Props
import models.Data
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.{Application, GlobalSettings, Logger}
import utilities.TickActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object QuupNotificationsGlobal extends GlobalSettings {
  override def onStart(app: Application) {
    Logger.info("Starting quup Notifications Server...")

    Data.load()

    val tickActor = Akka.system(app).actorOf(Props[TickActor], "tickActor")
    Akka.system(app).scheduler.schedule(5.seconds, 5.minutes, tickActor, "Tick!")
  }

  override def onStop(app: Application) {
    Logger.info("Stopping quup Notifications Server...")

    Data.save()

    Akka.system(app).shutdown()
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(NotFound("Not found!"))
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(BadRequest("Bad Request! " + error))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError("An error occurred! " + ex.getMessage))
  }
}