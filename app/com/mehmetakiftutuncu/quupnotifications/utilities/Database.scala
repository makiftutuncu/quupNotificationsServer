package com.mehmetakiftutuncu.quupnotifications.utilities

import java.sql.Connection

import play.api.db.DBApi

case class Database(private val dbApi: DBApi) {
  def withConnection[R](action: Connection => R): R = {
    dbApi.database("default").withConnection(action)
  }
}
