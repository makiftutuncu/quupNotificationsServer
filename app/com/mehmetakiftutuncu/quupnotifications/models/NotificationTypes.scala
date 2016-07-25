package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe.Maybe
import com.mehmetakiftutuncu.quupnotifications.utilities.{Log, Loggable}

import scala.util.matching.Regex

object NotificationTypes extends Loggable {
  sealed trait NotificationType {val regex: Regex}

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
