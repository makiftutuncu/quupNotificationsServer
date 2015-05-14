package utilities

object Conf {
  val cookie: String       = ".gauth"
  val timeoutInMillis: Int = 10000

  object Auth {
    val logoutUrl: String = "https://quup.com/a/LogOff"
  }

  object Notifications {
    val url: String                 = "https://quup.com/my/notification/"
    val notificationTypeKey: String = "notificationType"
    val markAsReadKey: String       = "clearNotifications"
  }
}
