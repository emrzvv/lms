package component.impl

import component.{ActorSystemComponent, Repositories, Services}
import service.{CourseService, CourseServiceImpl, UserService, UserServiceImpl}

import scala.concurrent.ExecutionContext

trait ServicesImpl extends Services {
  this: Repositories with ActorSystemComponent =>
  override val userService: UserService = UserServiceImpl(userRepository, executionContext)

  override val courseService: CourseService = CourseServiceImpl(courseRepository, executionContext)
}
