package models

import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

case class QuupUser(id: Long, username: String, name: String, avatar: String) {
  def toJson: JsObject = Json.obj(
    "id"       -> id,
    "username" -> username,
    "name"     -> name,
    "avatar"   -> avatar
  )
}

object QuupUser {
  def fromJson(json: JsValue): Option[QuupUser] = {
    try {
      val id       = (json \ "MemberId").as[String].toLong
      val username = (json \ "UserName").as[String]
      val name     = (json \ "DisplayName").as[String]
      val avatar   = (json \ "AvatarUrl").as[String]

      val finalAvatar = if (avatar.endsWith("/s")) avatar.dropRight(2) else avatar

      Option(QuupUser(id, username, name, finalAvatar))
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to create quup user from json $json", t)
        None
    }
  }
}
