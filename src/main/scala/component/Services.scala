package component

import service.UserService

trait Services {
  def userService: UserService
}
