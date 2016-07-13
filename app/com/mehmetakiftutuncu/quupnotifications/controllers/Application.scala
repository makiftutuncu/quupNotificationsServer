package com.mehmetakiftutuncu.quupnotifications.controllers

import play.api.mvc.{Action, AnyContent, Controller}

class Application extends Controller {
  def index: Action[AnyContent] = {
    Action {
      Ok("quup Notifications Server is up and running!")
    }
  }
}
