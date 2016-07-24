package com.mehmetakiftutuncu.quupnotifications.application

import play.api.mvc.{Action, AnyContent, Controller}

class Application extends Controller {
  def index: Action[AnyContent] = {
    Action {
      Ok("quup Notifications Server is up and running!")
    }
  }

  def favicon: Action[AnyContent] = Action(Ok)

  def robots: Action[AnyContent] = Action(Ok)
}
