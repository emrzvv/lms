package component

import service.{CourseService, UserService}

trait Services {
  def userService: UserService
  def courseService: CourseService
}
