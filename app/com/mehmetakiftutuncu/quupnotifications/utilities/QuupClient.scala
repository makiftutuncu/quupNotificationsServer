package com.mehmetakiftutuncu.quupnotifications.utilities

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, QuupNotification, QuupSession}
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * Created by akif on 12/07/16.
  */
case class QuupClient(private val wsClient: WSClient) {
  def login(username: String, password: String): Future[Maybe[String]] = {
    val body: Map[String, Seq[String]] = Map(
      Conf.Login.usernameKey -> Seq(username),
      Conf.Login.passwordKey -> Seq(password)
    )

    val request: WSRequest = wsClient.url(Conf.Url.login)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withBody(body)

    request.post(body).map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != Status.FOUND) {
          Maybe(Errors(CommonError.requestFailed.reason("""quup returned invalid status!""").data(status.toString)))
        } else {
          QuupSession.getSessionFrom(wsResponse)
        }
    }.recover {
      case t: Throwable =>
        Maybe(Errors(CommonError.requestFailed))
    }
  }

  def getNotifications(session: QuupSession): Future[Maybe[List[QuupNotification]]] = {
    val request: WSRequest = wsClient.url(Conf.Url.notifications())
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withHeaders(HeaderNames.COOKIE -> session.toCookie, HeaderNames.ACCEPT -> ContentTypes.JSON)

    request.get().map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header(HeaderNames.CONTENT_TYPE).getOrElse("")

        if (status != Status.OK) {
          Maybe(Errors(CommonError.requestFailed.reason("""quup returned invalid status!""").data(status.toString)))
        } else if (!contentType.contains(ContentTypes.JSON)) {
          Maybe(Errors(CommonError.requestFailed.reason("""quup returned invalid "Content-Type"!""").data(contentType)))
        } else {
          val maybeNotificationsJson: Try[JsValue] = Try(wsResponse.json)

          if (maybeNotificationsJson.isFailure) {
            Maybe(Errors(CommonError.requestFailed.reason("""quup returned invalid body!""").data(wsResponse.body)))
          } else {
            QuupNotification.getQuupNotifications(maybeNotificationsJson.get)
          }
        }
    }.recover {
      case t: Throwable =>
        Maybe(Errors(CommonError.requestFailed))
    }
  }

  def logout(session: QuupSession): Future[Errors] = {
    val request: WSRequest = wsClient.url(Conf.Url.logout)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withHeaders(HeaderNames.COOKIE -> session.toCookie)

    request.get().map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != Status.FOUND) {
          Errors(CommonError.requestFailed.reason("""quup returned invalid status!""").data(status.toString))
        } else {
          Errors.empty
        }
    }.recover {
      case t: Throwable =>
        Errors(CommonError.requestFailed)
    }
  }
}
