package com.mehmetakiftutuncu.quupnotifications.utilities

import java.util.concurrent.TimeUnit

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.{Configuration, Environment}

import scala.concurrent.duration.FiniteDuration

@Singleton
case class Conf @Inject()(Environment: Environment) extends ConfBase

@ImplementedBy(classOf[Conf])
trait ConfBase {
  protected val Environment: Environment

  private lazy val conf: Configuration = Configuration.load(Environment)

  def getFiniteDuration(key: String, defaultValue: FiniteDuration): FiniteDuration = conf.getMilliseconds(key).map(FiniteDuration(_, TimeUnit.MILLISECONDS)).getOrElse(defaultValue)
  def getString(key: String, defaultValue: String): String                         = conf.getString(key).getOrElse(defaultValue)

  object Common {
    val wsTimeout: FiniteDuration = getFiniteDuration("qns.common.wsTimeout", FiniteDuration(15, TimeUnit.SECONDS))
  }

  object Url {
    val leaveAsUnreadFlagName: String = getString("qns.url.leaveAsUnreadFlagName", "notUnRead")

    val login: String         = getString("qns.url.login",         "https://quup.com/member/logon")
    val notifications: String = getString("qns.url.notifications", "https://quup.com/social/notification")
    val logout: String        = getString("qns.url.logout",        "https://quup.com/social/member/me/logoff/new")
  }

  object Login {
    val sessionKey: String  = getString("qns.login.sessionKey",  ".qauth")
    val usernameKey: String = getString("qns.login.usernameKey", "Email")
    val passwordKey: String = getString("qns.login.passwordKey", "Password")
  }

  object Notifications {
    val interval: FiniteDuration = getFiniteDuration("qns.notifications.interval", FiniteDuration(30, TimeUnit.SECONDS))
  }

  object Heartbeat {
    val host: String             = getString("qns.heartbeat.host", "https://quupnotifications.herokuapp.com")
    val interval: FiniteDuration = getFiniteDuration("qns.heartbeat.interval", FiniteDuration(5, TimeUnit.MINUTES))
  }

  object CloudMessagingClient {
    val url: String                 = "https://fcm.googleapis.com/fcm/send"
    val authorizationHeader: String = "Authorization"
    val authorizationKey: String    = "key"
    val toKey: String               = "to"
    val collapseKey: String         = "collapse_key"
    val dataKey: String             = "data"
    val apiKey: String              = getString("qns.cloudMessagingClient.apiKey", "")
  }

  object RealtimeDatabase {
    val credentialsFilePath: String = getString("qns.realtimeDatabase.credentialsFilePath", "conf/credentials.json")
    val databaseName: String        = getString("qns.realtimeDatabase.databaseName",        "quup-notifications")
  }
}
