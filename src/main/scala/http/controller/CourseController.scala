package http.controller

import akka.http.scaladsl.model.{HttpResponse, StatusCodes, headers}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshaller
import component.{ActorSystemComponent, Repositories, Services}
import db.model.{Course, User}
import http.HttpBaseController
import http.auth.JwtSecurity
import http.model._
import utils.Serializers
import views.html.components.{footer, head, header}
import views.html.course.{course_preview, course_users, courses_all, newcourse, course_content, course_lesson_edit}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization.write

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
                onSuccess(courseService.allFreeAndPublished(limit.getOrElse(9), offset.getOrElse(0))) { (courses, totalCourses) =>
                  complete(courses_all(user, courses, limit.getOrElse(9), offset.getOrElse(0), totalCourses))
                }
              }
            }
          }
        } ~
        path(JavaUUID) { id =>
          get {
            authenticatedWithRole("user") { user =>
              onSuccess {
                for {
                  course <- courseService.getById(id) if course.nonEmpty
                  isUserOnCourse <- courseService.checkIfUserOnCourse(user.id, id)
                  modulesWithLessons <- courseService.getModulesWithLessons(id)
                } yield (course.get, isUserOnCourse, modulesWithLessons.sortBy(_.order).map(mls => mls.copy(lessons = mls.lessons.sortBy(_.order))))
              } {
                case (course, isUserOnCourse, modulesWithLessons) => complete(course_preview(user, course, isUserOnCourse, modulesWithLessons))
                case _ => complete(StatusCodes.BadRequest)
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
              onSuccess {
                for {
                  ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                  courseOpt  <- courseService.getById(courseId)
                  result     <- courseOpt match {
                    case Some(course) =>
                      courseService.getUsersOnCourseWithRights(courseId).map { users =>
                        complete(course_users(tutor, course, users))
                      }
                    case None => Future.successful(complete(StatusCodes.NotFound))
                  }
                } yield result
              } {
                case result: Route => result
                case _             => complete(StatusCodes.Forbidden)
              }
            }
          } ~ parameters("id".as[UUID]) { userId =>
            put {
              authenticatedWithRole("tutor") { tutor =>
                onSuccess {
                  for {
                    ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                    courseOpt  <- courseService.getById(courseId)
                    result     <- courseOpt match {
                      case Some(_) => courseService.addUserToCourse(userId, courseId).map(_ => StatusCodes.OK)
                      case None    => Future.successful(StatusCodes.NotFound)
                    }
                  } yield result
                } {
                  case StatusCodes.OK        => complete(StatusCodes.OK)
                  case StatusCodes.NotFound  => complete(StatusCodes.NotFound)
                  case StatusCodes.Forbidden => complete(StatusCodes.Forbidden)
                }
              }
            } ~
              delete {
                authenticatedWithRole("tutor") { tutor =>
                  onSuccess {
                    for {
                      ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                      courseOpt  <- courseService.getById(courseId)
                      result     <- courseOpt match {
                        case Some(_) => courseService.removeUserFromCourse(userId, courseId).map(_ => StatusCodes.OK)
                        case None    => Future.successful(StatusCodes.NotFound)
                      }
                    } yield result
                  } {
                    case StatusCodes.OK         => complete(StatusCodes.OK)
                    case StatusCodes.NotFound   => complete(StatusCodes.NotFound)
                    case StatusCodes.Forbidden  => complete(StatusCodes.Forbidden)
                  }
                }
              }
          }
        } ~
        path(JavaUUID / "users" / "grant_access") { courseId =>
          parameters("id".as[UUID]) { userId =>
            put {
              authenticatedWithRole("tutor") { tutor =>
                onSuccess {
                  for {
                    ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                    user <- userService.getById(userId) if user.exists(_.roles.contains("tutor"))
                    courseOpt <- courseService.getById(courseId)
                    result <- courseOpt match {
                      case Some(_) => courseService.grantCourseAccessToUser(userId, courseId, ableToEdit = true).map(_ => StatusCodes.OK)
                      case None => Future.successful(StatusCodes.NotFound)
                    }
                  } yield result
                } {
                  case StatusCodes.OK        => complete(StatusCodes.OK)
                  case StatusCodes.NotFound  => complete(StatusCodes.NotFound)
                  case StatusCodes.Forbidden => complete(StatusCodes.Forbidden)
                }
              }
            }
          }
        } ~
        path(JavaUUID / "users" / JavaUUID / "enroll") { (courseId, userId) =>
          post {
            authenticatedWithRole("user") { user =>
              onSuccess {
                for {
                  courseOpt <- courseService.getById(courseId)
                  result <- courseOpt match {
                    case Some(course) if course.isFree && course.isPublished =>
                      courseService.addUserToCourse(userId, courseId).map(_ => StatusCodes.OK)
                    case None => Future.successful(StatusCodes.NotFound)
                  }
                } yield result
              } {
                r => complete(r)
              }
            }
          }
        } ~
        path(JavaUUID / "users" / JavaUUID / "quit") { (courseId, userId) =>
          delete {
            authenticatedWithRole("user") { user =>
              onSuccess {
                for {
                  courseOpt  <- courseService.getById(courseId)
                  result     <- courseOpt match {
                    case Some(_) => courseService.removeUserFromCourse(userId, courseId).map(_ => StatusCodes.OK)
                    case None    => Future.successful(StatusCodes.NotFound)
                  }
                } yield result
              } {
                case StatusCodes.OK         => complete(StatusCodes.OK)
                case StatusCodes.NotFound   => complete(StatusCodes.NotFound)
                case StatusCodes.Forbidden  => complete(StatusCodes.Forbidden)
              }
            }
          }

        } ~
        path(JavaUUID / "publish") { courseId =>
          put {
            authenticatedWithRole("admin") { admin =>
              onSuccess(courseService.publishCourse(courseId)) { _ =>
                complete(StatusCodes.OK)
              }
            }
          }
        } ~
        path(JavaUUID / "hide") { courseId =>
          put {
            authenticatedWithRole("admin") { admin =>
              onSuccess(courseService.hideCourse(courseId)) { _ =>
                complete(StatusCodes.OK)
              }
            }
          }
        } ~
        path(JavaUUID / "edit" / "content") { courseId =>
          get {
            authenticatedWithRole("tutor") { tutor =>
              onSuccess {
                for {
                  ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                  courseOpt <- courseService.getById(courseId) if courseOpt.nonEmpty
                  result <- courseService.getModulesWithLessons(courseId)
                } yield (courseOpt.get, result.sortBy(_.order).map(mls => mls.copy(lessons = mls.lessons.sortBy(_.order))))
              } {
                case (course, modulesWithLessons) =>
                  complete(course_content(tutor, course, modulesWithLessons))
              }
            }
          }
        } ~
        path(JavaUUID / "edit" / "module") { courseId =>
          post {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[CreateModuleRequest]) { body =>
                onSuccess(courseService.addModule(body.name, body.description, courseId)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          } ~ put {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[UpdateModuleRequest]) { body =>
                onSuccess(courseService.updateModule(body.id, body.name, body.description)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          } ~ delete {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[DeleteModuleRequest]) { body =>
                onSuccess(courseService.deleteModule(body.id)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          }
        } ~
        path(JavaUUID / "edit" / "module" / "move") { courseId =>
          put {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[MoveModuleRequest]) { body =>
                onSuccess(courseService.moveModule(courseId, body.id, body.direction)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          }
        } ~
        path(JavaUUID / "edit" / "lesson") { courseId =>
          post {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[CreateLessonRequest]) { body =>
                onSuccess(courseService.addLesson(body.moduleId, body.name)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          } ~
            put {
              authenticatedWithRole("tutor") { tutor =>
                entity(as[UpdateLessonRequest]) { body =>
                  onSuccess(courseService.updateLesson(body.id, body.name)) { _ =>
                    complete(StatusCodes.OK)
                  }
                }
              }
            } ~
            delete {
              authenticatedWithRole("tutor") { tutor =>
                entity(as[DeleteLessonRequest]) { body =>
                  onSuccess(courseService.deleteLesson(body.id)) { _ =>
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
        } ~
        path(JavaUUID / "edit" / "lesson" / "move") { courseId =>
          put {
            authenticatedWithRole("tutor") { tutor =>
              entity(as[MoveLessonRequest]) { body =>
                onSuccess(courseService.moveLesson(body.id, body.moduleId, body.direction)) { _ =>
                  complete(StatusCodes.OK)
                }
              }
            }
          }
        } ~
        path(JavaUUID / "edit" / "lesson" / JavaUUID) { (courseId, lessonId) =>
          get {
            authenticatedWithRole("tutor") { tutor =>
              onSuccess {
                for {
                  ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                  courseOpt <- courseService.getById(courseId) if courseOpt.nonEmpty
                  lessonOpt <- courseService.getLesson(lessonId) if lessonOpt.nonEmpty
                } yield (courseOpt.get, lessonOpt.get)
              } { case (course, lesson) =>
                complete(course_lesson_edit(tutor, course, lesson, compact(render(lesson.content))))
              }
            }
          } ~
            put {
              authenticatedWithRole("tutor") { tutor =>
                entity(as[UpdateLessonContentRequest]) { body =>
                  onSuccess {
                    for {
                      ableToEdit <- courseService.isAbleToEdit(tutor.id, courseId) if ableToEdit
                      courseOpt <- courseService.getById(courseId) if courseOpt.nonEmpty
                      lessonOpt <- courseService.getLesson(lessonId) if lessonOpt.nonEmpty
                      result <- courseService.updateLessonContent(lessonId, body.content)
                    } yield result
                  } { _ =>
                    complete(StatusCodes.OK)
                  }
                }
              }
            }
        }
    }
  )
}
