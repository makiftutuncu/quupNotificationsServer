package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.mehmetakiftutuncu.quupnotifications.utilities.{Log, Loggable}
import play.api.libs.json.{JsObject, JsValue, Json}

case class User(id: String, userName: String, fullName: String) {
  def toJson: JsObject = {
    Json.obj(
      "id"       -> id,
      "userName" -> userName,
      "fullName" -> fullName
    )
  }

  override def toString: String = toJson.toString()
}

object User extends Loggable {
  def from(json: JsValue): Maybe[User] = {
    try {
      val maybeIdJson: Option[JsValue]       = (json \ "me_mi").toOption
      val maybeUserNameJson: Option[JsValue] = (json \ "me_un").toOption
      val maybeFullNameJson: Option[JsValue] = (json \ "me_dn").toOption

      val (maybeId: Option[String], idErrors: Errors) = {
        if (maybeIdJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""me_mi" key is missing!""").data(json.toString()))
        } else {
          val maybeId: Option[String] = maybeIdJson.get.asOpt[String]

          if (maybeId.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""me_mi" wasn't a String!""").data(maybeIdJson.get.toString()))
          } else {
            maybeId -> Errors.empty
          }
        }
      }

      val (maybeUserName: Option[String], userNameErrors: Errors) = {
        if (maybeUserNameJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""me_un" key is missing!""").data(json.toString()))
        } else {
          val maybeUserName: Option[String] = maybeUserNameJson.get.asOpt[String]

          if (maybeUserName.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""me_un" wasn't a String!""").data(maybeUserNameJson.get.toString()))
          } else {
            maybeUserName -> Errors.empty
          }
        }
      }

      val (maybeFullName: Option[String], fullNameErrors: Errors) = {
        if (maybeFullNameJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""me_dn" key is missing!""").data(json.toString()))
        } else {
          val maybeFullName: Option[String] = maybeFullNameJson.get.asOpt[String]

          if (maybeFullName.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""me_dn" wasn't a String!""").data(maybeFullNameJson.get.toString()))
          } else {
            maybeFullName -> Errors.empty
          }
        }
      }

      val errors: Errors = idErrors ++ userNameErrors ++ fullNameErrors

      if (errors.hasErrors) {
        Log.error("Failed to parse User!", errors)

        Maybe(errors)
      } else {
        val id: String       = maybeId.get
        val userName: String = maybeUserName.get
        val fullName: String = maybeFullName.get

        Maybe(User(id, userName, fullName))
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.invalidData.reason(t.getMessage).data(json.toString()))
        Log.error("Failed to parse User with exception!", errors, t)

        Maybe(errors)
    }
  }
}
