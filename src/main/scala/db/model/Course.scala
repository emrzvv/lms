package db.model

import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime
import java.util.UUID

case class Course(id: UUID, name: String, creatorId: UUID,description: Option[String], createdAt: LocalDateTime, lastModifiedAt: LocalDateTime)

//class CourseTable(tag: Tag) extends Table[Course](tag, None, "courses")  {
//  def id = column[UUID]("id", O.PrimaryKey)
//  def name = column[String]("name")
//  def creatorId = column[UUID]("creator_id")
//  def description = column[Option[String]]("description")
//  def createdAt = column[LocalDateTime]("created_at", O.Default(LocalDateTime.now()))
//  def lastModifiedAt = column[LocalDateTime]("last_modified_at", O.Default(LocalDateTime.now()))
//
//  def creator = foreignKey("creator_fk", creatorId, userTable)(_.id)
//
//  override def * : ProvenShape[Course] = (id, name, creatorId, description, createdAt, lastModifiedAt) <> (Course.tupled, Course.unapply)
//}
