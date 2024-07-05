import component.Services
import component.impl.{ActorSystemComponentImpl, ConfigComponentImpl, DatabaseComponentImpl, RepositoriesImpl, RoutingComponentImpl, ServicesImpl}
import http.auth.JwtSecurity
import http.{HttpRoute, HttpRoutingController}
import utils.{Logging, Serializers}

object Boot extends App
  with Logging
  with ConfigComponentImpl
  with ActorSystemComponentImpl
  with DatabaseComponentImpl
  with RepositoriesImpl
  with ServicesImpl
  with Serializers
  with HttpRoute
  with HttpRoutingController
  with RoutingComponentImpl
  with JwtSecurity