package com.mehmetakiftutuncu.quupnotifications.notifications

import com.google.inject.AbstractModule

class NotificationCheckerSystemModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[NotificationCheckerSystem]).asEagerSingleton()
  }
}
