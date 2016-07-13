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

case class QuupClient(private val wsClient: WSClient) extends Loggable {
  def login(username: String, password: String): Future[Maybe[String]] = {
    val body: Map[String, Seq[String]] = Map(
      Conf.Login.usernameKey -> Seq(username),
      Conf.Login.passwordKey -> Seq(password)
    )

    val request: WSRequest = wsClient.url(Conf.Url.login)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withBody(body)

    Log.debug(s"""Logging user "$username" in...""")

    request.post(body).map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != Status.FOUND) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("quup returned invalid status!").data(status.toString))
          Log.error("Login failed!", errors)

          Maybe(errors)
        } else {
          QuupSession.getSessionFrom(wsResponse)
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed)
        Log.error("Login Future failed!", errors, t)

        Maybe(errors)
    }
  }

  def getNotifications(quupSession: QuupSession): Future[Maybe[List[QuupNotification]]] = {
    val request: WSRequest = wsClient.url(Conf.Url.notifications())
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withHeaders(HeaderNames.COOKIE -> quupSession.toCookie, HeaderNames.ACCEPT -> ContentTypes.JSON)

    Log.debug(s"""Getting notifications for registration "${quupSession.registration}"...""")

    request.get().map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header(HeaderNames.CONTENT_TYPE).getOrElse("")

        if (status != Status.OK) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("quup returned invalid status!").data(status.toString))
          Log.error("Getting notifications failed!", errors)

          Maybe(errors)
        } else if (!contentType.contains(ContentTypes.JSON)) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("""quup returned invalid "Content-Type"!""").data(contentType))
          Log.error("Getting notifications failed!", errors)

          Maybe(errors)
        } else {
          val maybeNotificationsJson: Try[JsValue] = Try(wsResponse.json)

          if (maybeNotificationsJson.isFailure) {
            val errors: Errors = Errors(CommonError.requestFailed.reason("quup returned invalid body!").data(wsResponse.body))
            Log.error("Getting notifications failed!", errors)

            Maybe(errors)
          } else {
            QuupNotification.getQuupNotificationList(maybeNotificationsJson.get)
          }
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed)
        Log.error("Getting notifications Future failed!", errors, t)

        Maybe(errors)
    }
  }

  def logout(quupSession: QuupSession): Future[Errors] = {
    val request: WSRequest = wsClient.url(Conf.Url.logout)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withHeaders(HeaderNames.COOKIE -> quupSession.toCookie)

    Log.debug(s"""Logging registration "${quupSession.registration}" out...""")

    request.get().map {
      wsResponse: WSResponse =>
        val status: Int = wsResponse.status

        if (status != Status.FOUND) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("quup returned invalid status!").data(status.toString))
          Log.error("Logout failed!", errors)

          errors
        } else {
          Errors.empty
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed)
        Log.error("Logout Future failed!", errors, t)

        errors
    }
  }
}
