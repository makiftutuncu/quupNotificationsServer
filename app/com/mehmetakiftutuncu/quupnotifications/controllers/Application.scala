package com.mehmetakiftutuncu.quupnotifications.controllers

import play.api.mvc.{Action, AnyContent, Controller}

/**
  * Created by akif on 12/07/16.
  */
class Application extends Controller {
  def index: Action[AnyContent] = {
    Action {
      Ok("quup Notifications Server is up and running!")
    }
  }
}
