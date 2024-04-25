package component.impl

import component.{ActorSystemComponent, DatabaseComponent, Repositories}
import db.model.{UserRepository, UserRepositoryImpl}
import utils.Logging

trait RepositoriesImpl extends Repositories {
  this: DatabaseComponent with Logging with ActorSystemComponent =>

  override val userRepository: UserRepository = UserRepositoryImpl(db, profile)
}