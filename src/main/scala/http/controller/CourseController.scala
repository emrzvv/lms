package http.controller

import akka.http.scaladsl.model.{HttpResponse, StatusCodes, headers}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import component.{ActorSystemComponent, Repositories, Services}
import db.model.{Course, User}
import http.HttpBaseController
import http.auth.JwtSecurity
import http.model.UpdateCourseRequest
import utils.Serializers
import views.html.components.{footer, head, header}
import views.html.course.{course_preview, courses_all, newcourse, course_users}

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
              complete(newcourse(user))
            }
          },
          post {
            authenticatedWithRole("tutor") { user =>
              newCourseForm(user)
            }
          }
        )
      } ~
        path("all") {
          get {
            parameters("limit".as[Int].optional, "offset".as[Int].optional) { (limit, offset) =>
              authenticatedWithRole("user") { user =>
                onSuccess(courseService.all(limit.getOrElse(9), offset.getOrElse(0))) { (courses, totalCourses) =>
                  complete(courses_all(user, courses, limit.getOrElse(9), offset.getOrElse(0), totalCourses))
                }
              }
            }
          }
        }~
        path(JavaUUID) { id =>
          get {
            authenticatedWithRole("user") { user =>
              onSuccess(courseService.getById(id)) {
                case Some(course) => complete(course_preview(user, course))
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
                        body.estimatedTime,
                        body.isFree)) { _ =>
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
        } ~
        path(JavaUUID / "users") { courseId =>
          get {
            authenticatedWithRole("tutor") { tutor =>
              onSuccess(courseService.isAbleToEdit(tutor.id, courseId)) {
                case true =>
                  onSuccess(courseService.getById(courseId)) {
                    case Some(course) =>
                      onSuccess(courseService.getUsersOnCourseWithRights(courseId)) { users =>
                        complete(course_users(tutor, course, users))
                      }
                    case None => complete(StatusCodes.NotFound)
                  }
                case false => complete(StatusCodes.Forbidden)
              }
            }
          } ~ parameters("id".as[UUID]) { userId =>
            put {
              authenticatedWithRole("tutor") { tutor =>
                onSuccess(courseService.isAbleToEdit(tutor.id, courseId)) {
                  case true =>
                    onSuccess(courseService.getById(courseId)) {
                      case Some(_) =>
                        onSuccess(courseService.addUserToCourse(userId, courseId)) { _ =>
                          complete(StatusCodes.OK)
                        }
                      case None => complete(StatusCodes.NotFound)
                    }
                  case false => complete(StatusCodes.Forbidden)
                }
              }
            }
          }
        }
    }
  )
}
