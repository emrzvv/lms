package http.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import component.{ActorSystemComponent, Repositories}
import db.model.User
import http.HttpBaseController
import http.auth.{Auth, JwtSecurity, JwtToken}
import org.mdedetrich.akka.http.WebJarsSupport.webJars
import play.twirl.api.Html
import utils.Serializers
import views.html.{home, login, register, head, header, footer}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

// GET /user/<uuid>
// POST /user
// DELETE /user/<uuid>
// PUT /user
// GET /user/all?limit=<limit>&offset=<offset>

trait UserController {
  this: Repositories with HttpBaseController with ActorSystemComponent with Serializers with JwtSecurity =>

  private def registerForm =
    formFields("username", "email", "role") { (username, email, role) =>
      val maybeUser = User(UUID.randomUUID(), username, email, role, LocalDate.now()) // TODO: validation
      onSuccess(userRepository.add(maybeUser)) { result =>
        setCookie(HttpCookie("jwt_token", value = encodeToken(maybeUser))) {
          redirect("/", StatusCodes.SeeOther)
        }
      }
    }

  private def loginForm =
    formFields("username") { username =>
      onSuccess(userRepository.getByUsername(username)) {
        case Some(user) =>
          setCookie(HttpCookie("jwt_token", value = encodeToken(user))) {
            redirect("/", StatusCodes.SeeOther)
          }
        case None =>
          redirect("/login", StatusCodes.SeeOther)
      }
    }

  registerRoute(pathPrefix("webjars") {
    webJars
  } ~ pathSingleSlash {
    get {
      authenticatedWithRole("user") { user =>
        complete(home(user)(head())(header())(footer()))
      }
    }
  } ~ path("register") {
      concat(
        get {
          complete(register(None, None, None))
        },
        post {
          registerForm
        }
      )
    } ~ path("login") {
      concat(
        get {
          complete(login(None))
        },
        post {
          loginForm
        }
      )
    } ~ pathPrefix("user") {
      authenticatedWithRole("user") { user =>
        complete(user)
      }
    }
  )
}
