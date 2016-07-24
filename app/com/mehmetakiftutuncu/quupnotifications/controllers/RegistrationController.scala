package com.mehmetakiftutuncu.quupnotifications.controllers

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.google.inject.Inject
import com.mehmetakiftutuncu.quupnotifications.models.Maybe._
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, Registration}
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.Environment
import play.api.db.DBApi
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationController @Inject() (dbApi: DBApi,
                                        environment: Environment,
                                        wsClient: WSClient) extends ControllerBase {
  private lazy val conf: ConfBase         = Conf(environment)
  private lazy val database: Database     = Database(dbApi)
  private lazy val quupClient: QuupClient = QuupClient(conf, wsClient)

  def login = {
    Action.async(parse.json) {
      implicit request: Request[JsValue] =>
        val maybeRegistrationId: Maybe[String] = extract[String]("registrationId")
        val maybeUsername: Maybe[String]       = extract[String]("username")
        val maybePassword: Maybe[String]       = extract[String]("password")

        val maybeRegistrationIdUsernameAndPassword: Maybe[(String, String, String)] = {
          if (maybeRegistrationId.hasErrors || maybeUsername.hasErrors || maybePassword.hasErrors) {
            Maybe(maybeRegistrationId.maybeErrors.getOrElse(Errors.empty) ++ maybeUsername.maybeErrors.getOrElse(Errors.empty) ++ maybePassword.maybeErrors.getOrElse(Errors.empty))
          } else {
            Maybe((maybeRegistrationId.value, maybeUsername.value, maybePassword.value))
          }
        }

        if (maybeRegistrationIdUsernameAndPassword.hasErrors) {
          futureFailWithErrors("Failed to login!", maybeRegistrationIdUsernameAndPassword.errors)
        } else {
          val (registrationId: String, username: String, password: String) = maybeRegistrationIdUsernameAndPassword.value

          quupClient.login(username, password).map {
            maybeSessionId: Maybe[String] =>
              if (maybeSessionId.hasErrors) {
                failWithErrors("Failed to login!", maybeSessionId.errors)
              } else {
                val registration: Registration = Registration(registrationId, maybeSessionId.value, 0)

                val saveErrors: Errors = Registration.saveRegistration(database, registration)

                if (saveErrors.hasErrors) {
                  failWithErrors("Failed to login!", saveErrors)
                } else {
                  Ok
                }
              }
          }
        }
    }
  }

  def logout(registrationId: String) = {
    Action.async {
      implicit request: Request[AnyContent] =>
        if (!Option(registrationId).getOrElse("").matches("[a-zA-Z0-9\\-_:]+")) {
          futureFailWithErrors("Failed to logout!", Errors(CommonError.invalidRequest.reason("Registration id was invalid!").data(registrationId)))
        } else {
          val maybeRegistration: Maybe[Registration] = Registration.getRegistration(database, registrationId)

          if (maybeRegistration.hasErrors) {
            futureFailWithErrors("Failed to logout!", maybeRegistration.errors)
          } else {
            val registration: Registration = maybeRegistration.value

            /* Ignore the result of logout request to quup because
             * deleting Registration from database below is gonna make quup Notifications Server forget about the user anyway.
             * Calling this is just to let quup know so they may choose to delete user's session. */
            quupClient.logout(registration)

            val deleteErrors: Errors = Registration.deleteRegistration(database, registration)

            if (deleteErrors.hasErrors) {
              futureFailWithErrors("Failed to logout!", deleteErrors)
            } else {
              Future.successful(Ok)
            }
          }
        }
    }
  }
}
