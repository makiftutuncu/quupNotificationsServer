import akka.actor.Props
import play.api.libs.concurrent.Akka
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import play.api.{Application, GlobalSettings, Logger}
import utilities.{Conf, TickActor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object QuupNotificationsGlobal extends GlobalSettings {
  override def onStart(app: Application) {
    Logger.info("Starting quup Notifications Server...")

    val tickActor = Akka.system(app).actorOf(Props[TickActor], "tickActor")
    Akka.system(app).scheduler.schedule(Conf.tickInitialDelay, Conf.tickInterval, tickActor, "Tick!")
  }

  override def onStop(app: Application) {
    Logger.info("Stopping quup Notifications Server...")

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