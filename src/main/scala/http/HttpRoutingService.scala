package http

import component.{ActorSystemComponent, Repositories}
import http.service.UserService
import utils.Serializers

trait HttpRoutingService extends HttpBaseService
  with UserService {
  this: Repositories with ActorSystemComponent with Serializers with HttpRoute =>
}
