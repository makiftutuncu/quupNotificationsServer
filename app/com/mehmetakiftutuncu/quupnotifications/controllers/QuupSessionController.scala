package com.mehmetakiftutuncu.quupnotifications.controllers

import javax.inject.Inject

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe._
import com.mehmetakiftutuncu.quupnotifications.models.{Maybe, QuupNotification, QuupSession}
import com.mehmetakiftutuncu.quupnotifications.utilities.{Database, QuupClient}
import play.api.db.DBApi
import play.api.http.ContentTypes
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Controller, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by akif on 12/07/16.
  */
class QuupSessionController @Inject() (dBApi: DBApi, wsClient: WSClient) extends Controller {
  lazy val database: Database     = Database(dBApi)
  lazy val quupClient: QuupClient = QuupClient(wsClient)

  /* -------------------
   * Logs a user into quup
   * -------------------
   * Expects a Json in request body with following format:
   *
   * {
   *   "registration": "0123456789ABCDEF"
   *   "username": "quupuser",
   *   "password": "Sup3r,5ecuRep@ssw0rD"
   * }
   * ------------------- */
  def login() = {
    Action.async(parse.json) {
      implicit request: Request[JsValue] =>
        val maybeBodyJsObject: Option[JsObject] = request.body.asOpt[JsObject]

        val maybeRegistrationIdUsernameAndPassword: Maybe[(String, String, String)] = {
          if (maybeBodyJsObject.isEmpty) {
            Maybe(Errors(CommonError.invalidRequest.reason("""Request body wasn't a JsObject!""").data(request.body.toString())))
          } else {
            val body: JsObject = maybeBodyJsObject.get

            val maybeRegistration: Maybe[String] = {
              (body \ "registration").toOption.fold[Maybe[String]] {
                Maybe(Errors(CommonError.invalidRequest.reason("""Request body didn't contain "registration"!""").data(body.toString())))
              } {
                registrationJsValue: JsValue =>
                  registrationJsValue.asOpt[String].fold[Maybe[String]] {
                    Maybe(Errors(CommonError.invalidRequest.reason(""""registration" in request body wasn't a String!""").data(registrationJsValue.toString())))
                  } {
                    registration: String => Maybe(registration)
                  }
              }
            }

            val maybeUsername: Maybe[String] = {
              (body \ "username").toOption.fold[Maybe[String]] {
                Maybe(Errors(CommonError.invalidRequest.reason("""Request body didn't contain "username"!""").data(body.toString())))
              } {
                usernameJsValue: JsValue =>
                  usernameJsValue.asOpt[String].fold[Maybe[String]] {
                    Maybe(Errors(CommonError.invalidRequest.reason(""""username" in request body wasn't a String!""").data(usernameJsValue.toString())))
                  } {
                    username: String => Maybe(username)
                  }
              }
            }

            val maybePassword: Maybe[String] = {
              (body \ "password").toOption.fold[Maybe[String]] {
                Maybe(Errors(CommonError.invalidRequest.reason("""Request body didn't contain "password"!""").data(body.toString())))
              } {
                passwordJsValue: JsValue =>
                  passwordJsValue.asOpt[String].fold[Maybe[String]] {
                    Maybe(Errors(CommonError.invalidRequest.reason(""""password" in request body wasn't a String!""").data(passwordJsValue.toString())))
                  } {
                    password: String => Maybe(password)
                  }
              }
            }

            if (maybeRegistration.hasErrors || maybeUsername.hasErrors || maybePassword.hasErrors) {
              Maybe(maybeRegistration.maybeErrors.getOrElse(Errors.empty) ++ maybeUsername.maybeErrors.getOrElse(Errors.empty) ++ maybePassword.maybeErrors.getOrElse(Errors.empty))
            } else {
              Maybe((maybeRegistration.value, maybeUsername.value, maybePassword.value))
            }
          }
        }

        if (maybeRegistrationIdUsernameAndPassword.hasErrors) {
          Future.successful(BadRequest(maybeRegistrationIdUsernameAndPassword.errors.toString()).as(ContentTypes.JSON))
        } else {
          val (registration: String, username: String, password: String) = maybeRegistrationIdUsernameAndPassword.value

          quupClient.login(username, password).map {
            maybeSession: Maybe[String] =>
              if (maybeSession.hasErrors) {
                BadRequest(maybeSession.errors.toString()).as(ContentTypes.JSON)
              } else {
                val quupSession: QuupSession = QuupSession(registration, maybeSession.value)

                val saveErrors: Errors = database.saveQuupSession(quupSession)

                if (saveErrors.hasErrors) {
                  BadRequest(saveErrors.toString()).as(ContentTypes.JSON)
                } else {
                  Ok(quupSession.toJson).as(ContentTypes.JSON)
                }
              }
          }
        }
    }
  }

  /* -------------------
   * Logs a user out of quup
   * -------------------
   * Expects a Json in request body with following format:
   *
   * {
   *   "registration": "0123456789ABCDEF"
   * }
   * ------------------- */
  def logout(registration: String) = {
    Action.async {
      implicit request: Request[AnyContent] =>
        if (!Option(registration).getOrElse("").matches("[a-zA-Z0-9]+")) {
          Future.successful(BadRequest(Errors(CommonError.invalidRequest.reason("""Registration was invalid!""").data(registration)).toString()).as(ContentTypes.JSON))
        } else {
          val maybeQuupSession: Maybe[QuupSession] = database.getQuupSession(registration)

          if (maybeQuupSession.hasErrors) {
            Future.successful(BadRequest(maybeQuupSession.errors.toString()).as(ContentTypes.JSON))
          } else {
            val quupSession: QuupSession = maybeQuupSession.value

            quupClient.logout(quupSession).map {
              logoutErrors: Errors =>
                if (logoutErrors.hasErrors) {
                  // TODO Log
                  println(logoutErrors)
                }
            }.recover {
              case t: Throwable =>
              // TODO Log
              t.printStackTrace()
            }

            val deleteErrors: Errors = database.deleteQuupSession(quupSession)

            if (deleteErrors.hasErrors) {
              Future.successful(BadRequest(deleteErrors.toString()).as(ContentTypes.JSON))
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
              BadRequest(maybeNotifications.errors.toString()).as(ContentTypes.JSON)
            } else {
              Ok(Json.toJson(maybeNotifications.value.map(_.toJson))).as(ContentTypes.JSON)
            }
        }
    }
  }
}
