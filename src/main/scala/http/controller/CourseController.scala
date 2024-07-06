package http.controller

import akka.http.scaladsl.model.{HttpResponse, StatusCodes, headers}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import component.{ActorSystemComponent, Repositories, Services}
import db.model.{Course, User}
import http.HttpBaseController
import http.auth.JwtSecurity
import http.html.PageComponents
import http.model.UpdateCourseRequest
import utils.Serializers
import views.html.{course_preview, footer, head, header, newcourse}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CourseController {
  this: Services // TODO: Services
    with HttpBaseController
    with ActorSystemComponent
    with Serializers
    with JwtSecurity =>

  implicit val intUnmarshaller: Unmarshaller[String, Int] = Unmarshaller.strict(_.toInt)

  private def newCourseForm(user: User) =
    formFields("name", "short_description", "description", "preview_image_url".optional, "estimated_time".as[Int]) {
      (name, shortDescription, description, previewImageUrl, estimatedTime) =>

        // TODO: validation
        onSuccess(courseService.createCourse(user)(name, shortDescription, description, previewImageUrl, estimatedTime)) { id =>
          redirect(s"/course/${id}", StatusCodes.SeeOther)
        }
    }

  registerRoute(
    pathPrefix("course") {
      path("new") {
        concat(
          get {
            authenticatedWithRole("tutor") { user =>
              complete(newcourse(user)(PageComponents(head(), header(user), footer())))
            }
          },
          post {
            authenticatedWithRole("tutor") { user =>
              newCourseForm(user)
            }
          }
        )
      } ~
        path(JavaUUID) { id =>
          get {
            authenticatedWithRole("user") { user =>
              onSuccess(courseService.getById(id)) {
                case Some(course) => complete(course_preview(user, course)(PageComponents(head(), header(user), footer())))
                case None => complete(StatusCodes.NotFound)
              }
            }
          } ~
            put {
              authenticatedWithRole("tutor") { tutor =>
                entity(as[UpdateCourseRequest]) { body =>
                  onSuccess(
                    courseService
                      .updateCourse(id,
                        body.name,
                        body.shortDescription,
                        body.description,
                        body.previewImageUrl,
                        body.estimatedTime)) { _ =>
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
        }
    }
  )
}
