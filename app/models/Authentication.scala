package models

import controllers.Application._
import controllers.QuupRequest
import play.api.Logger
import play.api.libs.ws.WSResponse
import utilities.Conf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Authentication {
  def getTrackingCookie: Future[Option[String]] = {
    Request.url(Conf.Quup.quupHome).get() map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != OK) {
          Logger.error(s"Failed to get tracking cookie, quup returned invalid status $status")
          None
        } else {
          val setCookieHeader: String = wsResponse.header("Set-Cookie").getOrElse("")
          val trackingCookie: String  = Conf.trackingCookieRegex.findFirstMatchIn(setCookieHeader).map(m => m.group(1)).getOrElse("")

          if (trackingCookie.isEmpty) {
            Logger.error(s"Failed to get tracking cookie, tracking cookie is not found in cookies $setCookieHeader")
            None
          } else {
            Option(trackingCookie)
          }
        }
    } recover {
      case t: Throwable =>
        Logger.error(s"Failed to get tracking cookie!", t)
        None
    }
  }

  def getSessionCookie(trackingCookie: String, username: String, password: String): Future[Option[String]] = {
    Request.url(Conf.Quup.quupLogin)
      .withHeaders(
        "Cookie"          -> s"${Conf.trackingCookie}=$trackingCookie;",
        "Host"            -> "quup.com",
        "Connection"      -> "keep-alive",
        "Cache-Control"   -> "max-age=0",
        "Accept"          -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Origin"          -> "https://quup.com",
        "User-Agent"      -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36",
        "DNT"             -> "1",
        "Referer"         -> "https://quup.com/welcome",
        "Accept-Encoding" -> "gzip, deflate",
        "Accept-Language" -> "en-US,en;q=0.8,tr;q=0.6"
      )
      .withFollowRedirects(follow = false)
      .post(
        Map(
          "UserName"   -> Seq(username),
          "Password"   -> Seq(password),
          "RememberMe" -> Seq("true")
        )
      ) map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != FOUND) {
          Logger.error(s"Failed to get session cookie, quup returned invalid status $status")
          None
        } else {
          val setCookiesHeader: String = wsResponse.header("Set-Cookie").getOrElse("")
          val sessionCookie: String    = Conf.sessionCookieRegex.findFirstMatchIn(setCookiesHeader).map(m => m.group(1)).getOrElse("")

          if (sessionCookie.isEmpty) {
            Logger.error(s"Failed to get session cookie, session cookie is not found in cookies $setCookiesHeader")
            None
          } else {
            Option(sessionCookie)
          }
        }
    } recover {
      case t: Throwable =>
        Logger.error(s"Failed to get session cookie!", t)
        None
    }
  }

  def logout(implicit qr: QuupRequest): Future[Boolean] = {
    Request.quupUrl(qr.quupSession, Conf.Quup.quupLogout)
      .withFollowRedirects(follow = false)
      .get() map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != FOUND) {
          Logger.error(s"Failed to logout, quup returned invalid status $status")
          false
        } else {
          true
        }
    } recover {
      case t: Throwable =>
        Logger.error(s"Failed to logout!", t)
        false
    }
  }
}
