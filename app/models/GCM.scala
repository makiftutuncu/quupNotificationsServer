package models

import models.enum.NotificationTypes
import play.api.Logger
import play.api.http.{MimeTypes, Status}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import utilities.Conf

import scala.concurrent.ExecutionContext.Implicits.global

object GCM {
  def pushAll(): Unit = Data.foreach(prepareAndPush)

  def prepareAndPush(registrationId: String, quupSession: QuupSession): Unit = {
    Notifications.get(quupSession, NotificationTypes.Comment) map {
      notifications: List[Notification] =>
        push(registrationId, "notifications", notifications)
    }

    Notifications.get(quupSession, NotificationTypes.Mention) map {
      mentions: List[Notification] =>
        push(registrationId, "mentions", mentions)
    }

    Notifications.get(quupSession, NotificationTypes.DirectMessage) map {
      messages: List[Notification] =>
        push(registrationId, "messages", messages)
    }
  }

  def push(registrationId: String, key: String, notifications: List[Notification]) = {
    val data: JsValue = Json.obj(
      key -> Json.toJson(notifications.map(_.toJson))
    )

    val json: JsObject = Json.obj(
      Conf.GCM.registrationIdsKey -> Json.arr(registrationId),
      Conf.GCM.collapseKey        -> key,
      Conf.GCM.dataKey            -> data
    )

    Request.gcm(json) map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header("Content-Type").getOrElse("")

        if (status != Status.OK) {
          Logger.info(s"Failed to push, GCM returned with status $status and body ${wsResponse.body} for registration id $registrationId")
        } else if (!contentType.contains(MimeTypes.JSON)) {
          Logger.info(s"Failed to push, GCM returned with content type $contentType and body ${wsResponse.body} for registration id $registrationId")
        } else {
          val json: JsValue = wsResponse.json

          val success: Int = (json \ "success").asOpt[Int].getOrElse(-1)

          if (success != 1) {
            Logger.info(s"Failed to push, GCM returned with json $json for registration id $registrationId")

            (json \ "results").asOpt[JsArray].getOrElse(Json.arr()).value.headOption.foreach {
              result: JsValue =>
                (result \ "error").asOpt[String] match {
                  case Some(reason) =>
                    if (reason == "NotRegistered" || reason == "InvalidRegistration") {
                      Data.remove(registrationId)
                    }

                  case _ =>
                }
            }
          }
        }
    } recover {
      case t: Throwable =>
        Logger.error(s"Failed to push for registration id $registrationId and key $key", t)
    }
  }
}
