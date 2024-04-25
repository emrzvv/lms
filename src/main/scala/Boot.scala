import component.impl.{ActorSystemComponentImpl, ConfigComponentImpl, DatabaseComponentImpl, RepositoriesImpl, RoutingComponentImpl}
import http.{HttpRoute, HttpRoutingService}
import utils.{Logging, Serializers}

object Boot extends App
  with Logging
  with ConfigComponentImpl
  with ActorSystemComponentImpl
  with DatabaseComponentImpl
  with RepositoriesImpl
  with Serializers
  with HttpRoute
  with HttpRoutingService
  with RoutingComponentImpl