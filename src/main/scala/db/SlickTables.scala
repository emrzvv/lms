package db

import db.model.{CourseTable, UserTable}
import slick.lifted.TableQuery

object SlickTables {
  lazy val userTable = TableQuery[UserTable]
  lazy val courseTable = TableQuery[CourseTable]
}
