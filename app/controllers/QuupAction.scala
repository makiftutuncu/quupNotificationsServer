package controllers

import models.Data
import play.api.libs.json.JsValue
import play.api.mvc._
import utilities.Conf

import scala.concurrent.Future

case class QuupRequest(request: Request[JsValue], registrationId: String) {
  def getCookie: String = Data.getSession(registrationId) map {
    qs =>
      s"${Conf.trackingCookie}=${qs.tracking}; ${Conf.sessionCookie}=${qs.session};"
  } getOrElse ""
}

object QuupAction extends Controller {
  def apply(qr: QuupRequest => Future[Result]): Action[JsValue] = Action.async(parse.json) {
    request: Request[JsValue] =>
      val registrationIdAsOpt: Option[String] = (request.body \ "registrationId").asOpt[String]

      if (registrationIdAsOpt.getOrElse("").isEmpty) {
        Future.successful(BadRequest)
      } else if (!Data.contains(registrationIdAsOpt.get)) {
        Future.successful(Unauthorized)
      } else {
        val quupRequest: QuupRequest = QuupRequest(request, registrationIdAsOpt.get)

        qr(quupRequest)
      }
  }
}