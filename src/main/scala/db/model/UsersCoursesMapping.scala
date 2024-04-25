package db.model

import db.SlickTables.{courseTable, userTable}
import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class UsersCoursesMapping(userId: UUID, courseId: UUID, rating: Option[Int])

class UsersCoursesMappingTable(tag: Tag) extends Table[UsersCoursesMapping](tag, None, "users_courses") {
  def userId = column[UUID]("user_id")
  def courseId = column[UUID]("course_id")
  def rating = column[Option[Int]]("rating")

  def pk = primaryKey("pk_users_courses", (userId, courseId))

  def user = foreignKey("user_fk", userId, userTable)(_.id)
  def course = foreignKey("course_fk", courseId, courseTable)(_.id)

  override def * : ProvenShape[UsersCoursesMapping] = (userId, courseId, rating) <> (UsersCoursesMapping.tupled, UsersCoursesMapping.unapply)
}
