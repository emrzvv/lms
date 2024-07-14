package service

import db.model.{Course, CourseRepository, User}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait CourseService {
  def createCourse(user: User)(name: String,
                               shortDescription: String,
                               description: String,
                               previewImageUrl: Option[String],
                               estimatedTime: Int): Future[UUID]
  def getById(id: UUID): Future[Option[Course]]
  def updateCourse(id: UUID,
                    name: String,
                    shortDescription: String,
                    description: String,
                    previewImageUrl: Option[String],
                    estimatedTime: Int,
                    isFree: Boolean): Future[UUID]
  def all(limit: Int, offset: Int): Future[(Seq[Course], Int)]
}

object CourseServiceImpl {
  def apply(courseRepository: CourseRepository, executionContext: ExecutionContext) =
    new CourseServiceImpl(courseRepository, executionContext)
}

class CourseServiceImpl(courseRepository: CourseRepository,
                        executionContext: ExecutionContext) extends CourseService {
  implicit val ec: ExecutionContext = executionContext

  def createCourse(user: User)
                  (name: String,
                   shortDescription: String,
                   description: String,
                   previewImageUrl: Option[String],
                   estimatedTime: Int): Future[UUID] = {
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
      isPublished = false,
      isFree = false
    )
    courseRepository.add(maybeCourse).map(_ => id)
  }

  def getById(id: UUID): Future[Option[Course]] = {
    courseRepository.getById(id)
  }

  def updateCourse(id: UUID,
                   name: String,
                   shortDescription: String,
                   description: String,
                   previewImageUrl: Option[String],
                   estimatedTime: Int,
                   isFree: Boolean): Future[UUID] = {
    val oldCourse = courseRepository.getById(id)
    oldCourse.flatMap {
      case Some(course) =>
        val newCourse = course.copy(
          name = name,
          shortDescription = shortDescription,
          description = description,
          previewImageUrl = previewImageUrl,
          estimatedTime = estimatedTime,
          isFree = isFree
        )
        courseRepository.update(newCourse).flatMap(amount =>
          if (amount > 0) Future.successful(id)
          else Future.failed(new Exception())
        )
      case None => Future.failed(new Exception())
    }
  }

  def all(limit: Int = 0, offset: Int = 0): Future[(Seq[Course], Int)] = {
    courseRepository.all(limit, offset)
  }
}
