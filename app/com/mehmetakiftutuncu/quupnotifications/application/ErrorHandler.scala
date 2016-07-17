package com.mehmetakiftutuncu.quupnotifications.application

import javax.inject.{Inject, Provider, Singleton}

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors}
import com.mehmetakiftutuncu.quupnotifications.utilities.{ControllerBase, Log, Loggable}
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result}
import play.api.routing.Router
import play.api.{Configuration, Environment, OptionalSourceMapper, UsefulException}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (environment: Environment,
                              config: Configuration,
                              sourceMapper: OptionalSourceMapper,
                              router: Provider[Router]) extends DefaultHttpErrorHandler(environment, config, sourceMapper, router) with ControllerBase with Loggable {

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] = {
    val errors: Errors = Errors(CommonError.requestFailed)

    Log.error(s"""Request "$request" failed with exception!""", errors, exception)

    futureFailWithErrors(errors)
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    futureFailWithErrors(s"""Request "$request" failed with status code "$statusCode"!""", Errors(CommonError.requestFailed.reason(message)))
  }

  override protected def onOtherClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = onClientError(request, statusCode, message)
}
