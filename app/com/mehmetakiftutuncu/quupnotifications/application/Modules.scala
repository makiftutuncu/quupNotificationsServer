package com.mehmetakiftutuncu.quupnotifications.application

import com.google.inject.AbstractModule
import com.mehmetakiftutuncu.quupnotifications.heartbeat.Heartbeat
import com.mehmetakiftutuncu.quupnotifications.notifications.NotificationCheckerSystem

class Modules extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Heartbeat]).asEagerSingleton()
    bind(classOf[NotificationCheckerSystem]).asEagerSingleton()
  }
}
