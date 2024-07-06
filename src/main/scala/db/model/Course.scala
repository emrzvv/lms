package db.model

import component.impl.MyPostgresProfile
import db.Tables
import slick.lifted.ProvenShape
import slick.jdbc.JdbcBackend.Database

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

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
  def all(limit: Int, offset: Int): Future[Seq[Course]]
}

object CourseRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile, tables: Tables) =
    new CourseRepositoryImpl(db, profile, tables)
}

class CourseRepositoryImpl(db: Database, profile: MyPostgresProfile, tables: Tables) extends CourseRepository {
  import profile.api._
  import tables.coursesQuery

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

  override def all(limit: Int, offset: Int): Future[Seq[Course]] = db.run {
    coursesQuery.drop(offset).take(limit).result
  }
}