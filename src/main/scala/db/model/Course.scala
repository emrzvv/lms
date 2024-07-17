package db.model

import component.impl.MyPostgresProfile
import db.Tables
import slick.jdbc.{GetResult, PositionedResult}
import slick.lifted.ProvenShape
import slick.jdbc.JdbcBackend.Database

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.{TimeZone, UUID}
import scala.concurrent.{ExecutionContext, Future}

case class Course(id: UUID,
                  name: String,
                  creatorId: UUID,
                  shortDescription: String,
                  description: String,
                  previewImageUrl: Option[String],
                  estimatedTime: Int,
                  createdAt: LocalDateTime,
                  isPublished: Boolean,
                  isFree: Boolean)

trait CourseRepository {
  def add(course: Course): Future[Int]
  def addWithMapping(course: Course, mapping: UsersCoursesMapping): Future[UUID]
  def update(course: Course): Future[Int]
  def getById(uuid: UUID): Future[Option[Course]]
  def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)]
  def getUserCourse(userId: UUID, courseId: UUID): Future[Option[UsersCoursesMapping]]
  def getUsersOnCourse(courseId: UUID): Future[Seq[User]]
  def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]]
  def addToMapping(userId: UUID, courseId: UUID): Future[Int]
  def removeFromMapping(userId: UUID, courseId: UUID): Future[Int]
  def setValuesInMapping(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int]
}

object CourseRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) =
    new CourseRepositoryImpl(db, profile, tables, executionContext)
}

class CourseRepositoryImpl(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) extends CourseRepository {

  import profile.api._
  import tables.coursesQuery
  import tables.usersCoursesQuery
  import Results._

  implicit val ev: ExecutionContext = executionContext

  override def add(course: Course): Future[Int] = db.run {
    coursesQuery += course
  }

  override def addWithMapping(course: Course, mapping: UsersCoursesMapping): Future[UUID] = {
    val action = for {
      _ <- coursesQuery += course
      _ <- usersCoursesQuery += mapping
    } yield course.id
    db.run(action.transactionally)
  }

  override def update(course: Course): Future[Int] = db.run {
    coursesQuery.filter(_.id === course.id)
      .map(c =>
        (c.name, c.shortDescription, c.description, c.previewImageUrl, c.estimatedTime, c.isPublished, c.isFree))
        .update((course.name, course.shortDescription, course.description, course.previewImageUrl, course.estimatedTime, course.isPublished, course.isFree))
  }

  override def getById(uuid: UUID): Future[Option[Course]] = db.run {
    coursesQuery.filter(_.id === uuid).result.headOption
  }

  override def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)] = {
    val updatedLimit = if (limit == 0) Int.MaxValue else limit

    val getCoursesAction = sql"select * from courses as c where c.is_free and c.is_published order by c.created_at limit $updatedLimit offset $offset".as[Course]
    val countCoursesAction = sql"select count(*) from courses where is_free and is_published".as[Int].head
    val query = for {
      courses <- getCoursesAction
      count <- countCoursesAction
    } yield (courses, count)

    db.run {
      query.transactionally
    }
  }

  override def getUserCourse(userId: UUID, courseId: UUID): Future[Option[UsersCoursesMapping]] = {
    val query =
      sql"select * from users_courses as uc where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid".as[UsersCoursesMapping].headOption
    db.run(query)
  }

  override def getUsersOnCourse(courseId: UUID): Future[Seq[User]] = {
    val query =
      sql"""
           select u.id,
           u.username,
           u.email,
           u.password_hash,
           u.roles,
           u.registered_at from users_courses as uc
           join users as u on uc.user_id = u.id where uc.course_id = ${courseId.toString}::uuid
         """.as[User]
    db.run(query)
  }

  override def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]] = {
    val query =
      sql"""
           select u.id,
           u.username,
           u.email,
           u.password_hash,
           u.roles,
           u.registered_at,
           uc.able_to_edit from users_courses as uc
           join users as u on uc.user_id = u.id where uc.course_id = ${courseId.toString}::uuid
         """.as[(User, Boolean)]
    db.run(query)
  }


  override def addToMapping(userId: UUID, courseId: UUID): Future[Int] = {
    val maybeMapping = UsersCoursesMapping(userId, courseId, ableToEdit = false)
    db.run(usersCoursesQuery += maybeMapping)
  }


  override def removeFromMapping(userId: UUID, courseId: UUID): Future[Int] = {
    val query =
      sqlu"""
           delete from users_courses as uc
           where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid
         """
    db.run(query)
  }


  override def setValuesInMapping(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int] = {
    val query =
      sqlu"""
        update users_courses as uc
        set able_to_edit = $ableToEdit
        where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid
      """
    db.run(query)
  }
}