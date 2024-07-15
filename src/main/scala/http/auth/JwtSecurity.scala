package http.auth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive0, Directive1}
import akka.http.scaladsl.server.Directives._
import db.model.User
import org.json4s.native.Serialization.read
import utils.Serializers

import java.time.Clock
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.util.UUID
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

trait JwtSecurity extends Serializers {

  private val expiresIn = 1 * 24 * 60 * 60
  implicit val clock: Clock = Clock.systemUTC

  private val secretKey = "lms"

  def encodeToken(user: User): String = {
    val claim =
      JwtClaim(serialization.write(user))
        .issuedNow
        .expiresIn(expiresIn)
    Jwt.encode(claim, secretKey, JwtAlgorithm.HS256)
  }

  def authenticatedWithRole(role: String): Directive1[User] = {
    authenticated flatMap {
      case user: User if user.roles.contains(role.trim) => provide(user)
      case _ => reject(AuthorizationFailedRejection).toDirective[Tuple1[User]]
    }
  }

  def authenticated: Directive1[User] = {
    optionalCookie("jwt_token").flatMap {
      case Some(jwtToken) if Jwt.isValid(jwtToken.value, secretKey, Seq(JwtAlgorithm.HS256)) =>
        getClaims(jwtToken.value) match {
          case Some(user) => provide(user)
          case None => reject(AuthorizationFailedRejection).toDirective[Tuple1[User]]
        }
      case t =>
        redirect("/login", StatusCodes.SeeOther)
    }
  }

  private def getClaims(jwtToken: String): Option[User] = {
    try {
      Jwt.decode(jwtToken, secretKey, Seq(JwtAlgorithm.HS256)) match {
        case Success(value) =>
          Some(read[User](value.content))
        case Failure(ex) =>
          ex.printStackTrace()
          None
      }
    } catch {
      case NonFatal(ex) => None
    }
  }
}
