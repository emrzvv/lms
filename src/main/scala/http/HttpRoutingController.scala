package http

import component.{ActorSystemComponent, Repositories, Services}
import http.auth.JwtSecurity
import http.controller.{CourseController, UserController}
import service.UserService
import utils.Serializers

trait HttpRoutingController extends HttpBaseController
  with UserController
  with CourseController {
  this: Services
    with ActorSystemComponent
    with Serializers
    with HttpRoute
    with JwtSecurity =>
}
