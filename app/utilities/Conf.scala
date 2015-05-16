package utilities

import scala.util.matching.Regex

object Conf {
  val trackingCookie: String     = "_tracking"
  val trackingCookieRegex: Regex = s"""^.*\\$trackingCookie=([a-zA-Z0-9]+);.*$$""".r

  val sessionCookie: String     = ".gauth"
  val sessionCookieRegex: Regex = s"""^.*\\$sessionCookie=([a-zA-Z0-9]+);.*$$""".r

  val timeoutInMillis: Int = 15000

  object Url {
    val quupHome: String   = "https://quup.com/welcome"
    val quupLogin: String  = "https://quup.com/a/Member/Logon?returnUrl=%2fwelcome&returnController="
    val quupLogout: String = "https://quup.com/a/LogOff"
  }

  object Notifications {
    val url: String                 = "https://quup.com/my/notification/"
    val notificationTypeKey: String = "notificationType"
    val markAsReadKey: String       = "clearNotifications"
  }
}
