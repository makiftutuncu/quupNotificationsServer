package utilities

import scala.util.matching.Regex
import scala.concurrent.duration._

object Conf {
  val tickInitialDelay: FiniteDuration = 1.minute
  val tickInterval: FiniteDuration     = 1.minute

  val trackingCookie: String     = "_tracking"
  val trackingCookieRegex: Regex = s"""^.*\\$trackingCookie=([a-zA-Z0-9]+);.*$$""".r

  val sessionCookie: String     = ".gauth"
  val sessionCookieRegex: Regex = s"""^.*\\$sessionCookie=([a-zA-Z0-9]+);.*$$""".r

  val timeoutInMillis: Int = 15000

  val dataPath: String = "data.json"

  object Quup {
    val quupHome: String   = "https://quup.com/welcome"
    val quupLogin: String  = "https://quup.com/a/Member/Logon?returnUrl=%2fwelcome&returnController="
    val quupLogout: String = "https://quup.com/a/LogOff"
  }
  
  object GCM {
    val apiKey: String              = "SOMETHING_MORE_SECRET_THAN_VICTORIA'S"
    val url: String                 = "https://android.googleapis.com/gcm/send"
    val authorizationHeader: String = "Authorization"
    val authorizationKey: String    = "key"
    val registrationIdsKey: String  = "registration_ids"
    val collapseKey: String         = "collapse_key"
    val dataKey: String             = "data"
  }

  object Notifications {
    val url: String                 = "https://quup.com/my/notification/"
    val notificationTypeKey: String = "notificationType"
    val markAsReadKey: String       = "clearNotifications"
  }
}
