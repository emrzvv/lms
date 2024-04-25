package http.service

import akka.http.scaladsl.server.Directives._
import component.{ActorSystemComponent, Repositories}
import db.model.User
import http.HttpBaseService
import org.mdedetrich.akka.http.WebJarsSupport.webJars
import play.twirl.api.Html
import utils.Serializers
import views.html.home

// GET /user/<uuid>
// POST /user
// DELETE /user/<uuid>
// PUT /user
// GET /user/all?limit=<limit>&offset=<offset>

trait UserService {
  this: Repositories with HttpBaseService with ActorSystemComponent with Serializers =>

  registerRoute(pathPrefix("webjars") {
    webJars
  })

  registerRoute(pathPrefix("user") {
    concat(
      path(JavaUUID) { uuid =>
        get {
          handleResponse[User](userRepository.getById(uuid).map(_.getOrElse("not found")))
        }
      }
    )
  })

  registerRoute(pathSingleSlash {
    get {
      complete(home())
    }
  })
}
