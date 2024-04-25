package http.service

import akka.http.scaladsl.server.Directives._
import component.{ActorSystemComponent, Repositories}
import db.model.User
import http.HttpBaseService
import http.auth.{Auth, JwtSecurity, JwtToken}
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
  this: Repositories with HttpBaseService with ActorSystemComponent with Serializers with JwtSecurity =>

  registerRoute(pathPrefix("auth") {
    post {
      entity(as[Auth]) { auth =>
        val maybeUserF = userRepository.getByAuthData(auth)
        rejectEmptyResponse {
          handleResponse {
            maybeUserF.map {
              case Some(user) => Some(JwtToken(encodeToken(user)))
              case None => None
            }
          }
        }
      }
    }
  })

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
