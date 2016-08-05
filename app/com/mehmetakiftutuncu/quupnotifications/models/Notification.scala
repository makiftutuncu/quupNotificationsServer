package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.mehmetakiftutuncu.quupnotifications.models.NotificationTypes.NotificationType
import com.mehmetakiftutuncu.quupnotifications.utilities.{Log, Loggable}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.util.Try

case class Notification(quupId: Option[String],
                        notificationType: NotificationType,
                        when: Long,
                        by: User,
                        others: Set[User] = Set.empty[User]) {
  def switchByTo(user: User): Notification = {
    if (user.id == by.id) {
      this
    } else {
      copy(by = user, others = others + by)
    }
  }

  def toJson: JsObject = {
    Json.obj(
      "quupId" -> quupId,
      "type"   -> notificationType.toString,
      "when"   -> when,
      "by"     -> by.toJson,
      "others" -> others.map(_.toJson)
    )
  }

  override def toString: String = toJson.toString()
}

object Notification extends Loggable  {
  def getNotificationList(json: JsValue): Maybe[List[Notification]] = {
    (json \ "Data").toOption.fold[Maybe[List[Notification]]] {
      val errors: Errors = Errors(CommonError.invalidData.reason(""""Data" key is missing!""").data(json.toString()))

      Log.error("Failed to get Notification list!", errors)

      Maybe(errors)
    } {
      dataJsValue: JsValue =>
        dataJsValue.asOpt[JsArray].fold[Maybe[List[Notification]]] {
          val errors: Errors = Errors(CommonError.invalidData.reason(""""Data" wasn't a JsArray!""").data(dataJsValue.toString()))

          Log.error("Failed to get Notification list!", errors)

          Maybe(errors)
        } {
          dataJsArray: JsArray =>
            val notificationsJsonList: List[JsValue] = dataJsArray.value.toList

            val (errors: Errors, notifications: List[Notification]) = {
              notificationsJsonList.foldLeft(Errors.empty -> List.empty[Notification]) {
                case ((errors: Errors, notifications: List[Notification]), notificationJson: JsValue) =>
                  val maybeNotification: Maybe[Notification] = getNotification(notificationJson)

                  val newErrors: Errors                    = errors ++ maybeNotification.maybeErrors.getOrElse(Errors.empty)
                  val newNotifications: List[Notification] = maybeNotification.maybeValue.map(notifications :+ _).getOrElse(notifications)

                  newErrors -> newNotifications
              }
            }

            if (errors.hasErrors) {
              Log.error("Failed to get Notification list!", errors)

              Maybe(errors)
            } else {
              Maybe(notifications.sortWith((n1: Notification, n2: Notification) => n1.when > n2.when))
            }
        }
    }
  }

  private def getNotification(json: JsValue): Maybe[Notification] = {
    (json \ "n").toOption.fold[Maybe[Notification]] {
      val errors: Errors = Errors(CommonError.invalidData.reason(""""n" key is missing!""").data(json.toString()))

      Log.error("Failed to get Notification!", errors)

      Maybe(errors)
    } {
      nJsValue: JsValue =>
        nJsValue.asOpt[JsArray].fold[Maybe[Notification]] {
          val errors: Errors = Errors(CommonError.invalidData.reason(""""n" wasn't a JsArray!""").data(nJsValue.toString()))

          Log.error("Failed to get Notification!", errors)

          Maybe(errors)
        } {
          nJsArray: JsArray =>
            val notificationsJsonList: List[JsValue] = nJsArray.value.reverse.toList

            val (errors: Errors, maybeNotification: Option[Notification]) = {
              notificationsJsonList.foldLeft(Errors.empty -> Option.empty[Notification]) {
                case ((errors: Errors, currentNotification: Option[Notification]), notificationJson: JsValue) =>
                  val maybeNotification: Maybe[Notification] = from(notificationJson)

                  val newErrors: Errors = errors ++ maybeNotification.maybeErrors.getOrElse(Errors.empty)

                  val newCurrentNotification: Option[Notification] = currentNotification.flatMap {
                    notification: Notification =>
                      maybeNotification.maybeValue map {
                        newNotification: Notification => notification.switchByTo(newNotification.by)
                      }
                  }.orElse {
                    maybeNotification.maybeValue
                  }

                  newErrors -> newCurrentNotification
              }
            }

            if (errors.hasErrors) {
              Log.error("Failed to get Notification!", errors)

              Maybe(errors)
            } else {
              Maybe(maybeNotification.get)
            }
        }
    }
  }

  private def from(json: JsValue): Maybe[Notification] = {
    try {
      val maybeQuupIdJson: Option[JsValue]      = (json \ "ei").toOption
      val maybeDescriptionJson: Option[JsValue] = (json \ "dt").toOption
      val maybeWhenJson: Option[JsValue]        = (json \ "cs").toOption

      val (maybeNotificationType: Option[NotificationType], notificationTypeErrors: Errors) = {
        if (maybeDescriptionJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""dt" key is missing!""").data(json.toString()))
        } else {
          val maybeDescription: Option[String] = maybeDescriptionJson.get.asOpt[String]

          if (maybeDescription.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""dt" wasn't a String!""").data(maybeDescriptionJson.get.toString()))
          } else {
            val maybeNotificationType: Maybe[NotificationType] = NotificationTypes.from(maybeDescription.get)

            maybeNotificationType.maybeValue -> maybeNotificationType.maybeErrors.getOrElse(Errors.empty)
          }
        }
      }

      val (quupId: Option[String], quupIdErrors: Errors) = {
        if (maybeNotificationType.isDefined && maybeNotificationType.contains(NotificationTypes.Follow)) {
          None -> Errors.empty
        } else if (maybeQuupIdJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""ei" key is missing!""").data(json.toString()))
        } else {
          val maybeQuupId: Option[String] = maybeQuupIdJson.get.asOpt[String]

          if (maybeQuupId.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""ei" wasn't a String!""").data(maybeQuupIdJson.get.toString()))
          } else {
            maybeQuupId -> Errors.empty
          }
        }
      }

      val (maybeWhen: Option[Long], whenErrors: Errors) = {
        if (maybeWhenJson.isEmpty) {
          None -> Errors(CommonError.invalidData.reason(""""cs" key is missing!""").data(json.toString()))
        } else {
          val maybeWhen: Option[Long] = maybeWhenJson.get.asOpt[String] flatMap {
            whenString: String =>
              Try.apply(whenString.toLong).toOption
          }

          if (maybeWhen.isEmpty) {
            None -> Errors(CommonError.invalidData.reason(""""cs" wasn't a Long!""").data(maybeWhenJson.get.toString()))
          } else {
            maybeWhen -> Errors.empty
          }
        }
      }

      val maybeBy: Maybe[User] = User.from(json)
      val byErrors: Errors         = maybeBy.maybeErrors.getOrElse(Errors.empty)

      val errors: Errors = quupIdErrors ++ notificationTypeErrors ++ whenErrors ++ byErrors

      if (errors.hasErrors) {
        Log.error("Failed to parse Notification!", errors)

        Maybe(errors)
      } else {
        val notificationType: NotificationType = maybeNotificationType.get
        val when: Long                         = maybeWhen.get
        val by: User                           = maybeBy.value

        Maybe(Notification(quupId, notificationType, when, by))
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.invalidData.reason("Failed to parse Notification!").data(json.toString()))

        Log.error("Failed to parse Notification with exception!", errors, t)

        Maybe(errors)
    }
  }
}
