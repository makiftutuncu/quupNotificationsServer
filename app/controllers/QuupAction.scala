package controllers

import play.api.libs.json.JsValue
import play.api.mvc._
import utilities.Conf

import scala.concurrent.Future

case class QuupRequest(request: Request[JsValue], tracking: String, session: String) {
  def getCookie: String = s"${Conf.trackingCookie}=$tracking; ${Conf.sessionCookie}=$session;"
}

object QuupAction extends Controller {
  def apply(qr: QuupRequest => Future[Result]): Action[JsValue] = Action.async(parse.json) {
    request: Request[JsValue] =>
      val trackingAsOpt = (request.body \ "tracking").asOpt[String]
      val sessionAsOpt  = (request.body \ "session").asOpt[String]

      if (trackingAsOpt.getOrElse("").isEmpty || sessionAsOpt.getOrElse("").isEmpty) {
        Future.successful(BadRequest("""Your request must contain "tracking" and "session" values as JSON!"""))
      } else {
        val quupRequest: QuupRequest = QuupRequest(request, trackingAsOpt.get, sessionAsOpt.get)

        qr(quupRequest)
      }
  }
}