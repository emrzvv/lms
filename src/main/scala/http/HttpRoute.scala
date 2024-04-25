package http

import akka.http.scaladsl.server.Route

trait HttpRoute {
  def route: Route
}
