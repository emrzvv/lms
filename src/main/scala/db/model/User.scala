package db.model

import component.impl.MyPostgresProfile
import http.auth.Auth
import org.json4s.{CustomSerializer, JArray, JField, JObject, JString}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.{GetResult, JdbcProfile}
import slick.lifted.ProvenShape

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

case class User(id: UUID, username: String, email: String, roles: List[String], registeredAt: LocalDate) // TODO: role -> roles

class UserTableComponent(val profile: MyPostgresProfile) {
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, None, "users") {
    def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)
    def username: Rep[String] = column[String]("username", O.Unique, O.Length(32))
    def email: Rep[String] = column[String]("email", O.Unique)
    def roles: Rep[List[String]] = column[List[String]]("roles")
    def registeredAt: Rep[LocalDate] = column[LocalDate]("registered_at", O.Default(LocalDate.now()))

    override def * : ProvenShape[User] = (id, username, email, roles, registeredAt) <> (User.tupled, User.unapply)
  }

  val userQuery: TableQuery[UserTable] = TableQuery[UserTable]
}

object UserTableComponent {
  def apply(profile: MyPostgresProfile): UserTableComponent = new UserTableComponent(profile)
}

trait UserRepository {
  def add(user: User): Future[Int]
  def update(user: User): Future[Int]
  def getById(uuid: UUID): Future[Option[User]]
  def getByUsername(username: String): Future[Option[User]]
  def getByAuthData(auth: Auth): Future[Option[User]]
  def all(limit: Int, offset: Int): Future[Seq[User]]
}

object UserRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile) =
    new UserRepositoryImpl(db, profile)
}

class UserRepositoryImpl(db: Database, profile: MyPostgresProfile) extends UserRepository {
  protected val table: UserTableComponent = UserTableComponent(profile)

  import profile.api._
  import table.userQuery

  override def add(user: User): Future[Int] = db.run {
    userQuery += user
  }

  override def update(user: User): Future[Int] = db.run {
    userQuery.filter(_.id === user.id).map(u => (u.username, u.email, u.roles)).update((user.username, user.email, user.roles))
  }

  override def getById(uuid: UUID): Future[Option[User]] = db.run {
    userQuery.filter(_.id === uuid).result.headOption
  }

  override def getByUsername(username: String): Future[Option[User]] = db.run {
    userQuery.filter(_.username === username).result.headOption
  }

  override def getByAuthData(auth: Auth): Future[Option[User]] = db.run {
    userQuery.filter(u => u.username === auth.username).result.headOption
  }

  override def all(limit: Int, offset: Int): Future[Seq[User]] = db.run {
    userQuery.drop(offset).take(limit).result
  }
}

class UserSerializer extends CustomSerializer[User] (format =>
  ({
    case JObject(List(
      JField("id", JString(i)),
      JField("username", JString(username)),
      JField("email", JString(email)),
      JField("roles", a@JArray(roles)),
      JField("registeredAt", JString(registeredAt))
    )) => User(UUID.fromString(i), username, email, a.values.map(_.toString), LocalDate.parse(registeredAt))
  },
  {
    case u: User => JObject(
      JField("id", JString(u.id.toString)) ::
        JField("username", JString(u.username)) ::
        JField("email", JString(u.email)) ::
        JField("roles", JArray(u.roles.map(JString))) ::
        JField("registeredAt", JString(u.registeredAt.toString)) ::
        Nil
    )
  })
)