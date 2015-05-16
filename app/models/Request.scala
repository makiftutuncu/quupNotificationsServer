package models

import controllers.QuupRequest
import play.api.Play.current
import play.api.libs.ws.{WS, WSRequestHolder}
import utilities.Conf

object Request {
  def url(url: String): WSRequestHolder = {
    WS.url(url).withRequestTimeout(Conf.timeoutInMillis)
  }

  def quupUrl(url: String)(implicit qr: QuupRequest): WSRequestHolder = {
    WS.url(url).withRequestTimeout(Conf.timeoutInMillis).withHeaders("Cookie" -> qr.getCookie)
  }
}
