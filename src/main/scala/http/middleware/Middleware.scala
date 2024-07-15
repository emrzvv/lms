package http.middleware

import akka.http.scaladsl.server.{Directive0, Directive1}
import db.model.{Course, User}
import utils.Serializers

trait Middleware extends Serializers {
}
