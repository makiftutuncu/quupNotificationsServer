package com.mehmetakiftutuncu.quupnotifications.controllers

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.google.inject.Inject
import com.mehmetakiftutuncu.quupnotifications.models.Registration
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationController @Inject()(Conf: ConfBase,
                                       QuupClient: QuupClient,
                                       Registrations: RegistrationsBase) extends ControllerBase {
  def login: Action[JsValue] = {
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

          QuupClient.login(username, password).flatMap {
            maybeSessionId: Maybe[String] =>
              if (maybeSessionId.hasErrors) {
                futureFailWithErrors("Failed to login!", maybeSessionId.errors)
              } else {
                val registration: Registration = Registration(registrationId, maybeSessionId.value, 0)

                Registrations.save(registration).map {
                  saveErrors: Errors =>
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
  }

  def logout(registrationId: String): Action[AnyContent] = {
    Action.async {
      implicit request: Request[AnyContent] =>
        if (!Option(registrationId).getOrElse("").matches("[a-zA-Z0-9\\-_:]+")) {
          futureFailWithErrors("Failed to logout!", Errors(CommonError.invalidRequest.reason("Registration id was invalid!").data(registrationId)))
        } else {
          Registrations.get(registrationId).flatMap {
            maybeRegistration: Maybe[Registration] =>
              if (maybeRegistration.hasErrors) {
                futureFailWithErrors("Failed to logout!", maybeRegistration.errors)
              } else {
                val registration: Registration = maybeRegistration.value

                /* Ignore the result of logout request to quup because
                 * deleting Registration from database below is gonna make quup Notifications Server forget about the user anyway.
                 * Calling this is just to let quup know so they may choose to delete user's session. */
                QuupClient.logout(registration)

                Registrations.delete(registration).map {
                  deleteErrors: Errors =>
                    if (deleteErrors.hasErrors) {
                      failWithErrors("Failed to logout!", deleteErrors)
                    } else {
                      Ok
                    }
                }
              }
          }
        }
    }
  }
}
