package com.mehmetakiftutuncu.quupnotifications.utilities

import java.util.concurrent.TimeUnit

import play.api.{Configuration, Environment}

import scala.concurrent.duration.FiniteDuration

case class Conf(private val environment: Environment) {
  private lazy val conf: Configuration = Configuration.load(environment)

  def getFiniteDuration(key: String, defaultValue: FiniteDuration): FiniteDuration = conf.getMilliseconds(key).map(FiniteDuration(_, TimeUnit.MILLISECONDS)).getOrElse(defaultValue)
  def getString(key: String, defaultValue: String): String                         = conf.getString(key).getOrElse(defaultValue)

  object Common {
    val wsTimeout: FiniteDuration = getFiniteDuration("qns.common.wsTimeout", FiniteDuration(15, TimeUnit.SECONDS))
  }

  object Url {
    private val markAsReadFlagName: String                 = getString("qns.url.markAsReadFlagName", "notUnRead")

    val login: String                                      = getString("qns.url.login",         s"https://quup.com/member/logon")
    def notifications(markAsRead: Boolean = false): String = getString("qns.url.notifications", s"https://quup.com/social/notification${if (markAsRead) s"?$markAsReadFlagName=true" else ""}")
    val logout: String                                     = getString("qns.url.logout",        s"https://quup.com/social/member/me/logoff")
  }

  object Login {
    val sessionKey: String  = getString("qns.login.sessionKey",  ".qauth")
    val usernameKey: String = getString("qns.login.usernameKey", "Email")
    val passwordKey: String = getString("qns.login.passwordKey", "Password")
  }

  object Notifications {
    val interval: FiniteDuration = getFiniteDuration("qns.notifications.interval", FiniteDuration(30, TimeUnit.SECONDS))
  }

  object GCM {
    val url: String                 = "https://android.googleapis.com/gcm/send"
    val authorizationHeader: String = "Authorization"
    val authorizationKey: String    = "key"
    val registrationIdsKey: String  = "registration_ids"
    val collapseKey: String         = "collapse_key"
    val dataKey: String             = "data"
    val apiKey: String              = getString("qns.gcm.apiKey", "")
  }
}
