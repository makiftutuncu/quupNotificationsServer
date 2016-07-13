package com.mehmetakiftutuncu.quupnotifications.utilities

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.models.Maybe
import com.mehmetakiftutuncu.quupnotifications.models.Maybe._
import play.api.http.ContentTypes
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{Controller, Request, Result}

import scala.concurrent.Future

trait ControllerBase extends Controller with Loggable {
  protected def extract[T](key: String)(implicit request: Request[JsValue], reads: Reads[T]): Maybe[T] = {
    def hidePassword(s: String): String = {
      // We don't want to log people's passwords so do your best to hide it!
      s.replaceAll(""""([passwordPASSWORD]+)":.+""", """"$1":"HIDDEN"""")
    }

    val body: JsValue = request.body

    (body \ key).toOption.fold[Maybe[T]] {
      Maybe(Errors(CommonError.invalidRequest.reason(s"""Request body didn't contain "$key"!""").data(hidePassword(body.toString()))))
    } {
      _.asOpt[T].fold[Maybe[T]] {
        Maybe(Errors(CommonError.invalidRequest.reason(s""""$key" in request body wasn't of requested type!""").data(hidePassword(body.toString()))))
      } {
        value: T => Maybe(value)
      }
    }
  }

  protected def success(result: JsValue): Result = {
    Ok(result).as(ContentTypes.JSON)
  }

  protected def futureSuccess(result: JsValue): Future[Result] = {
    Future.successful(success(result))
  }

  protected def failWithErrors(errors: Errors): Result = {
    BadRequest(errors.represent(JsonErrorRepresenter)).as(ContentTypes.JSON)
  }

  protected def failWithErrors(message: => String, errors: Errors): Result = {
    Log.error(message, errors)
    failWithErrors(errors)
  }

  protected def futureFailWithErrors(errors: Errors): Future[Result] = {
    Future.successful(failWithErrors(errors))
  }

  protected def futureFailWithErrors(message: => String, errors: Errors): Future[Result] = {
    Log.error(message, errors)
    futureFailWithErrors(errors)
  }
}
