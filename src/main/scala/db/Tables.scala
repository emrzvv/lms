package db

import component.impl.MyPostgresProfile
import db.model.{Category, Course, CoursesCategoriesMapping, User, UsersCoursesMapping}
import slick.lifted.ProvenShape

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

object Tables {
  def apply(profile: MyPostgresProfile): Tables = new Tables(profile)
}

class Tables(val profile: MyPostgresProfile) {
  import profile.api._

  class UserTable(tag: Tag) extends Table[User](tag, None, "users") {
    def id: Rep[UUID] = column[UUID]("id", O.PrimaryKey)
    def username: Rep[String] = column[String]("username", O.Unique)
    def email: Rep[String] = column[String]("email", O.Unique)
    def passwordHash: Rep[String] = column[String]("password_hash")
    def roles: Rep[List[String]] = column[List[String]]("roles")
    def registeredAt: Rep[LocalDate] = column[LocalDate]("registered_at", O.Default(LocalDate.now()))

    override def * : ProvenShape[User] = (id, username, email, passwordHash, roles, registeredAt) <> (User.tupled, User.unapply)
  }

  val usersQuery: TableQuery[UserTable] = TableQuery[UserTable]

  class CourseTable(tag: Tag) extends Table[Course](tag, None, "courses") { // TODO: check default values
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def creatorId = column[UUID]("creator_id")
    def shortDescription = column[String]("short_description")
    def description = column[String]("description")
    def previewImageUrl = column[Option[String]]("preview_image_url")
    def estimatedTime = column[Int]("estimated_time")
    def createdAt = column[LocalDateTime]("created_at")
    def isPublished = column[Boolean]("is_published")
    def isFree = column[Boolean]("is_free")

    def fk_creatorId = foreignKey("fk_creator_id", creatorId, usersQuery)(_.id)

    override def * : ProvenShape[Course] =
      (id, name, creatorId, shortDescription, description, previewImageUrl, estimatedTime, createdAt, isPublished, isFree) <> (Course.tupled, Course.unapply)
  }

  val coursesQuery: TableQuery[CourseTable] = TableQuery[CourseTable]

  class UsersCoursesMappingTable(tag: Tag) extends Table[UsersCoursesMapping](tag, None, "users_courses") {
    def userId = column[UUID]("user_id")
    def courseId = column[UUID]("course_id")
    def ableToEdit = column[Boolean]("able_to_edit", O.Default(false))

    def pk = primaryKey("pk", (userId, courseId))
    def fk_userId = foreignKey("fk_user_id", userId, usersQuery)(_.id)
    def fk_courseId = foreignKey("fk_course_id", courseId, coursesQuery)(_.id)

    override def * : ProvenShape[UsersCoursesMapping] =
      (userId, courseId, ableToEdit) <> (UsersCoursesMapping.tupled, UsersCoursesMapping.unapply)
  }

  val usersCoursesQuery: TableQuery[UsersCoursesMappingTable] = TableQuery[UsersCoursesMappingTable]

  class CategoriesTable(tag: Tag) extends Table[Category](tag, None, "categories") {
    def id = column[UUID]("id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[Option[String]]("description")

    override def * : ProvenShape[Category] =
      (id, name, description) <> (Category.tupled, Category.unapply)
  }

  val categoriesQuery: TableQuery[CategoriesTable] = TableQuery[CategoriesTable]

  class CoursesCategoriesMappingTable(tag: Tag) extends Table[CoursesCategoriesMapping](tag, None, "courses_categories") {
    def courseId = column[UUID]("course_id")
    def categoryId = column[UUID]("category_id")

    def pk = primaryKey("pk", (courseId, categoryId))
    def fk_courseId = foreignKey("fk_course_id", courseId, coursesQuery)(_.id)
    def fk_categoryId = foreignKey("fk_category_id", categoryId, categoriesQuery)(_.id)
    override def * : ProvenShape[CoursesCategoriesMapping] =
      (courseId, categoryId) <> (CoursesCategoriesMapping.tupled, CoursesCategoriesMapping.unapply)
  }
}