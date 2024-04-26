package http

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import http.auth.JwtSecurity
import play.twirl.api.Html

import scala.collection.mutable
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

trait HttpBaseController {
  this: HttpRoute =>

  private val routes = mutable.ArrayBuffer[Route]()

  override lazy val route: Route = {
    if (routes.nonEmpty) {
      routes.reduceLeft((next, prev) => next ~ prev)
    } else {
      pathPrefix(RemainingPath) { _ =>
        complete(StatusCodes.NotImplemented)
      }
    }
  }

  implicit val twirlMarshaller: ToEntityMarshaller[Html] =
    Marshaller.withFixedContentType(ContentTypes.`text/html(UTF-8)`) { html =>
      HttpEntity(ContentTypes.`text/html(UTF-8)`, html.body)
    }

  protected def registerRoute(route: Route): Unit = routes += route

  protected def defaultStatusCode[T]: T => StatusCode = _ => StatusCodes.OK

  protected def handleResponse[T: ClassTag](future: Future[_], statusCode: T => StatusCode = defaultStatusCode)(implicit
                                                                                                                m: ToEntityMarshaller[T]
  ): Route =
    onComplete(future.mapTo[T]) {
      case Success(value) => complete(statusCode(value), value)
      case Failure(exception) => complete(StatusCodes.InternalServerError, exception.getMessage)
    }
}