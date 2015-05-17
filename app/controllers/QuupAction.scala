package controllers

import models.{Data, QuupSession}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

case class QuupRequest(request: Request[JsValue], registrationId: String) {
  def quupSession: QuupSession = Data.getSession(registrationId).getOrElse(QuupSession("", ""))
}

object QuupAction extends Controller {
  def apply(qr: QuupRequest => Future[Result]): Action[JsValue] = Action.async(parse.json) {
    request: Request[JsValue] =>
      val registrationIdAsOpt: Option[String] = (request.body \ "registrationId").asOpt[String]

      if (registrationIdAsOpt.getOrElse("").isEmpty) {
        Logger.error("Received a quup request without registration id!")
        Future.successful(BadRequest)
      } else if (!Data.contains(registrationIdAsOpt.get)) {
        Logger.error("Received a quup request with an unauthorized registration id!")
        Future.successful(Unauthorized)
      } else {
        val quupRequest: QuupRequest = QuupRequest(request, registrationIdAsOpt.get)

        qr(quupRequest)
      }
  }
}