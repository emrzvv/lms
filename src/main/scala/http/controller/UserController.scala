package http.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import component.{ActorSystemComponent, Repositories, Services}
import db.model.User
import http.HttpBaseController
import http.auth.{Auth, JwtSecurity, JwtToken}
import org.mdedetrich.akka.http.WebJarsSupport.webJars
import play.twirl.api.{Html, JavaScript}
import service.{UserService, UserServiceImpl}
import utils.Serializers
import views.html.{footer, head, header, home, login, profile, register}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future


trait UserController {
  this:  Services
    with HttpBaseController
    with ActorSystemComponent
    with Serializers
    with JwtSecurity =>

  private def registerForm =
    formFields("username", "email", "password") { (username, email, password) =>
      onSuccess(userService.createUser(username, email, password)) { result =>
        setCookie(HttpCookie("jwt_token", value = encodeToken(result))) {
          redirect("/", StatusCodes.SeeOther)
        }
      }
    }

  private def loginForm =
    formFields("username", "password") { (username, password) =>
      onSuccess(userService.validateUser(username, password)) {
        case Some(user) =>
          setCookie(HttpCookie("jwt_token", value = encodeToken(user))) {
            redirect("/", StatusCodes.SeeOther)
          }
        case None =>
          complete(login(Some(username), Some("Неверный логин или пароль")))
      }
    }

  private def profileForm(user: User, viewingUser: User) =
    formFields("username", "email") { (username, email) =>
      val updatedViewingUser = viewingUser.copy(username = username, email = email) // TODO: checkbox?
      val updatedUser = if (user.id == viewingUser.id) updatedViewingUser else user
      onSuccess(userService.updateUser(updatedUser)) { _ =>
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
          complete(login(None, None))
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
          onSuccess(userService.getUserById(id)) {
            case Some(viewingUser) => complete(profile(currentUser, viewingUser)(head())(header(currentUser))(footer()))
            case None => complete(StatusCodes.NotFound)
          }
        }
      } ~ post {
        authenticatedWithRole("user") { currentUser =>
          onSuccess(userService.getUserById(id)) {
            case Some(viewingUser) =>
              if (viewingUser.id == currentUser.id || currentUser.roles.contains("admin")) {
                println(s"UPDATING ${}")
                profileForm(currentUser, viewingUser)
              } else {
                complete(StatusCodes.Forbidden)
              }
            case None => complete(StatusCodes.NotFound)
          }

        }
      }
    } ~ path("user" / JavaUUID / "roles") { id =>
      post {
        parameters("action", "role") { (action, role) =>
          authenticatedWithRole("admin") { admin =>
            onSuccess(userService.getUserById(id)) {
              case Some(updatingUser) =>
                if (action == "add") {
                  val updatedRoles = (role :: updatingUser.roles).distinct
                  val updatedUser = updatingUser.copy(roles = updatedRoles)
                  onSuccess(userService.updateUser(updatedUser)) { _ =>
                    complete(profile(admin, updatedUser)(head())(header(updatedUser))(footer()))
                  }
                } else if (action == "remove") {
                  val updatedRoles = updatingUser.roles.filterNot(_ == role)
                  val updatedUser = updatingUser.copy(roles = updatedRoles)
                  onSuccess(userService.updateUser(updatedUser)) { _ =>
                    complete(profile(admin, updatedUser)(head())(header(updatedUser))(footer()))
                  }
                } else {
                  complete(StatusCodes.BadRequest)
                }
              case None => complete(StatusCodes.NotFound)
            }
          }
        }
      }
    }
  )
}
