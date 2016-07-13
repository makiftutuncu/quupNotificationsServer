package com.mehmetakiftutuncu.quupnotifications.utilities

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

object Conf {
  object Common {
    val wsTimeout: FiniteDuration = FiniteDuration(15, TimeUnit.SECONDS)
  }

  object Url {
    val login: String                                      = "https://quup.com/member/logon"
    def notifications(markAsRead: Boolean = false): String = s"https://quup.com/social/notification${if (markAsRead) "" else "?notUnRead=true"}"
    val logout: String                                     = "https://quup.com/social/member/me/logoff"
  }

  object Login {
    val sessionKey: String = ".qauth"

    val usernameKey: String = "Email"
    val passwordKey: String = "Password"
  }
}
