package models

import controllers.QuupRequest
import models.enum.NotificationTypes
import play.api.Logger
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import utilities.Conf

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Notifications {
  def get(notificationType: NotificationTypes, markAsRead: Boolean = true)(implicit qr: QuupRequest): Future[List[Notification]] = {
    val notificationTypeValue: String = notificationType match {
      case NotificationTypes.Mention       => "mention"
      case NotificationTypes.DirectMessage => "directMessage"
      case _                               => "followComment"
    }

    Request.quupUrl(Conf.Notifications.url).post(
      Map(
        Conf.Notifications.notificationTypeKey -> Seq(notificationTypeValue),
        Conf.Notifications.markAsReadKey       -> Seq(markAsRead.toString)
      )
    ) map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header("Content-Type").getOrElse("")

        if (status != Status.OK) {
          Logger.error(s"Failed to get notifications with type $notificationType and markAsRead $markAsRead, quup returned invalid status $status")
          List.empty[Notification]
        } else if (!contentType.contains(MimeTypes.JSON)) {
          Logger.error(s"Failed to get notifications with type $notificationType and markAsRead $markAsRead, quup returned invalid content type $contentType")
          List.empty[Notification]
        } else {
          val json: JsValue = wsResponse.json

          val notificationsListWithOpt: List[Option[Notification]] = (json \ "Notifications").asOpt[JsArray].getOrElse(Json.arr()).value.toList.map(Notification.fromJson)
          val notificationsList: List[Notification] = notificationsListWithOpt.collect {case n if n.isDefined => n.get}

          if (notificationsListWithOpt.size != notificationsList.size) {
            Logger.error(s"Failed to get notifications with type $notificationType and markAsRead $markAsRead, could not parse some of the notifications from json $json")
            List.empty[Notification]
          } else {
            notificationsList
          }
        }
    } recover {
      case t: Throwable =>
        Logger.error(s"Failed to get notifications with type $notificationType and markAsRead $markAsRead", t)
        List.empty[Notification]
    }
  }
}