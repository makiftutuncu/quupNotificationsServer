package controllers

import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.Future

case class QuupRequest(request: Request[JsValue], token: String)

object QuupAction extends Controller {
  def apply(qr: QuupRequest => Future[Result]): Action[JsValue] = Action.async(parse.json) {
    request: Request[JsValue] =>
      val token       = (request.body \ "token").as[String]
      val quupRequest = QuupRequest(request, token)

      qr(quupRequest)
  }
}