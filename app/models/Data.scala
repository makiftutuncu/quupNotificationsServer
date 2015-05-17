package models

import java.io.{FileWriter, BufferedWriter}

import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utilities.Conf

import scala.io.{BufferedSource, Source}

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
    map = try {
      val source: BufferedSource = Source.fromFile(Conf.dataPath, "UTF-8")
      val dataAsString: String = source.mkString

      source.close()

      val dataAsJson: JsObject = Json.parse(dataAsString).as[JsObject]

      val registrationIdToQuupSessionAsOptSeq: Seq[(String, Option[QuupSession])] = dataAsJson.fields.map {
        case (registrationId: String, quupSessionAsJson: JsValue) =>
          registrationId -> QuupSession.fromJson(quupSessionAsJson)
      }

      val registrationIdToQuupSessionSeq: Seq[(String, QuupSession)] = registrationIdToQuupSessionAsOptSeq collect {
        case (registrationId: String, quupSessionAsOpt: Option[QuupSession]) =>
          registrationId -> quupSessionAsOpt.get
      }

      registrationIdToQuupSessionSeq.toMap
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to load data!", t)
        Map.empty[String, QuupSession]
    }
  }

  def save(): Unit = {
    try {
      val data: String = Json.toJson(map.map(i => i._1 -> i._2.toJson)).toString()

      val fileWriter: FileWriter         = new FileWriter(Conf.dataPath)
      val bufferedWriter: BufferedWriter = new BufferedWriter(fileWriter)

      bufferedWriter.write(data)
      bufferedWriter.flush()

      bufferedWriter.close()
      fileWriter.close()
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to save data!", t)
    }
  }
}
