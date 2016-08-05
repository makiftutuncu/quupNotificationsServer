package com.mehmetakiftutuncu.quupnotifications.utilities

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Results.EmptyContent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

@Singleton
case class QuupClient @Inject()(Conf: ConfBase,
                                WSClient: WSClient) extends QuupClientBase

@ImplementedBy(classOf[QuupClient])
trait QuupClientBase extends Loggable {
  protected val Conf: ConfBase
  protected val WSClient: WSClient

  def login(username: String, password: String): Future[Maybe[String]] = {
    val body: Map[String, Seq[String]] = Map(
      Conf.Login.usernameKey -> Seq(username),
      Conf.Login.passwordKey -> Seq(password)
    )

    val request: WSRequest = WSClient.url(Conf.Url.login)
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
          Registration.getSessionIdFrom(Conf, wsResponse)
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed.reason(t.getMessage))

        Log.error("Login Future failed!", errors, t)

        Maybe(errors)
    }
  }

  def getNotifications(registration: Registration): Future[Maybe[List[Notification]]] = {
    val request: WSRequest = WSClient.url(Conf.Url.notifications)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withQueryString(Conf.Url.leaveAsUnreadFlagName -> "true")
                                     .withHeaders(HeaderNames.COOKIE -> registration.toCookie(Conf), HeaderNames.ACCEPT -> ContentTypes.JSON)

    Log.debug(s"""Getting notifications for registration id "${registration.registrationId}"...""")

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
            Notification.getNotificationList(maybeNotificationsJson.get)
          }
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed.reason(t.getMessage))

        Log.error("Getting notifications Future failed!", errors, t)

        Maybe(errors)
    }
  }

  def logout(registration: Registration): Future[Errors] = {
    val request: WSRequest = WSClient.url(Conf.Url.logout)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withFollowRedirects(follow = false)
                                     .withHeaders(HeaderNames.COOKIE -> registration.toCookie(Conf))

    Log.debug(s"""Logging registration id "${registration.registrationId}" out...""")

    request.post(EmptyContent()).map {
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
        val errors: Errors = Errors(CommonError.requestFailed.reason(t.getMessage))

        Log.error("Logout Future failed!", errors, t)

        errors
    }
  }
}
