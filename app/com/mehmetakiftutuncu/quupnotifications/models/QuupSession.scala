package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.utilities.Conf
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
  * Created by akif on 12/07/16.
  */
case class QuupSession(registration: String, session: String) {
  def toCookie: String = s"${Conf.Login.sessionKey}=$session"

  def toJson: JsObject = Json.obj("registration" -> registration, "session" -> session)

  override def toString: String = toJson.toString()
}

object QuupSession {
  private val extractSessionFromSetCookieRegex: Regex = s"""^.*(${Conf.Login.sessionKey.replaceAll("""\.""", """\\.""")})\\s*?=\\s*?([0-9A-Z]+);.*$$""".r

  def getSessionFrom(wsResponse: WSResponse): Maybe[String] = {
    val maybeSetCookieHeader: Option[String] = wsResponse.header(HeaderNames.SET_COOKIE)

    if (maybeSetCookieHeader.isEmpty) {
      Maybe(Errors(CommonError.requestFailed.reason("""Response didn't contain "Set-Cookie" header!""").data(wsResponse.allHeaders.toString)))
    } else {
      val setCookieHeader: String = maybeSetCookieHeader.get
      val maybeSessionMatch: Option[Match] = extractSessionFromSetCookieRegex.findFirstMatchIn(setCookieHeader)

      if (maybeSessionMatch.isEmpty) {
        Maybe(Errors(CommonError.requestFailed.reason(""""Set-Cookie" header didn't contain session data!""").data(setCookieHeader)))
      } else {
        val sessionMatch: Match = maybeSessionMatch.get

        val session: String = sessionMatch.group(2)

        Maybe(session)
      }
    }
  }
}
