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
import views.html.{home, login, register}

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

//  registerRoute(pathPrefix("auth") {
//    post {
//      entity(as[Auth]) { auth =>
//        val maybeUserF: Future[Option[User]] = userRepository.getByAuthData(auth)
//        handleResponse[Option[JwtToken]] {
//          maybeUserF.map {
//            case Some(user) =>
//              setCookie(HttpCookie("jwt_token", value = encodeToken(user))) {
//                redirect("/", StatusCodes.TemporaryRedirect)
//              }
////              Some(JwtToken(encodeToken(user)))
//            case None =>
//              redirect("/login", StatusCodes.TemporaryRedirect)
//          }
//        }
//      }
//    } ~ path("register") {
//      concat(
//        get {
//          println("GET REGISTER FORM")
//          complete(register())
//        },
//        post {
//          println("POST REGISTER FORM")
//          registerForm
//        }
//      )
//    }
//  })

  registerRoute(pathPrefix("webjars") {
    webJars
  } ~ pathSingleSlash {
    get {
      authenticatedWithRole("admin") { admin =>
        complete(s"hello ${admin}")
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
    } ~ path("login") {
      concat(
        get {
          complete(login(None))
        },
        post {
          loginForm
        }
      )
    }
  )

  registerRoute(pathSingleSlash {
    get {
      complete(home())
    }
  })
}
