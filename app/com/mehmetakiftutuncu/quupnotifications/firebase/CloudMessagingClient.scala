package com.mehmetakiftutuncu.quupnotifications.firebase

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import com.mehmetakiftutuncu.quupnotifications.utilities._
import play.api.http.{ContentTypes, Status}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
case class CloudMessagingClient @Inject()(Conf: ConfBase,
                                          Registrations: RegistrationsBase,
                                          WSClient: WSClient) extends CloudMessagingClientBase

@ImplementedBy(classOf[CloudMessagingClient])
trait CloudMessagingClientBase extends Loggable {
  protected val Conf: ConfBase
  protected val Registrations: RegistrationsBase
  protected val WSClient: WSClient

  def push(registration: Registration, notifications: List[Notification]): Future[Errors] = {
    val request: WSRequest = WSClient.url(Conf.CloudMessagingClient.url)
                                     .withRequestTimeout(Conf.Common.wsTimeout)
                                     .withHeaders(Conf.CloudMessagingClient.authorizationHeader -> s"${Conf.CloudMessagingClient.authorizationKey}=${Conf.CloudMessagingClient.apiKey}")

    val data: JsObject = Json.obj(
      Conf.CloudMessagingClient.toKey       -> registration.registrationId,
      Conf.CloudMessagingClient.collapseKey -> registration.registrationId,
      Conf.CloudMessagingClient.dataKey     -> Json.obj("notifications" -> notifications.map(_.toJson))
    )

    Log.debug(s"""Pushing "${notifications.size}" notifications to registration id "${registration.registrationId}"...""")

    request.post(data).map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header("Content-Type").getOrElse("")

        if (status != Status.OK) {
          val errors: Errors = Errors(CommonError.requestFailed.reason(s"""FCM returned invalid status "$status"!""").data(wsResponse.body))

          Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

          errors
        } else if (!contentType.contains(ContentTypes.JSON)) {
          val errors: Errors = Errors(CommonError.requestFailed.reason(s"""FCM returned invalid Content-Type "$contentType"!""").data(wsResponse.body))

          Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

          errors
        } else {
          val json: JsValue = wsResponse.json

          val success: Int = (json \ "success").asOpt[Int].getOrElse(-1)

          if (success != 1) {
            val errors: Errors = Errors(CommonError.requestFailed.reason("""FCM returned invalid data!""").data(json.toString()))

            Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

            (json \ "results").asOpt[JsArray].flatMap(_.value.headOption).flatMap(r => (r \ "error").asOpt[String]).foreach {
              case reason: String if reason == "NotRegistered" || reason == "InvalidRegistration" =>
                Registrations.delete(registration)

              case _ =>
            }

            errors
          } else {
            Log.debug(s"""Pushed ${notifications.size} notifications to registration id "${registration.registrationId}"!""")

            Errors.empty
          }
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed.reason(t.getMessage))

        Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors, t)

        errors
    }
  }
}
