package com.mehmetakiftutuncu.quupnotifications.utilities

import java.sql.Connection

import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.db.DBApi

@Singleton
case class Database @Inject() (dbApi: DBApi) extends DatabaseBase

@ImplementedBy(classOf[Database])
trait DatabaseBase {
  protected val dbApi: DBApi

  def withConnection[R](action: Connection => R): R = {
    dbApi.database("default").withConnection(action)
  }
}
