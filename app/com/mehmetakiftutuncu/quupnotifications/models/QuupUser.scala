package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import play.api.libs.json.{JsObject, JsValue, Json}

/**
  * Created by akif on 30/06/16.
  */
case class QuupUser(userId: String,
                    userName: String,
                    fullName: String) {
  val profileUrl: String = s"""/@$userName"""
  val avatarUrl: String  = s"""/social/avatar/$userId"""

  def toJson: JsObject = {
    Json.obj(
      "userId"   -> userId,
      "userName" -> userName,
      "fullName" -> fullName
    )
  }

  override def toString: String = toJson.toString()
}

object QuupUser {
  def from(json: JsValue): Maybe[QuupUser] = {
    try {
      val maybeUserIdJson: Option[JsValue]   = (json \ "me_mi").toOption
      val maybeUserNameJson: Option[JsValue] = (json \ "me_un").toOption
      val maybeFullNameJson: Option[JsValue] = (json \ "me_dn").toOption

      val (maybeUserId: Option[String], userIdErrors: Errors) = if (maybeUserIdJson.isEmpty) {
        None -> Errors(CommonError.invalidData.reason("""Failed to parse userId of QuupUser because "me_mi" key for is missing!""").data(json.toString()))
      } else {
        val maybeUserId: Option[String] = maybeUserIdJson.get.asOpt[String]

        if (maybeUserId.isEmpty) {
          None -> Errors(CommonError.invalidData.reason("""Failed to parse userId of QuupUser because value of "me_mi" wasn't a String!""").data(maybeUserIdJson.get.toString()))
        } else {
          maybeUserId -> Errors.empty
        }
      }

      val (maybeUserName: Option[String], userNameErrors: Errors) = if (maybeUserNameJson.isEmpty) {
        None -> Errors(CommonError.invalidData.reason("""Failed to parse userName of QuupUser because "me_un" key for is missing!""").data(json.toString()))
      } else {
        val maybeUserName: Option[String] = maybeUserNameJson.get.asOpt[String]

        if (maybeUserName.isEmpty) {
          None -> Errors(CommonError.invalidData.reason("""Failed to parse userName of QuupUser because value of "me_un" wasn't a String!""").data(maybeUserNameJson.get.toString()))
        } else {
          maybeUserName -> Errors.empty
        }
      }

      val (maybeFullName: Option[String], fullNameErrors: Errors) = if (maybeFullNameJson.isEmpty) {
        None -> Errors(CommonError.invalidData.reason("""Failed to parse fullName of QuupUser because "me_dn" key for is missing!""").data(json.toString()))
      } else {
        val maybeFullName: Option[String] = maybeFullNameJson.get.asOpt[String]

        if (maybeFullName.isEmpty) {
          None -> Errors(CommonError.invalidData.reason("""Failed to parse fullName of QuupUser because value of "me_dn" wasn't a String!""").data(maybeFullNameJson.get.toString()))
        } else {
          maybeFullName -> Errors.empty
        }
      }

      val errors: Errors = userIdErrors ++ userNameErrors ++ fullNameErrors

      if (errors.hasErrors) {
        // Something went wrong
        Maybe(errors)
      } else {
        // Everything was fine
        val userId: String   = maybeUserId.get
        val userName: String = maybeUserName.get
        val fullName: String = maybeFullName.get

        Maybe(QuupUser(userId, userName, fullName))
      }
    } catch {
      case t: Throwable =>
        Maybe(Errors(CommonError.invalidData.reason("Failed to parse QuupUser!").data(json.toString())))
    }
  }
}
