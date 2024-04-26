package http

import component.{ActorSystemComponent, Repositories}
import http.auth.JwtSecurity
import http.controller.UserController
import utils.Serializers

trait HttpRoutingController extends HttpBaseController
  with UserController {
  this: Repositories with ActorSystemComponent with Serializers with HttpRoute with JwtSecurity =>
}
