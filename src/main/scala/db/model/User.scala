package db.model

import component.impl.MyPostgresProfile
import db.Tables
import http.auth.Auth
import org.json4s.{CustomSerializer, JArray, JField, JObject, JString}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

case class User(id: UUID, username: String, email: String, passwordHash: String, roles: List[String], registeredAt: LocalDate)

trait UserRepository {
  def add(user: User): Future[Int]
  def update(user: User): Future[Int]
  def getById(uuid: UUID): Future[Option[User]]
  def getByUsername(username: String): Future[Option[User]]
  def getByAuthData(auth: Auth): Future[Option[User]]
  def all(limit: Int, offset: Int): Future[Seq[User]]
}

object UserRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile, tables: Tables) =
    new UserRepositoryImpl(db, profile, tables)
}

class UserRepositoryImpl(db: Database, profile: MyPostgresProfile, tables: Tables) extends UserRepository {
  import profile.api._
  import tables.usersQuery

  override def add(user: User): Future[Int] = db.run {
    usersQuery += user
  }

  override def update(user: User): Future[Int] = db.run {
    usersQuery.filter(_.id === user.id).map(u => (u.username, u.email, u.roles)).update((user.username, user.email, user.roles))
  }

  override def getById(uuid: UUID): Future[Option[User]] = db.run {
    usersQuery.filter(_.id === uuid).result.headOption
  }

  override def getByUsername(username: String): Future[Option[User]] = db.run {
    usersQuery.filter(_.username === username).result.headOption
  }

  override def getByAuthData(auth: Auth): Future[Option[User]] = db.run {
    usersQuery.filter(u => u.username === auth.username).result.headOption
  }

  override def all(limit: Int, offset: Int): Future[Seq[User]] = db.run {
    usersQuery.drop(offset).take(limit).result
  }
}

class UserSerializer extends CustomSerializer[User] (format =>
  ({
    case JObject(List(
      JField("id", JString(i)),
      JField("username", JString(username)),
      JField("email", JString(email)),
      JField("passwordHash", JString(passwordHash)),
      JField("roles", a@JArray(roles)),
      JField("registeredAt", JString(registeredAt))
    )) => User(UUID.fromString(i), username, email, passwordHash, a.values.map(_.toString), LocalDate.parse(registeredAt))
  },
  {
    case u: User => JObject(
      JField("id", JString(u.id.toString)) ::
        JField("username", JString(u.username)) ::
        JField("email", JString(u.email)) ::
        JField("passwordHash", JString(u.passwordHash)) ::
        JField("roles", JArray(u.roles.map(JString))) ::
        JField("registeredAt", JString(u.registeredAt.toString)) ::
        Nil
    )
  })
)