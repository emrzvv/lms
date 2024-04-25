package component

import db.model.UserRepository

trait Repositories {
  def userRepository: UserRepository
}
