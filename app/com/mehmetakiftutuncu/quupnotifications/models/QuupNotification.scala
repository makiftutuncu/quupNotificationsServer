package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.{Maybe, _}
import com.mehmetakiftutuncu.quupnotifications.utilities.{Log, Loggable}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

import scala.util.Try
import scala.util.matching.Regex

case class QuupNotification(quupId: Option[String],
                            notificationType: NotificationType,
                            when: Long,
                            by: QuupUser,
                            others: Set[QuupUser] = Set.empty[QuupUser]) {
  def switchByTo(quupUser: QuupUser): QuupNotification = {
    if (quupUser.userId == by.userId) {
      this
    } else {
      copy(by = quupUser, others = others + by)
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

object QuupNotification extends Loggable  {
  def getQuupNotificationList(json: JsValue): Maybe[List[QuupNotification]] = {
    (json \ "Data").toOption.fold[Maybe[List[QuupNotification]]] {
      val errors: Errors = Errors(CommonError.invalidData.reason(""""Data" key is missing!""").data(json.toString()))
      Log.error("Failed to get QuupNotification list!", errors)

      Maybe(errors)
    } {
      dataJsValue: JsValue =>
        dataJsValue.asOpt[JsArray].fold[Maybe[List[QuupNotification]]] {
          val errors: Errors = Errors(CommonError.invalidData.reason(""""Data" wasn't a JsArray!""").data(dataJsValue.toString()))
          Log.error("Failed to get QuupNotification list!", errors)

          Maybe(errors)
        } {
          dataJsArray: JsArray =>
            val quupNotificationsJsonList: List[JsValue] = dataJsArray.value.toList

            val (errors: Errors, quupNotifications: List[QuupNotification]) = {
              quupNotificationsJsonList.foldLeft(Errors.empty -> List.empty[QuupNotification]) {
                case ((errors: Errors, quupNotifications: List[QuupNotification]), quupNotificationJson: JsValue) =>
                  val maybeQuupNotification: Maybe[QuupNotification] = getQuupNotification(quupNotificationJson)

                  val newErrors: Errors                            = errors ++ maybeQuupNotification.maybeErrors.getOrElse(Errors.empty)
                  val newQuupNotifications: List[QuupNotification] = maybeQuupNotification.maybeValue.map(quupNotifications :+ _).getOrElse(quupNotifications)

                  newErrors -> newQuupNotifications
              }
            }

            if (errors.hasErrors) {
              Log.error("Failed to get QuupNotification list!", errors)

              Maybe(errors)
            } else {
              Maybe(quupNotifications.sortWith((qn1: QuupNotification, qn2: QuupNotification) => qn1.when > qn2.when))
            }
        }
    }
  }

  private def getQuupNotification(json: JsValue): Maybe[QuupNotification] = {
    (json \ "n").toOption.fold[Maybe[QuupNotification]] {
      val errors: Errors = Errors(CommonError.invalidData.reason(""""n" key is missing!""").data(json.toString()))
      Log.error("Failed to get QuupNotification!", errors)

      Maybe(errors)
    } {
      nJsValue: JsValue =>
        nJsValue.asOpt[JsArray].fold[Maybe[QuupNotification]] {
          val errors: Errors = Errors(CommonError.invalidData.reason(""""n" wasn't a JsArray!""").data(nJsValue.toString()))
          Log.error("Failed to get QuupNotification!", errors)

          Maybe(errors)
        } {
          nJsArray: JsArray =>
            val quupNotificationsJsonList: List[JsValue] = nJsArray.value.reverse.toList

            val (errors: Errors, maybeQuupNotification: Option[QuupNotification]) = {
              quupNotificationsJsonList.foldLeft(Errors.empty -> Option.empty[QuupNotification]) {
                case ((errors: Errors, currentQuupNotification: Option[QuupNotification]), quupNotificationJson: JsValue) =>
                  val maybeQuupNotification: Maybe[QuupNotification] = from(quupNotificationJson)

                  val newErrors: Errors = errors ++ maybeQuupNotification.maybeErrors.getOrElse(Errors.empty)

                  val newCurrentQuupNotification: Option[QuupNotification] = currentQuupNotification.flatMap {
                    quupNotification: QuupNotification =>
                      maybeQuupNotification.maybeValue map {
                        newQuupNotification: QuupNotification =>
                          quupNotification.switchByTo(newQuupNotification.by)
                      }
                  }.orElse {
                    maybeQuupNotification.maybeValue
                  }

                  newErrors -> newCurrentQuupNotification
              }
            }

            if (errors.hasErrors) {
              Log.error("Failed to get QuupNotification!", errors)

              Maybe(errors)
            } else {
              Maybe(maybeQuupNotification.get)
            }
        }
    }
  }

  private def from(json: JsValue): Maybe[QuupNotification] = {
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

      val maybeBy: Maybe[QuupUser] = QuupUser.from(json)
      val byErrors: Errors         = maybeBy.maybeErrors.getOrElse(Errors.empty)

      val errors: Errors = quupIdErrors ++ notificationTypeErrors ++ whenErrors ++ byErrors

      if (errors.hasErrors) {
        Log.error("Failed to parse QuupNotification!", errors)

        Maybe(errors)
      } else {
        val notificationType: NotificationType = maybeNotificationType.get
        val when: Long                         = maybeWhen.get
        val by: QuupUser                       = maybeBy.value

        Maybe(QuupNotification(quupId, notificationType, when, by))
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.invalidData.reason("Failed to parse QuupNotification!").data(json.toString()))
        Log.error("Failed to parse QuupNotification with exception!", errors, t)

        Maybe(errors)
    }
  }
}

sealed trait NotificationType {
  val regex: Regex
}

object NotificationTypes extends Loggable  {
  case object QuupLike    extends NotificationType {override val regex: Regex = """.+gönderini.+beğendi.*""".r}
  case object CommentLike extends NotificationType {override val regex: Regex = """.+yorumunu.+beğendi.*""".r}
  case object Comment     extends NotificationType {override val regex: Regex = """.+yorum.+yaptı.*""".r}
  case object Mention     extends NotificationType {override val regex: Regex = """.+senden.+bahsetti.*""".r}
  case object Message     extends NotificationType {override val regex: Regex = """.+özel.+mesaj.*""".r}
  case object Follow      extends NotificationType {override val regex: Regex = """.+takip.*""".r}
  case object Share       extends NotificationType {override val regex: Regex = """.+paylaştı.*""".r}

  private val types: Set[NotificationType] = Set(QuupLike, CommentLike, Comment, Mention, Message, Follow, Share)

  def from(description: String): Maybe[NotificationType] = {
    val maybeNotificationType: Option[NotificationType] = types.find(_.regex.findFirstMatchIn(description).isDefined)

    if (maybeNotificationType.isEmpty) {
      val errors: Errors = Errors(CommonError.invalidData.reason("""Description didn't match to any NotificationType!""").data(description))
      Log.error("Failed to parse NotificationType!", errors)

      Maybe(errors)
    } else {
      Maybe(maybeNotificationType.get)
    }
  }
}
