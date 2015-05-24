package models

import java.sql.Connection

import anorm.{Row, SimpleSql}
import play.api.Logger
import play.api.Play.current
import play.api.db.DB

object Data {
  def add(registrationId: String, quupSession: QuupSession): Boolean = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          val affectedRows: Int = insertSql(registrationId, quupSession).executeUpdate()

          val successful: Boolean = affectedRows == 1

          successful
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to add registration id $registrationId", t)
        false
    }
  }

  def remove(registrationId: String): Boolean = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          val affectedRows: Int = deleteSql(registrationId).executeUpdate()

          val successful: Boolean = affectedRows == 1

          successful
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to remove registration id $registrationId", t)
        false
    }
  }

  def getSession(registrationId: String): Option[QuupSession] = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          selectSessionSql(registrationId).executeQuery().apply().toList.headOption map {
            row: Row =>
              val tracking: String = row[String]("Data.trackingId")
              val session: String  = row[String]("Data.sessionId")

              QuupSession(tracking, session)
          }
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to get session for registration id $registrationId", t)
        None
    }
  }

  def contains(registrationId: String): Boolean = getSession(registrationId).isDefined

  def foreach[T](f: (String, QuupSession) => T): Unit = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          selectAllSessionsSql.executeQuery().apply().toList map {
            row: Row =>
              val registrationId: String = row[String]("Data.registrationId")
              val tracking: String       = row[String]("Data.trackingId")
              val session: String        = row[String]("Data.sessionId")

              val quupSession: QuupSession = QuupSession(tracking, session)

              (registrationId, quupSession)
          }
      } foreach {
        case (registrationId: String, quupSession: QuupSession) =>
          f(registrationId, quupSession)
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to perform foreach", t)
    }
  }

  def getLastId(registrationId: String, key: String): Option[String] = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          selectLastIdSql(registrationId, key).executeQuery().apply().toList.headOption flatMap {
            row: Row =>
              key match {
                case "notifications" => row[Option[String]]("lastNotificationId")
                case "mentions"      => row[Option[String]]("lastMentionId")
                case "messages"      => row[Option[String]]("lastMessageId")
              }
          }
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to get last $key id for registration id $registrationId", t)
        None
    }
  }

  def setLastId(registrationId: String, key: String, id: String): Boolean = {
    try {
      DB.withConnection {
        implicit connection: Connection =>
          val affectedRows: Int = updateLastIdSql(registrationId, key, id).executeUpdate()

          val successful: Boolean = affectedRows == 1

          successful
      }
    } catch {
      case t: Throwable =>
        Logger.error(s"Failed to set last $key id for registration id $registrationId", t)
        false
    }
  }

  private def insertSql(registrationId: String, quupSession: QuupSession): SimpleSql[Row] = {
    anorm.SQL(
      s"""
         |INSERT INTO Data(registrationId, trackingId, sessionId) VALUES({registrationId}, {trackingId}, {sessionId})
       """.stripMargin
    ).on("registrationId" -> registrationId, "trackingId" -> quupSession.tracking, "sessionId" -> quupSession.session)
  }

  private def deleteSql(registrationId: String): SimpleSql[Row] = {
    anorm.SQL(
      s"""
         |DELETE FROM Data WHERE registrationId = {registrationId}
       """.stripMargin
    ).on("registrationId" -> registrationId)
  }

  private def selectSessionSql(registrationId: String): SimpleSql[Row] = {
    anorm.SQL(
      s"""
         |SELECT trackingId, sessionId
         |FROM Data
         |WHERE registrationId = {registrationId}
       """.stripMargin
    ).on("registrationId" -> registrationId)
  }

  private def selectAllSessionsSql: SimpleSql[Row] = {
    anorm.SQL(
      s"""
         |SELECT registrationId, trackingId, sessionId
         |FROM Data
       """.stripMargin
    )
  }

  private def selectLastIdSql(registrationId: String, key: String): SimpleSql[Row] = {
    key match {
      case "notifications" =>
        anorm.SQL(
          s"""
             |SELECT lastNotificationId
             |FROM Data
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId)

      case "mentions" =>
        anorm.SQL(
          s"""
             |SELECT lastMentionId
             |FROM Data
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId)

      case "messages" =>
        anorm.SQL(
          s"""
             |SELECT lastMessageId
             |FROM Data
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId)
    }
  }

  private def updateLastIdSql(registrationId: String, key: String, id: String): SimpleSql[Row] = {
    key match {
      case "notifications" =>
        anorm.SQL(
          s"""
             |UPDATE Data
             |SET lastNotificationId = {id}
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId, "id" -> id)

      case "mentions" =>
        anorm.SQL(
          s"""
             |UPDATE Data
             |SET lastMentionId = {id}
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId, "id" -> id)

      case "messages" =>
        anorm.SQL(
          s"""
             |UPDATE Data
             |SET lastMessageId = {id}
             |WHERE registrationId = {registrationId}
       """.stripMargin
        ).on("registrationId" -> registrationId, "id" -> id)
    }
  }
}
