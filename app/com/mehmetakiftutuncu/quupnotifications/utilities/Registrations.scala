package com.mehmetakiftutuncu.quupnotifications.utilities

import java.util

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.firebase.RealtimeDatabaseBase
import com.mehmetakiftutuncu.quupnotifications.models.Registration

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
case class Registrations @Inject()(RealtimeDatabase: RealtimeDatabaseBase) extends RegistrationsBase

@ImplementedBy(classOf[Registrations])
trait RegistrationsBase extends Loggable {
  protected val RealtimeDatabase: RealtimeDatabaseBase

  def get(registrationId: String): Future[Maybe[Registration]] = {
    try {
      Log.debug(s"""Getting Registration for registration id "$registrationId"...""")

      val futureMaybeSessionId: Future[Maybe[String]]      = RealtimeDatabase.get[String](s"registrations/$registrationId/sessionId")
      val futureMaybeLastNotification: Future[Maybe[Long]] = RealtimeDatabase.get[Long](s"registrations/$registrationId/lastNotification")

      val futureMaybeRegistration: Future[Maybe[Registration]] = {
        for {
          maybeSessionId: Maybe[String]      <- futureMaybeSessionId
          maybeLastNotification: Maybe[Long] <- futureMaybeLastNotification
        } yield {
          val errors: Errors = maybeSessionId.maybeErrors.getOrElse(Errors.empty) ++ maybeLastNotification.maybeErrors.getOrElse(Errors.empty)

          if (errors.hasErrors) {
            Maybe[Registration](errors)
          } else {
            val registration: Registration = Registration(registrationId, maybeSessionId.value, maybeLastNotification.value)

            Maybe[Registration](registration)
          }
        }
      }

      futureMaybeRegistration.recover {
        case t: Throwable =>
          val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

          Log.error(s"""Failed to get Registration for registration id "$registrationId", future failed!""", errors, t)

          Maybe[Registration](errors)
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

        Log.error(s"""Failed to get Registration for registration id "$registrationId" with exception!""", errors, t)

        Future.successful(Maybe[Registration](errors))
    }
  }

  def getAll: Future[Maybe[List[Registration]]] = {
    try {
      Log.debug(s"""Getting registrations...""")

      val futureMaybeRegistrationIds: Future[Maybe[List[String]]] = RealtimeDatabase.getAll[String]("registrations")

      futureMaybeRegistrationIds.flatMap {
        maybeRegistrationIds: Maybe[List[String]] =>
          if (maybeRegistrationIds.hasErrors) {
            Future.successful(Maybe[List[Registration]](maybeRegistrationIds.errors))
          } else {
            val futureMaybeRegistrationList: List[Future[Maybe[Registration]]] = maybeRegistrationIds.value.map(get)

            Future.sequence(futureMaybeRegistrationList).map {
              _.foldLeft(Maybe[List[Registration]](Errors.empty)) {
                case (currentResult: Maybe[List[Registration]], maybeRegistration: Maybe[Registration]) =>
                  if (currentResult.maybeErrors.getOrElse(Errors.empty).hasErrors) {
                    currentResult
                  } else if (maybeRegistration.hasErrors) {
                    Maybe[List[Registration]](maybeRegistration.errors)
                  } else {
                    Maybe[List[Registration]](currentResult.maybeValue.getOrElse(List.empty[Registration]) :+ maybeRegistration.value)
                  }
              }
            }.recover {
              case t: Throwable =>
                val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

                Log.error("""Failed to get registrations, collecting future failed!""", errors, t)

                Maybe[List[Registration]](errors)
            }
          }
      }.recover {
        case t: Throwable =>
          val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

          Log.error("""Failed to get registrations, future failed!""", errors, t)

          Maybe[List[Registration]](errors)
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

        Log.error("""Failed to get registrations with exception!""", errors, t)

        Future.successful(Maybe[List[Registration]](errors))
    }
  }

  def save(registration: Registration): Future[Errors] = {
    try {
      Log.debug(s"""Saving Registration for registration id "${registration.registrationId}"...""")

      val registrationMap: util.Map[String, Any] = new util.HashMap[String, Any]()
      registrationMap.put("sessionId", registration.sessionId)
      registrationMap.put("lastNotification", registration.lastNotification)

      RealtimeDatabase.set[util.Map[String, Any]](s"registrations/${registration.registrationId}", registrationMap).recover {
        case t: Throwable =>
          val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

          Log.error("Failed to save Registration, future failed!", errors, t)

          errors
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

        Log.error("Failed to save Registration with exception!", errors, t)

        Future.successful(errors)
    }
  }

  def delete(registration: Registration): Future[Errors] = {
    try {
      Log.debug(s"""Deleting Registration for registration id "${registration.registrationId}"...""")

      RealtimeDatabase.delete(s"registrations/${registration.registrationId}").recover {
        case t: Throwable =>
          val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

          Log.error("Failed to delete Registration, future failed!", errors, t)

          errors
      }
    } catch {
      case t: Throwable =>
        val errors: Errors = Errors(CommonError.database.reason(t.getMessage))

        Log.error("Failed to delete Registration with exception!", errors, t)

        Future.successful(errors)
    }
  }
}
