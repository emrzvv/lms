package http

import component.{ActorSystemComponent, Repositories}
import http.auth.JwtSecurity
import http.controller.{CourseController, UserController}
import utils.Serializers

trait HttpRoutingController extends HttpBaseController
  with UserController
  with CourseController {
  this: Repositories with ActorSystemComponent with Serializers with HttpRoute with JwtSecurity =>
}
