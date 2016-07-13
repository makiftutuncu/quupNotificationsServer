package com.mehmetakiftutuncu.quupnotifications.utilities

import com.github.mehmetakiftutuncu.errors.Errors
import play.api.Logger

object Log {
  def debug(message: => String)(implicit loggable: Loggable): Unit                                       = Logger.debug(messageWithTag(message))
  def debug(message: => String, throwable: Throwable)(implicit loggable: Loggable): Unit                 = Logger.debug(messageWithTag(message), throwable)
  def debug(message: => String, errors: Errors)(implicit loggable: Loggable): Unit                       = Logger.debug(messageWithErrors(message, errors))
  def debug(message: => String, errors: Errors, throwable: Throwable)(implicit loggable: Loggable): Unit = Logger.debug(messageWithErrors(message, errors), throwable)

  def warn(message: => String)(implicit loggable: Loggable): Unit                                       = Logger.warn(messageWithTag(message))
  def warn(message: => String, throwable: Throwable)(implicit loggable: Loggable): Unit                 = Logger.warn(messageWithTag(message), throwable)
  def warn(message: => String, errors: Errors)(implicit loggable: Loggable): Unit                       = Logger.warn(messageWithErrors(message, errors))
  def warn(message: => String, errors: Errors, throwable: Throwable)(implicit loggable: Loggable): Unit = Logger.warn(messageWithErrors(message, errors), throwable)

  def error(message: => String)(implicit loggable: Loggable): Unit                                       = Logger.error(messageWithTag(message))
  def error(message: => String, throwable: Throwable)(implicit loggable: Loggable): Unit                 = Logger.error(messageWithTag(message), throwable)
  def error(message: => String, errors: Errors)(implicit loggable: Loggable): Unit                       = Logger.error(messageWithErrors(message, errors))
  def error(message: => String, errors: Errors, throwable: Throwable)(implicit loggable: Loggable): Unit = Logger.error(messageWithErrors(message, errors), throwable)

  private def messageWithTag(message: => String)(implicit loggable: Loggable): String = {
    s"[${loggable.TAG}] $message"
  }

  private def messageWithErrors(message: => String, errors: Errors)(implicit loggable: Loggable): String = {
    s"${messageWithTag(message)} Errors: ${errors.represent(JsonErrorRepresenter)}"
  }
}

trait Loggable {
  self: Any =>
    implicit lazy val loggable: Loggable = self

    lazy val TAG: String = self.getClass.getSimpleName
}
