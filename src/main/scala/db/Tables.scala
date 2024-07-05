package db

import component.impl.MyPostgresProfile
import db.model.{Course, User}
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object Tables {
  def apply(profile: MyPostgresProfile): Tables = new Tables(profile)
}

class Tables(val profile: MyPostgresProfile) {
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, None, "users") {
    def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)
    def username: Rep[String] = column[String]("username", O.Unique)
    def email: Rep[String] = column[String]("email", O.Unique)
    def passwordHash: Rep[String] = column[String]("password_hash")
    def roles: Rep[List[String]] = column[List[String]]("roles")
    def registeredAt: Rep[LocalDate] = column[LocalDate]("registered_at", O.Default(LocalDate.now()))

    override def * : ProvenShape[User] = (id, username, email, passwordHash, roles, registeredAt) <> (User.tupled, User.unapply)
  }

  val usersQuery: TableQuery[UserTable] = TableQuery[UserTable]

  class CourseTable(tag: Tag) extends Table[Course](tag, None, "courses") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def creatorId = column[UUID]("creator_id")
    def shortDescription = column[String]("short_description")
    def description = column[String]("description")
    def previewImageUrl = column[Option[String]]("preview_image_url")
    def estimatedTime = column[Int]("estimated_time")
    def createdAt = column[LocalDateTime]("created_at")
    def lastModifiedAt = column[LocalDateTime]("last_modified_at")
    def isPublished = column[Boolean]("is_published")

    def fk_creatorId = foreignKey("fk_creator_id", creatorId, usersQuery)(_.id)

    override def * : ProvenShape[Course] =
      (id, name, creatorId, shortDescription, description, previewImageUrl, estimatedTime, createdAt, lastModifiedAt, isPublished) <> (Course.tupled, Course.unapply)
  }

  val coursesQuery: TableQuery[CourseTable] = TableQuery[CourseTable]


}