package controllers

import models.enum.NotificationTypes
import models.Notifications
import play.api.Logger
import play.api.Play.current
import play.api.http.MimeTypes
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WS
import play.api.mvc.{Action, Controller}
import utilities.Conf

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {
  def index = Action {
    Ok("Welcome to quup Notifications!")
  }

  def getNotifications = QuupAction {
    implicit qr =>
      Notifications.get(NotificationTypes.Comment) map {
        notificationList =>
          val json: JsObject = Json.obj(
            "notifications" -> Json.toJson(notificationList.map(_.toJson))
          )

          Ok(json).as(MimeTypes.JSON)
      }
  }

  def getMentions = QuupAction {
    implicit qr =>
      Notifications.get(NotificationTypes.Mention) map {
        notificationList =>
          val json: JsObject = Json.obj(
            "notifications" -> Json.toJson(notificationList.map(_.toJson))
          )

          Ok(json).as(MimeTypes.JSON)
      }
  }

  def getMessages = QuupAction {
    implicit qr =>
      Notifications.get(NotificationTypes.DirectMessage) map {
        notificationList =>
          val json: JsObject = Json.obj(
            "notifications" -> Json.toJson(notificationList.map(_.toJson))
          )

          Ok(json).as(MimeTypes.JSON)
      }
  }

  def logout = QuupAction {
    implicit qr =>
      WS.url(Conf.Auth.logoutUrl)
        .withHeaders("Cookie" -> s"${Conf.cookie}=${qr.token};")
        .withRequestTimeout(Conf.timeoutInMillis)
        .withFollowRedirects(follow = false)
        .get() map {
        wsResponse =>
          val status = wsResponse.status

          if (status != play.api.http.Status.FOUND) {
            Logger.error(s"Failed to logout, quup returned invalid status $status")
            InternalServerError
          } else {
            Ok
          }
      } recover {
        case t: Throwable =>
          Logger.error(s"Failed to logout", t)
          InternalServerError
      }
  }
}