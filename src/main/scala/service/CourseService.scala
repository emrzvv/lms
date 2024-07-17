package service

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import db.model.{Course, CourseRepository, User, UsersCoursesMapping}

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
  def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)]
  def isAbleToEdit(userId: UUID, courseId: UUID): Future[Boolean]
  def getUsersOnCourse(courseId: UUID): Future[Seq[User]]
  def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]]
  def addUserToCourse(userId: UUID, courseId: UUID): Future[Int]
  def removeUserFromCourse(userId: UUID, courseId: UUID): Future[Int]
  def grantCourseAccessToUser(userId: UUID, courseId: UUID): Future[Int]
  def publishCourse(courseId: UUID): Future[Int]
  def hideCourse(courseId: UUID): Future[Int]
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
    val maybeUserCourseMapping = UsersCoursesMapping(userId = user.id, courseId = id, ableToEdit = true)
    courseRepository.addWithMapping(maybeCourse, maybeUserCourseMapping)
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

  def allFreeAndPublished(limit: Int = 0, offset: Int = 0): Future[(Seq[Course], Int)] = {
    courseRepository.allFreeAndPublished(limit, offset)
  }

  def isAbleToEdit(userId: UUID, courseId: UUID): Future[Boolean] = {
    courseRepository.getUserCourse(userId, courseId).map(_.exists(_.ableToEdit))
  }

  def getUsersOnCourse(courseId: UUID): Future[Seq[User]] = {
    courseRepository.getUsersOnCourse(courseId)
  }

  def getUsersOnCourseWithRights(courseId: UUID): Future[(Seq[(User, Boolean)])] = {
    courseRepository.getUsersOnCourseWithRights(courseId)
  }

  override def addUserToCourse(userId: UUID, courseId: UUID): Future[Int] = {
    courseRepository.addToMapping(userId, courseId)
  }

  override def removeUserFromCourse(userId: UUID, courseId: UUID): Future[Int] = {
    courseRepository.removeFromMapping(userId, courseId)
  }

  override def grantCourseAccessToUser(userId: UUID, courseId: UUID): Future[Int] = {
    courseRepository.setValuesInMapping(userId, courseId, ableToEdit = true)
  }

  override def publishCourse(courseId: UUID): Future[Int] = {
    for {
      newCourse <- courseRepository.getById(courseId) if newCourse.nonEmpty
      result <- courseRepository.update(newCourse.get.copy(isPublished = true))
    } yield result
  }


  override def hideCourse(courseId: UUID): Future[Int] = {
    for {
      newCourse <- courseRepository.getById(courseId) if newCourse.nonEmpty
      result <- courseRepository.update(newCourse.get.copy(isPublished = false))
    } yield result
  }
}
