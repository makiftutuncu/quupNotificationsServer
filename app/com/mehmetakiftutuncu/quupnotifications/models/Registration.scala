package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class Registration(registrationId: String, sessionId: String, lastNotification: Long) {
  def toCookie(Conf: ConfBase): String = s"${Conf.Login.sessionKey}=$sessionId"

  def toJson: JsObject = Json.obj("registrationId" -> registrationId, "sessionId" -> sessionId, "lastNotification" -> lastNotification)

  override def toString: String = toJson.toString()
}

object Registration extends Loggable {
  private def extractSessionIdFromSetCookieRegex(Conf: ConfBase): Regex = s"""^.*(${Conf.Login.sessionKey.replaceAll("""\.""", """\\.""")})\\s*?=\\s*?([0-9A-Z]+);.*$$""".r

  def getSessionIdFrom(Conf: ConfBase, wsResponse: WSResponse): Maybe[String] = {
    val maybeSetCookieHeader: Option[String] = wsResponse.header(HeaderNames.SET_COOKIE)

    if (maybeSetCookieHeader.isEmpty) {
      val errors: Errors = Errors(CommonError.requestFailed.reason("""Response didn't contain "Set-Cookie" header!""").data(wsResponse.allHeaders.toString))
      Log.error("Failed to get session id!", errors)

      Maybe(errors)
    } else {
      val setCookieHeader: String = maybeSetCookieHeader.get
      val maybeSessionIdMatch: Option[Match] = extractSessionIdFromSetCookieRegex(Conf).findFirstMatchIn(setCookieHeader)

      if (maybeSessionIdMatch.isEmpty) {
        val errors: Errors = Errors(CommonError.requestFailed.reason(""""Set-Cookie" header didn't contain session data!""").data(setCookieHeader))
        Log.error("Failed to get session id!", errors)

        Maybe(errors)
      } else {
        val sessionIdMatch: Match = maybeSessionIdMatch.get

        val sessionId: String = sessionIdMatch.group(2)

        Maybe(sessionId)
      }
    }
  }
}
