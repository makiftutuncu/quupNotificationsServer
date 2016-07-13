package com.mehmetakiftutuncu.quupnotifications.utilities

import java.sql.Connection

import anorm.{Row, SimpleSql}
import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, QuupSession}
import play.api.db.DBApi

/**
  * Created by akif on 12/07/16.
  */
case class Database(private val db: DBApi) {
  def getQuupSession(registration: String): Maybe[QuupSession] = {
    try {
      withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""SELECT session FROM Users WHERE registration = {registration}""").on("registration" -> registration)

          val throwablesOrRows: Either[List[Throwable], List[Row]] = sql.executeQuery().fold(List.empty[Row])(_ :+ _)

          if (throwablesOrRows.isLeft) {
            Maybe(Errors(CommonError.database.reason("Failed to collect rows!")))
          } else {
            val maybeRow: Option[Row] = throwablesOrRows.right.get.headOption

            if (maybeRow.isEmpty) {
              Maybe(Errors(CommonError.database.reason("No rows were found!")))
            } else {
              val row: Row = maybeRow.get
              val session: String = row[String]("session")

              Maybe(QuupSession(registration, session))
            }
          }
      }
    } catch {
      case t: Throwable =>
        Maybe(Errors(CommonError.database))
    }
  }

  def saveQuupSession(quupSession: QuupSession): Errors = {
    try {
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
            Errors(CommonError.database.reason("No rows were affected!"))
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        Errors(CommonError.database)
    }
  }

  def deleteQuupSession(quupSession: QuupSession): Errors = {
    try {
      withConnection {
        implicit connection: Connection =>
          val sql: SimpleSql[Row] = anorm.SQL("""DELETE FROM Users WHERE registration = {registration}""").on("registration" -> quupSession.registration)

          val affectedRows: Int = sql.executeUpdate()

          if (affectedRows <= 0) {
            Errors(CommonError.database.reason("No rows were affected!"))
          } else {
            Errors.empty
          }
      }
    } catch {
      case t: Throwable =>
        Errors(CommonError.database)
    }
  }

  private def withConnection[R](action: Connection => R): R = {
    db.database("default").withConnection(action)
  }
}
