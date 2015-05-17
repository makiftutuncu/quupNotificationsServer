package models

import play.api.Play.current
import play.api.libs.ws.{WS, WSRequestHolder}
import utilities.Conf

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

  def gcm: WSRequestHolder = {
    WS.url(Conf.GCM.url)
      .withRequestTimeout(Conf.timeoutInMillis)
      .withHeaders(Conf.GCM.authorizationHeader -> Conf.GCM.apiKey)
  }
}
