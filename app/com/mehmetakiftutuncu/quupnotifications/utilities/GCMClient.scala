package com.mehmetakiftutuncu.quupnotifications.utilities

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.{Notification, Registration}
import play.api.http.{ContentTypes, Status}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GCMClient(private val conf: Conf,
                     private val database: Database,
                     private val wsClient: WSClient) extends Loggable {
  def push(registration: Registration, notifications: List[Notification]): Future[Errors] = {
    val request: WSRequest = wsClient.url(conf.GCM.url)
                                     .withRequestTimeout(conf.Common.wsTimeout)
                                     .withHeaders(conf.GCM.authorizationHeader -> s"${conf.GCM.authorizationKey}=${conf.GCM.apiKey}")

    val data: JsObject = Json.obj(
      conf.GCM.registrationIdsKey -> Json.arr(registration.registrationId),
      conf.GCM.collapseKey        -> registration.registrationId,
      conf.GCM.dataKey            -> notifications.map(_.toJson)
    )

    Log.debug(s"""Pushing "${notifications.size}" notifications to registration id "${registration.registrationId}"...""")

    request.post(data).map {
      wsResponse: WSResponse =>
        val status: Int         = wsResponse.status
        val contentType: String = wsResponse.header("Content-Type").getOrElse("")

        if (status != Status.OK) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("GCM returned invalid status!").data(status.toString))

          Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

          errors
        } else if (!contentType.contains(ContentTypes.JSON)) {
          val errors: Errors = Errors(CommonError.requestFailed.reason("""GCM returned invalid "Content-Type"!""").data(contentType))

          Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

          errors
        } else {
          val json: JsValue = wsResponse.json

          val success: Int = (json \ "success").asOpt[Int].getOrElse(-1)

          if (success != 1) {
            val errors: Errors = Errors(CommonError.requestFailed.reason("""GCM returned invalid data!""").data(json.toString()))

            Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors)

            (json \ "results").asOpt[JsArray].flatMap(_.value.headOption).flatMap(r => (r \ "error").asOpt[String]).foreach {
              case reason: String if reason == "NotRegistered" || reason == "InvalidRegistration" =>
                Registration.deleteRegistration(database, registration)

              case _ =>
            }

            errors
          } else {
            Log.warn(s"""Pushed ${notifications.size} notifications to registration id "${registration.registrationId}"!""")

            Errors.empty
          }
        }
    }.recover {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.requestFailed)

        Log.error(s"""Pushing "${notifications.size}" notifications to "${registration.registrationId}" failed!""", errors, t)

        errors
    }
  }
}
