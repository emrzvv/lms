package service

import component.{ActorSystemComponent, Repositories}
import db.model.{User, UserRepository}

import scala.concurrent.{ExecutionContext, Future}
import com.github.t3hnar.bcrypt._
import component.impl.{ActorSystemComponentImpl, RepositoriesImpl}

import java.time.LocalDate
import java.util.UUID
import scala.util.{Failure, Success}


trait UserService {
  def createUser(username: String, email: String, password: String): Future[User]
  def validateUser(username: String, password: String): Future[Option[User]]
  def updateUser(updatedUser: User): Future[Int]
  def getById(id: UUID): Future[Option[User]]
  def searchUsers(query: String): Future[Seq[User]]
  def getEncryptedPassword(password: String): String
}

object UserServiceImpl {
  def apply(userRepository: UserRepository, executionContext: ExecutionContext) =
    new UserServiceImpl(userRepository, executionContext)
}

class UserServiceImpl(userRepository: UserRepository, executionContext: ExecutionContext) extends UserService {
  implicit val ec: ExecutionContext = executionContext

  def createUser(username: String, email: String, password: String): Future[User] = {
    password.bcryptSafeBounded match {
      case Success(encryptedPassword) =>
        val maybeUser = User(
          id=UUID.randomUUID(),
          username=username,
          email=email,
          passwordHash=encryptedPassword,
          roles=List("user", "admin"),
          registeredAt=LocalDate.now()
        )
        userRepository.add(maybeUser).map(_ => maybeUser)
      case Failure(exception) => Future.failed(exception)
    }
  }

  def validateUser(username: String, password: String): Future[Option[User]] = {
    userRepository
      .getByUsername(username)
      .map(_.filter(user => password.bcryptSafeBounded(user.passwordHash).isSuccess))
  }

  def updateUser(updatedUser: User): Future[Int] = {
    userRepository.update(updatedUser)
  }

  def getById(id: UUID): Future[Option[User]] = {
    userRepository.getById(id)
  }

  def searchUsers(query: String): Future[Seq[User]] = {
    userRepository.matchByUsernameOrEmail(query)
  }

  override def getEncryptedPassword(password: String): String = {
    password.bcryptSafeBounded match {
      case Success(encrypted) => encrypted
      case Failure(exception) => exception.toString
    }
  }
}
