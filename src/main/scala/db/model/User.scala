package db.model

import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDate
import java.util.UUID

case class User(id: UUID, username: String, email: String, role: String, registeredAt: LocalDate)

class UserTable(tag: Tag) extends Table[User](tag, None, "users") {
  def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)
  def username: Rep[String] = column[String]("username", O.Unique, O.Length(32))
  def email: Rep[String] = column[String]("email", O.Unique)
  def role: Rep[String] = column[String]("role")
  def registeredAt: Rep[LocalDate] = column[LocalDate]("registered_at", O.Default(LocalDate.now()))

  override def * : ProvenShape[User] = (id, username, email, role, registeredAt) <> (User.tupled, User.unapply)
}
