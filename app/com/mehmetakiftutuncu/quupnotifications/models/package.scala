package com.mehmetakiftutuncu.quupnotifications

import com.github.mehmetakiftutuncu.errors.Errors

/**
  * Created by akif on 30/06/16.
  */
package object models {
  type MaybeValue[V] = Either[Errors, V]

  implicit def errorsToMaybeValue[V](errors: Errors): MaybeValue[V] = Left.apply[Errors, V](errors)
  implicit def valueToMaybeValue[V](value: V): MaybeValue[V]        = Right.apply[Errors, V](value)

  implicit class MaybeValueExtensions[V](maybe: MaybeValue[V]) {
    def maybeErrors: Option[Errors] = maybe.left.toOption
    def maybeValue: Option[V]       = maybe.right.toOption

    def errors: Errors = maybeErrors.getOrElse(throw new NoSuchElementException(s"""MaybeValue "${maybe.right.get}" did not have errors in it. Make sure you check it with "hasErrors" method first or use "maybeErrors" method!"""))
    def value: V       = maybeValue.getOrElse(throw new NoSuchElementException(s"""MaybeValue "${maybe.left.get}" did not have value in it. Make sure you check it with "hasValue" method first or use "maybeValue" method!"""))

    def hasErrors: Boolean  = maybeErrors.isDefined
    def hasValue: Boolean   = maybeValue.isDefined
  }
}
