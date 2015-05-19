package models

import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSResponse, WS, WSRequestHolder}
import utilities.Conf

import scala.concurrent.Future

object Request {
  def url(url: String): WSRequestHolder = {
    WS.url(url)
      .withRequestTimeout(Conf.timeoutInMillis)
  }

  def quupUrl(quupSession: QuupSession, url: String): WSRequestHolder = {
    WS.url(url)
      .withRequestTimeout(Conf.timeoutInMillis)
      .withHeaders("Cookie" -> quupSession.getCookie)
  }

  def gcm(data: JsValue): Future[WSResponse] = {
    WS.url(Conf.GCM.url)
      .withRequestTimeout(Conf.timeoutInMillis)
      .withHeaders(Conf.GCM.authorizationHeader -> s"${Conf.GCM.authorizationKey}=${Conf.GCM.apiKey}")
      .post(data)
  }
}
