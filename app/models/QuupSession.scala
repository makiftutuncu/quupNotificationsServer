package models

import play.api.Logger
import play.api.libs.json.{JsValue, JsObject, Json}

case class QuupSession(tracking: String, session: String) {
  def toJson: JsObject = Json.obj(
    "tracking" -> tracking,
    "session"  -> session
  )
}

object QuupSession {
  def fromJson(json: JsValue): Option[QuupSession] = {
    try {
      val tracking = (json \ "tracking").as[String]
      val session  = (json \ "session").as[String]

      Option(QuupSession(tracking, session))
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to create quup session from json $json", t)
        None
    }
  }
}
