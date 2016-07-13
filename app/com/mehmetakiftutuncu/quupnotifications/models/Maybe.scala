package com.mehmetakiftutuncu.quupnotifications.models

import com.github.mehmetakiftutuncu.errors.Errors

object Maybe {
  type Maybe[V] = Either[Errors, V]

  def apply[V](errors: Errors): Maybe[V] = Left.apply[Errors, V](errors)
  def apply[V](value: V): Maybe[V]       = Right.apply[Errors, V](value)

  implicit class MaybeExtensions[V](maybe: Maybe[V]) {
    def maybeErrors: Option[Errors] = maybe.left.toOption
    def maybeValue: Option[V]       = maybe.right.toOption

    def errors: Errors = maybeErrors.getOrElse(throw new NoSuchElementException(s"""Maybe "${maybe.right.get}" wasn't an Errors but a value. Make sure you check it with "hasErrors" method first or use "maybeErrors" method to access the Errors!"""))
    def value: V       = maybeValue.getOrElse(throw new NoSuchElementException(s"""Maybe "${maybe.left.get}" wasn't a value but an Errors. Make sure you check it with "hasValue" method first or use "maybeValue" method to access the value!"""))

    def hasErrors: Boolean  = maybeErrors.isDefined
    def hasValue: Boolean   = maybeValue.isDefined
  }
}
