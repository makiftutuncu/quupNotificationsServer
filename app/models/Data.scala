package models

import play.api.libs.json.{JsValue, Json}

object Data {
  private var map: Map[String, QuupSession] = Map.empty[String, QuupSession]

  def add(registrationId: String, quupSession: QuupSession): Unit = {
    map += (registrationId -> quupSession)
  }

  def remove(registrationId: String): Unit = {
    map -= registrationId
  }

  def getSession(registrationId: String): Option[QuupSession] = map.get(registrationId)

  def contains(registrationId: String): Boolean = map.contains(registrationId)

  def foreach[T](f: (String, QuupSession) => T): Unit = {
    map.foreach(i => f(i._1, i._2))
  }

  def load(): Unit = {
    // TODO Load Json from a file

    map = Map.empty[String, QuupSession]
  }

  def save(): Unit = {
    val data: JsValue = Json.toJson(map.map(i => i._1 -> i._2.toJson))

    // TODO Save Json to a file
  }
}
