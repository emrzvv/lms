package component

import db.model.{CourseRepository, UserRepository}

trait Repositories {
  def userRepository: UserRepository
  def courseRepository: CourseRepository
}
