package db.model

import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._

import java.sql.Date
import java.time.LocalDateTime
import java.util.UUID

case class Course(id: UUID, name: String, description: Option[String], createdAt: LocalDateTime, lastModifiedAt: LocalDateTime)

class CourseTable(tag: Tag) extends Table[Course](tag, None, "courses")  {
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  def createdAt = column[LocalDateTime]("created_at", O.Default(LocalDateTime.now()))
  def lastModifiedAt = column[LocalDateTime]("last_modified_at", O.Default(LocalDateTime.now()))

  def creatorId = column[UUID]("",)

  override def * : ProvenShape[Course] = (id, name, description, createdAt, lastModifiedAt) <> (Course.tupled, Course.unapply)
}
