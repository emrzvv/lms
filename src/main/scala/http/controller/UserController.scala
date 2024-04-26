package http.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import component.{ActorSystemComponent, Repositories}
import db.model.User
import http.HttpBaseController
import http.auth.{Auth, JwtSecurity, JwtToken}
import org.mdedetrich.akka.http.WebJarsSupport.webJars
import play.twirl.api.{Html, JavaScript}
import utils.Serializers
import views.html.{footer, head, header, home, login, profile, register}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

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

  private def profileForm(user: User, viewingUser: User) =
    formFields("username", "email", "role") { (username, email, role) =>
      val updatedViewingUser = viewingUser.copy(username = username, email = email, role = role)
      val updatedUser = if (user.id == viewingUser.id) updatedViewingUser else user
      onSuccess(userRepository.update(updatedUser)) { _ =>
        complete(profile(updatedUser, updatedViewingUser)(head())(header(updatedUser))(footer()))
      }
    }

  registerRoute(pathPrefix("webjars") {
    webJars
  })

  registerRoute(
    pathSingleSlash {
      get {
        authenticatedWithRole("user") { user =>
          complete(home(user)(head())(header(user))(footer()))
        }
      }
    } ~
    path("register") {
      concat(
        get {
          complete(register(None, None, None))
        },
        post {
          registerForm
        }
      )
    } ~
    path("login") {
      concat(
        get {
          complete(login(None))
        },
        post {
          loginForm
        }
      )
    } ~
    path("logout") {
      get {
        authenticatedWithRole("user") { user =>
          deleteCookie("jwt_token") {
            redirect("/login", StatusCodes.SeeOther)
          }
        }
      }
    } ~
    path("user" / JavaUUID) { id =>
      get {
        authenticatedWithRole("user") { currentUser =>
          onSuccess(userRepository.getById(id)) {
            case Some(viewingUser) => complete(profile(currentUser, viewingUser)(head())(header(currentUser))(footer()))
            case None => complete(StatusCodes.NotFound)
          }
        }
      } ~ post {
        authenticatedWithRole("user") { currentUser =>
          onSuccess(userRepository.getById(id)) {
            case Some(viewingUser) =>
              if (viewingUser.id == currentUser.id || currentUser.role == "admin") { // TODO" user.roles.contains("admin")
                println(s"UPDATING ${}")
                profileForm(currentUser, viewingUser)
              } else {
                complete(StatusCodes.Forbidden)
              }
            case None => complete(StatusCodes.NotFound)
          }

        }
      }
    }
  )
}
