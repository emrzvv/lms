package http.controller

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import component.{ActorSystemComponent, Repositories}
import db.model.{Course, User}
import http.HttpBaseController
import http.auth.JwtSecurity
import utils.Serializers
import views.html.{footer, head, header}

import java.time.LocalDateTime
import java.util.UUID

trait CourseController {
  this: Repositories with HttpBaseController with ActorSystemComponent with Serializers with JwtSecurity =>

  implicit val intUnmarshaller: Unmarshaller[String, Int] = Unmarshaller.strict(_.toInt)

  private def newCourseForm(user: User) =
    formFields("name", "short_description", "description", "preview_image_url".optional, "estimated_time".as[Int]) {
      (name, shortDescription, description, previewImageUrl, estimatedTime) =>
        val dateTime = LocalDateTime.now()
        val id = UUID.randomUUID()
        val maybeCourse = Course(
          id = id,
          name = name,
          creatorId = user.id,
          shortDescription = shortDescription,
          description = description,
          previewImageUrl = previewImageUrl,
          estimatedTime = estimatedTime,
          createdAt = dateTime,
          lastModifiedAt = dateTime,
          isPublished = false
        )
        // TODO: validation
        onSuccess(courseRepository.add(maybeCourse)) { result =>
          redirect(s"/course/${id}", StatusCodes.SeeOther)
        }
    }

  registerRoute(
    pathPrefix("course") {
      path("new") {
        concat(
          get {
            authenticatedWithRole("user") { user =>
              complete(newcourse(user)(head())(header(user))(footer()))
            }
          },
          post {
            authenticatedWithRole("user") { user =>
              newCourseForm(user)
            }
          }
        )
      } ~
        path(JavaUUID) { id =>
          get {
            authenticatedWithRole("user") { user =>
              onSuccess(courseRepository.getById(id)) {
                case Some(course) => complete(course_preview(user, course)(head())(header(user))(footer()))
                case None => complete(StatusCodes.NotFound)
              }
            }
          }
        }
    }
  )
}
