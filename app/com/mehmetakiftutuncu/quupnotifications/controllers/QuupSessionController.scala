package com.mehmetakiftutuncu.quupnotifications.controllers

import javax.inject.Inject

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe._
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, QuupNotification, QuupSession}
import com.mehmetakiftutuncu.quupnotifications.utilities.{ControllerBase, Database, QuupClient}
import play.api.db.DBApi
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class QuupSessionController @Inject() (dBApi: DBApi, wsClient: WSClient) extends ControllerBase {
  lazy val database: Database     = Database(dBApi)
  lazy val quupClient: QuupClient = QuupClient(wsClient)

  def login = {
    Action.async(parse.json) {
      implicit request: Request[JsValue] =>
        val maybeRegistration: Maybe[String] = extract[String]("registration")
        val maybeUsername: Maybe[String]     = extract[String]("username")
        val maybePassword: Maybe[String]     = extract[String]("password")

        val maybeRegistrationIdUsernameAndPassword: Maybe[(String, String, String)] = {
          if (maybeRegistration.hasErrors || maybeUsername.hasErrors || maybePassword.hasErrors) {
            Maybe(maybeRegistration.maybeErrors.getOrElse(Errors.empty) ++ maybeUsername.maybeErrors.getOrElse(Errors.empty) ++ maybePassword.maybeErrors.getOrElse(Errors.empty))
          } else {
            Maybe((maybeRegistration.value, maybeUsername.value, maybePassword.value))
          }
        }

        if (maybeRegistrationIdUsernameAndPassword.hasErrors) {
          futureFailWithErrors("Failed to login!", maybeRegistrationIdUsernameAndPassword.errors)
        } else {
          val (registration: String, username: String, password: String) = maybeRegistrationIdUsernameAndPassword.value

          quupClient.login(username, password).map {
            maybeSession: Maybe[String] =>
              if (maybeSession.hasErrors) {
                failWithErrors("Failed to login!", maybeSession.errors)
              } else {
                val quupSession: QuupSession = QuupSession(registration, maybeSession.value)

                val saveErrors: Errors = database.saveQuupSession(quupSession)

                if (saveErrors.hasErrors) {
                  failWithErrors("Failed to login!", saveErrors)
                } else {
                  success(quupSession.toJson)
                }
              }
          }
        }
    }
  }

  def logout(registration: String) = {
    Action.async {
      implicit request: Request[AnyContent] =>
        if (!Option(registration).getOrElse("").matches("[a-zA-Z0-9]+")) {
          futureFailWithErrors("Failed to logout!", Errors(CommonError.invalidRequest.reason("Registration was invalid!").data(registration)))
        } else {
          val maybeQuupSession: Maybe[QuupSession] = database.getQuupSession(registration)

          if (maybeQuupSession.hasErrors) {
            futureFailWithErrors("Failed to logout!", maybeQuupSession.errors)
          } else {
            val quupSession: QuupSession = maybeQuupSession.value

            /* Ignore the result of logout request to quup because
             * deleting QuupSession below is gonna make quup Notifications Server forget about the user anyway.
             * Calling this is just to let quup know so they may choose to delete user's session. */
            quupClient.logout(quupSession)

            val deleteErrors: Errors = database.deleteQuupSession(quupSession)

            if (deleteErrors.hasErrors) {
              futureFailWithErrors("Failed to logout!", deleteErrors)
            } else {
              Future.successful(Ok)
            }
          }
        }
    }
  }

  def test(session: String) = {
    Action.async {
      implicit request: Request[AnyContent] =>
        val quupSession: QuupSession = QuupSession("foo", session)

        quupClient.getNotifications(quupSession).map {
          maybeNotifications: Maybe[List[QuupNotification]] =>
            if (maybeNotifications.hasErrors) {
              failWithErrors("Failed to test!", maybeNotifications.errors)
            } else {
              success(Json.toJson(maybeNotifications.value.map(_.toJson)))
            }
        }
    }
  }
}
