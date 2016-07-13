package com.mehmetakiftutuncu.quupnotifications.utilities

import java.sql.Connection

import anorm.{Row, SimpleSql}
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, QuupSession}
import play.api.db.DBApi

case class Database(private val db: DBApi) extends Loggable {
  def getQuupSession(registration: String): Maybe[QuupSession] = {
    try {
      Log.debug(s"""Getting QuupSession for registration "$registration"...""")

      withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""SELECT session FROM Users WHERE registration = {registration}""").on("registration" -> registration)

          val throwablesOrRows: Either[List[Throwable], List[Row]] = sql.executeQuery().fold(List.empty[Row])(_ :+ _)

          if (throwablesOrRows.isLeft) {
            val errors: Errors = Errors(CommonError.database.reason("Failed to collect rows!"))
            Log.error("Failed to get QuupSession!", errors)

            Maybe(errors)
          } else {
            val maybeRow: Option[Row] = throwablesOrRows.right.get.headOption

            if (maybeRow.isEmpty) {
              val errors: Errors = Errors(CommonError.database.reason("No rows were found!"))
              Log.error("Failed to get QuupSession!", errors)

              Maybe(errors)
            } else {
              val row: Row = maybeRow.get
              val session: String = row[String]("session")

              Maybe(QuupSession(registration, session))
            }
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)
        Log.error("Failed to get QuupSession with exception!", errors, t)

        Maybe(errors)
    }
  }

  def saveQuupSession(quupSession: QuupSession): Errors = {
    try {
      Log.debug(s"""Saving QuupSession for registration "${quupSession.registration}"...""")

      withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL(
            """
               |INSERT INTO Users (registration, session, lastNotification) VALUES ({registration}, {session}, 0)
               |ON DUPLICATE KEY
               |UPDATE session = {session}
            """.stripMargin).on(
            "registration" -> quupSession.registration,
            "session"      -> quupSession.session
          )

          val affectedRows: Int = sql.executeUpdate()

          if (affectedRows <= 0) {
            val errors: Errors = Errors(CommonError.database.reason("No rows were affected!"))
            Log.error("Failed to save QuupSession!", errors)

            errors
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)
        Log.error("Failed to save QuupSession with exception!", errors, t)

        errors
    }
  }

  def deleteQuupSession(quupSession: QuupSession): Errors = {
    try {
      Log.debug(s"""Deleting QuupSession for registration "${quupSession.registration}"...""")

      withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""DELETE FROM Users WHERE registration = {registration}""").on("registration" -> quupSession.registration)

          val affectedRows: Int = sql.executeUpdate()

          if (affectedRows <= 0) {
            val errors: Errors = Errors(CommonError.database.reason("No rows were affected!"))
            Log.error("Failed to delete QuupSession!", errors)

            errors
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database)
        Log.error("Failed to delete QuupSession with exception!", errors, t)

        errors
    }
  }

  private def withConnection[R](action: Connection => R): R = {
    db.database("default").withConnection(action)
  }
}
