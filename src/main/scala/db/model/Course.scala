package db.model

import component.impl.MyPostgresProfile
import db.Tables
import slick.jdbc.GetResult
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
  def update(course: Course): Future[Int]
  def getById(uuid: UUID): Future[Option[Course]]
  def all(limit: Int, offset: Int): Future[(Seq[Course], Int)]
}

object CourseRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) =
    new CourseRepositoryImpl(db, profile, tables, executionContext)
}

class CourseRepositoryImpl(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) extends CourseRepository {
  import profile.api._
  import tables.coursesQuery

  implicit val ev: ExecutionContext = executionContext

  implicit val getCourseResult: GetResult[Course] = GetResult(r =>
    Course(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextString(),
      r.nextStringOption(),
      r.nextInt(),
      r.nextTimestamp().toLocalDateTime,
      r.nextBoolean(),
      r.nextBoolean()
    )
  )

  override def add(course: Course): Future[Int] = db.run {
    coursesQuery += course
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

  override def all(limit: Int, offset: Int): Future[(Seq[Course], Int)] = {
    val updatedLimit = if (limit == 0) Int.MaxValue else limit

    val getCoursesAction = sql"select * from courses as c order by c.created_at limit $updatedLimit offset $offset".as[Course]
    val countCoursesAction = sql"select count(*) from courses".as[Int].head
    val query = for {
      courses <- getCoursesAction
      count <- countCoursesAction
    } yield (courses, count)

    db.run {
      query.transactionally
    }
  }
}