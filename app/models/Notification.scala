package models

import models.enum.NotificationTypes
import play.api.Logger
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

case class Notification(id: String,
                        notificationType: NotificationTypes,
                        from: QuupUser,
                        otherUsers: List[QuupUser],
                        when: Long,
                        isRead: Boolean) {
  def toJson: JsObject = Json.obj(
    "id"         -> id,
    "type"       -> notificationType.toString,
    "from"       -> from.toJson,
    "otherUsers" -> Json.toJson(otherUsers.map(_.toJson)),
    "isRead"     -> isRead
  )
}

object Notification {
  def fromJson(json: JsValue): Option[Notification] = {
    try {
      val id: String = (json \ "NotificationId").as[String]

      val notificationTypeString: String = (json \ "Type").as[String]
      val notificationTypeAsOpt: Option[NotificationTypes] = NotificationTypes.withName(notificationTypeString)

      val fromAsOpt: Option[QuupUser] = QuupUser.fromJson((json \ "ActionBy").as[JsArray].value.head)

      val otherUsersJsonList: List[JsValue] = (json \ "MoreActionBy").asOpt[JsArray].getOrElse(Json.arr()).value.toList
      val otherUsersListWithOps: List[Option[QuupUser]] = otherUsersJsonList.map(j => QuupUser.fromJson(j))
      val otherUsersList: List[QuupUser] = otherUsersListWithOps.collect {case u if u.isDefined => u.get}

      val when: Long = (json \ "UpdateStamp").as[String].toLong

      val isRead: Boolean = (json \ "State").as[Int] == 1

      if (notificationTypeAsOpt.isEmpty || fromAsOpt.isEmpty || otherUsersListWithOps.size != otherUsersList.size) {
        Logger.error(s"Failed to create notification from json $json")
        None
      } else {
        Option(Notification(id, notificationTypeAsOpt.get, fromAsOpt.get, otherUsersList, when, isRead))
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to create notification from json $json", t)
        None
    }
  }
}
