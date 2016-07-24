package com.mehmetakiftutuncu.quupnotifications.models

import java.sql.Connection

import anorm.{Row, SimpleSql}
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.http.HeaderNames
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

case class Registration(registrationId: String, sessionId: String, lastNotification: Long) {
  def toCookie(conf: ConfBase): String = s"${conf.Login.sessionKey}=$sessionId"

  def toJson: JsObject = Json.obj("registrationId" -> registrationId, "sessionId" -> sessionId, "lastNotification" -> lastNotification)

  override def toString: String = toJson.toString()
}

object Registration extends Loggable {
  private def extractSessionIdFromSetCookieRegex(conf: ConfBase): Regex = s"""^.*(${conf.Login.sessionKey.replaceAll("""\.""", """\\.""")})\\s*?=\\s*?([0-9A-Z]+);.*$$""".r

  def getRegistrations(database: DatabaseBase): Maybe[List[Registration]] = {
    try {
      Log.debug(s"""Getting Registrations...""")

      database.withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""SELECT * FROM Registration""")

          val throwablesOrRows: Either[List[Throwable], List[Row]] = sql.executeQuery().fold(List.empty[Row])(_ :+ _)

          if (throwablesOrRows.isLeft) {
            val errors: Errors = Errors(CommonError.database.reason("Failed to collect rows!"))

            Log.error("Failed to get Registrations!", errors)

            Maybe(errors)
          } else {
            val rows: List[Row] = throwablesOrRows.right.get

            val registrations: List[Registration] = rows.map {
              row: Row =>
                val registrationId: String = row[String]("registrationId")
                val sessionId: String      = row[String]("sessionId")
                val lastNotification: Long = row[Long]("lastNotification")

                Registration(registrationId, sessionId, lastNotification)
            }

            Maybe(registrations)
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)

        Log.error("Failed to get Registrations with exception!", errors, t)

        Maybe(errors)
    }
  }

  def getRegistration(database: DatabaseBase, registrationId: String): Maybe[Registration] = {
    try {
      Log.debug(s"""Getting Registration for registration id "$registrationId"...""")

      database.withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""SELECT sessionId, lastNotification FROM Registration WHERE registrationId = {registrationId}""").on("registrationId" -> registrationId)

          val throwablesOrRows: Either[List[Throwable], List[Row]] = sql.executeQuery().fold(List.empty[Row])(_ :+ _)

          if (throwablesOrRows.isLeft) {
            val errors: Errors = Errors(CommonError.database.reason("Failed to collect rows!"))

            Log.error("Failed to get Registration!", errors)

            Maybe(errors)
          } else {
            val maybeRow: Option[Row] = throwablesOrRows.right.get.headOption

            if (maybeRow.isEmpty) {
              val errors: Errors = Errors(CommonError("notFound").reason("No rows were found!").data(registrationId))
              Log.error("Failed to get Registration!", errors)

              Maybe(errors)
            } else {
              val row: Row = maybeRow.get

              val sessionId: String      = row[String]("sessionId")
              val lastNotification: Long = row[Long]("lastNotification")

              Maybe(Registration(registrationId, sessionId, lastNotification))
            }
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)

        Log.error("Failed to get Registration with exception!", errors, t)

        Maybe(errors)
    }
  }

  def saveRegistration(database: DatabaseBase, registration: Registration): Errors = {
    try {
      Log.debug(s"""Saving Registration for registration id "${registration.registrationId}"...""")

      database.withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL(
            """
              |INSERT INTO Registration (registrationId, sessionId, lastNotification) VALUES ({registrationId}, {sessionId}, {lastNotification})
              |ON DUPLICATE KEY
              |UPDATE sessionId = {sessionId}, lastNotification = {lastNotification}
            """.stripMargin).on(
            "registrationId"   -> registration.registrationId,
            "sessionId"        -> registration.sessionId,
            "lastNotification" -> registration.lastNotification
          )

          val affectedRows: Int = sql.executeUpdate()

          if (affectedRows <= 0) {
            val errors: Errors = Errors(CommonError.database.reason("No rows were affected!"))

            Log.error("Failed to save Registration!", errors)

            errors
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)

        Log.error("Failed to save Registration with exception!", errors, t)

        errors
    }
  }

  def deleteRegistration(database: DatabaseBase, registration: Registration): Errors = {
    try {
      Log.debug(s"""Deleting Registration for registration id "${registration.registrationId}"...""")

      database.withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""DELETE FROM Registration WHERE registrationId = {registrationId}""").on("registrationId" -> registration.registrationId)

          val affectedRows: Int = sql.executeUpdate()

          if (affectedRows <= 0) {
            val errors: Errors = Errors(CommonError.database.reason("No rows were affected!"))

            Log.error("Failed to delete Registration!", errors)

            errors
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)

        Log.error("Failed to delete Registration with exception!", errors, t)

        errors
    }
  }

  def getSessionIdFrom(conf: ConfBase, wsResponse: WSResponse): Maybe[String] = {
    val maybeSetCookieHeader: Option[String] = wsResponse.header(HeaderNames.SET_COOKIE)

    if (maybeSetCookieHeader.isEmpty) {
      val errors: Errors = Errors(CommonError.requestFailed.reason("""Response didn't contain "Set-Cookie" header!""").data(wsResponse.allHeaders.toString))
      Log.error("Failed to get session id!", errors)

      Maybe(errors)
    } else {
      val setCookieHeader: String = maybeSetCookieHeader.get
      val maybeSessionIdMatch: Option[Match] = extractSessionIdFromSetCookieRegex(conf).findFirstMatchIn(setCookieHeader)

      if (maybeSessionIdMatch.isEmpty) {
        val errors: Errors = Errors(CommonError.requestFailed.reason(""""Set-Cookie" header didn't contain session data!""").data(setCookieHeader))
        Log.error("Failed to get session id!", errors)

        Maybe(errors)
      } else {
        val sessionIdMatch: Match = maybeSessionIdMatch.get

        val sessionId: String = sessionIdMatch.group(2)

        Maybe(sessionId)
      }
    }
  }
}
