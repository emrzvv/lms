package db.model

import component.impl.MyPostgresProfile
import db.Tables
import slick.jdbc.{GetResult, PositionedResult}
import slick.lifted.ProvenShape
import slick.jdbc.JdbcBackend.Database
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.{TimeZone, UUID}
import scala.concurrent.{ExecutionContext, Future}

case class Course(id: UUID,
                  name: String,
                  creatorId: UUID,
                  shortDescription: String,
                  description: String,
                  previewImageUrl: Option[String],
                  estimatedTime: Int,
                  createdAt: LocalDateTime,
                  isPublished: Boolean,
                  isFree: Boolean)

trait CourseRepository {
  def add(course: Course): Future[Int]
  def addWithMapping(course: Course, mapping: UsersCoursesMapping): Future[UUID]
  def update(course: Course): Future[Int]
  def getById(uuid: UUID): Future[Option[Course]]
  def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)]
  def getUserCourse(userId: UUID, courseId: UUID): Future[Option[UsersCoursesMapping]]
  def getUsersOnCourse(courseId: UUID): Future[Seq[User]]
  def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]]
  def addToMapping(userId: UUID, courseId: UUID): Future[Int]
  def removeFromMapping(userId: UUID, courseId: UUID): Future[Int]
  def setValuesInMapping(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int]
  def getModulesWithLessonsShort(courseId: UUID): Future[Seq[ModuleLessonOptShort]]
  def addModule(module: Module): Future[Int]
  def getModulesOrdered(courseId: UUID): Future[Seq[Module]]
  def getModuleById(id: UUID): Future[Option[Module]]
  def deleteModule(id: UUID): Future[Int]
  def updateModule(id: UUID, name: String, description: Option[String]): Future[Int]
  def updateModule(module: Module): Future[Int]
  def getLowerModuleByOrder(module: Module): Future[Option[Module]]
  def getUpperModuleByOrder(module: Module): Future[Option[Module]]
  def swapModuleOrders(left: Module, right: Module): Future[Int]
  def getLessonsShortByModuleOrdered(moduleId: UUID): Future[Seq[LessonShort]]
  def addLesson(lesson: Lesson): Future[Int]
  def updateLesson(id: UUID, name: String): Future[Int]
  def deleteLesson(id: UUID): Future[Int]
  def getLessonShortById(id: UUID): Future[Option[LessonShort]]
  def getLowerLessonShortByOrder(lessonShort: LessonShort, moduleId: UUID): Future[Option[LessonShort]]
  def getUpperLessonShortByOrder(lessonShort: LessonShort, moduleId: UUID): Future[Option[LessonShort]]
  def swapLessonOrders(left: LessonShort, right: LessonShort): Future[Int]
}

object CourseRepositoryImpl {
  def apply(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) =
    new CourseRepositoryImpl(db, profile, tables, executionContext)
}

class CourseRepositoryImpl(db: Database, profile: MyPostgresProfile, tables: Tables, executionContext: ExecutionContext) extends CourseRepository {

  import profile.api._
  import tables.coursesQuery
  import tables.usersCoursesQuery
  import Results._

  implicit val ev: ExecutionContext = executionContext

  override def add(course: Course): Future[Int] = db.run {
    coursesQuery += course
  }

  override def addWithMapping(course: Course, mapping: UsersCoursesMapping): Future[UUID] = {
    val action = for {
      _ <- coursesQuery += course
      _ <- usersCoursesQuery += mapping
    } yield course.id
    db.run(action.transactionally)
  }

  override def update(course: Course): Future[Int] = db.run {
    coursesQuery.filter(_.id === course.id)
      .map(c =>
        (c.name, c.shortDescription, c.description, c.previewImageUrl, c.estimatedTime, c.isPublished, c.isFree))
        .update((course.name, course.shortDescription, course.description, course.previewImageUrl, course.estimatedTime, course.isPublished, course.isFree))
  }

  override def getById(uuid: UUID): Future[Option[Course]] = db.run {
    coursesQuery.filter(_.id === uuid).result.headOption
  }

  override def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)] = {
    val updatedLimit = if (limit == 0) Int.MaxValue else limit

    val getCoursesAction = sql"select * from courses as c where c.is_free and c.is_published order by c.created_at limit $updatedLimit offset $offset".as[Course]
    val countCoursesAction = sql"select count(*) from courses where is_free and is_published".as[Int].head
    val query = for {
      courses <- getCoursesAction
      count <- countCoursesAction
    } yield (courses, count)

    db.run {
      query.transactionally
    }
  }

  override def getUserCourse(userId: UUID, courseId: UUID): Future[Option[UsersCoursesMapping]] = {
    val query =
      sql"select * from users_courses as uc where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid".as[UsersCoursesMapping].headOption
    db.run(query)
  }

  override def getUsersOnCourse(courseId: UUID): Future[Seq[User]] = {
    val query =
      sql"""
           select u.id,
           u.username,
           u.email,
           u.password_hash,
           u.roles,
           u.registered_at from users_courses as uc
           join users as u on uc.user_id = u.id where uc.course_id = ${courseId.toString}::uuid
         """.as[User]
    db.run(query)
  }

  override def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]] = {
    val query =
      sql"""
           select u.id,
           u.username,
           u.email,
           u.password_hash,
           u.roles,
           u.registered_at,
           uc.able_to_edit from users_courses as uc
           join users as u on uc.user_id = u.id where uc.course_id = ${courseId.toString}::uuid
         """.as[(User, Boolean)]
    db.run(query)
  }


  override def addToMapping(userId: UUID, courseId: UUID): Future[Int] = {
    val maybeMapping = UsersCoursesMapping(userId, courseId, ableToEdit = false)
    db.run(usersCoursesQuery += maybeMapping)
  }


  override def removeFromMapping(userId: UUID, courseId: UUID): Future[Int] = {
    val query =
      sqlu"""
           delete from users_courses as uc
           where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid
         """
    db.run(query)
  }


  override def setValuesInMapping(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int] = {
    val query =
      sqlu"""
        update users_courses as uc
        set able_to_edit = $ableToEdit
        where uc.user_id = ${userId.toString}::uuid and uc.course_id = ${courseId.toString}::uuid
      """
    db.run(query)
  }

  override def getModulesWithLessonsShort(courseId: UUID): Future[Seq[ModuleLessonOptShort]] = {
//    val getModulesAction =
//      sql"""
//           select m.id, m.name, m.description, m.order from modules as m
//           where m.course_id = ${courseId.toString}::uuid
//         """.as[ModuleShort]

    val getModulesLessonsShortAction =
      sql"""
           select m.id, m.name, m.description, m.order, l.id, l.name, l.order from modules as m
           left join lessons as l
           on l.module_id = m.id
           where m.course_id = ${courseId.toString}::uuid
         """ .as[ModuleLessonOptShort]

    db.run(getModulesLessonsShortAction)
  }

  override def addModule(module: Module): Future[Int] = {
    val query =
      sqlu"""
            insert into modules (id, name, course_id, description, "order", created_at) values
              (${module.id.toString}::uuid,
                ${module.name},
                ${module.courseId.toString}::uuid,
                ${module.description.getOrElse("")},
                ${module.order},
                ${Timestamp.valueOf(module.createdAt)})
          """
    db.run(query)
  }

  override def getModulesOrdered(courseId: UUID): Future[Seq[Module]] = {
    val query =
      sql"""
           select m.id, m.name, m.course_id, m.description, m."order", m.created_at from modules as m
           where m.course_id = ${courseId.toString}::uuid order by m.order asc
         """.as[Module]

    db.run(query)
  }

  override def getModuleById(id: UUID): Future[Option[Module]] = {
    val query =
      sql"""
           select * from modules as m where m.id = ${id.toString}::uuid
         """.as[Module].headOption

    db.run(query)
  }

  override def deleteModule(id: UUID): Future[Int] = {
    val query =
      sqlu"""
            delete from modules where id = ${id.toString}::uuid
          """
    db.run(query)
  }

  override def updateModule(id: UUID, name: String, description: Option[String]): Future[Int] = {
    val query =
      sqlu"""
            update modules
            set name = ${name}, description = ${description.getOrElse("")}
            where id = ${id.toString}::uuid
          """
    db.run(query)
  }

  override def updateModule(module: Module): Future[Int] = {
    val query =
      sqlu"""
            update modules
            set name = ${module.name},
                description = ${module.description.getOrElse("")},
                "order" = ${module.order}
            where id = ${module.id.toString}::uuid
          """

    db.run(query)
  }

  override def getLowerModuleByOrder(module: Module): Future[Option[Module]] = {
    val targetOrder = module.order + 1
    val query =
      sql"""
           select * from modules
           where course_id = ${module.courseId.toString}::uuid
           and "order" = ${targetOrder}
         """.as[Module].headOption
    db.run(query)
  }

  override def getUpperModuleByOrder(module: Module): Future[Option[Module]] = {
    val targetOrder = module.order -1
    val query =
      sql"""
           select * from modules
           where course_id = ${module.courseId.toString}::uuid
           and "order" = ${targetOrder}
         """.as[Module].headOption
    db.run(query)
  }


  override def swapModuleOrders(left: Module, right: Module): Future[Int] = {
    val leftQuery =
      sqlu"""
        update modules
        set "order" = ${right.order}
        where id = ${left.id.toString}::uuid
          """
    val rightQuery =
      sqlu"""
        update modules
        set "order" = ${left.order}
        where id = ${right.id.toString}::uuid
          """
    val query = for {
      _ <- leftQuery
      _ <- rightQuery
    } yield 2
    db.run(query.transactionally)
  }

  override def getLessonsShortByModuleOrdered(moduleId: UUID): Future[Seq[LessonShort]] = {
    val query =
      sql"""
            select l.id, l.name, l."order" from lessons as l
            where l.module_id = ${moduleId.toString}::uuid
            order by l."order" asc
          """.as[LessonShort]

    db.run(query)
  }

  override def addLesson(lesson: Lesson): Future[Int] = {
    val query =
      sqlu"""
            insert into lessons (id, name, module_id, "order", content, created_at, pass_points) values
              (
              ${lesson.id.toString}::uuid,
              ${lesson.name},
              ${lesson.moduleId.toString}::uuid,
              ${lesson.order},
              ${lesson.content},
              ${Timestamp.valueOf(lesson.createdAt)},
              ${lesson.passPoints}
              )
          """

    db.run(query)
  }

  override def updateLesson(id: UUID, name: String): Future[Int] = {
    val query =
      sqlu"""
            update lessons
            set name = ${name}
            where id = ${id.toString}::uuid
          """

    db.run(query)
  }

  override def deleteLesson(id: UUID): Future[Int] = {
    val query =
      sqlu"""
            delete from lessons
            where id = ${id.toString}::uuid
          """

    db.run(query)
  }

  override def getLessonShortById(id: UUID): Future[Option[LessonShort]] = {
    val query =
      sql"""
           select id, name, "order"
           from lessons
           where id = ${id.toString}::uuid
         """.as[LessonShort].headOption

    db.run(query)
  }

  override def getLowerLessonShortByOrder(lessonShort: LessonShort, moduleId: UUID): Future[Option[LessonShort]] = {
    val targetOrder = lessonShort.order + 1
    val query =
      sql"""
           select id, name, "order"
           from lessons
           where module_id = ${moduleId.toString}::uuid and "order" = ${targetOrder}
         """.as[LessonShort].headOption

    db.run(query)
  }

  override def getUpperLessonShortByOrder(lessonShort: LessonShort, moduleId: UUID): Future[Option[LessonShort]] = {
    val targetOrder = lessonShort.order - 1
    val query =
      sql"""
           select id, name, "order"
           from lessons
           where module_id = ${moduleId.toString}::uuid and "order" = ${targetOrder}
         """.as[LessonShort].headOption

    db.run(query)
  }

  override def swapLessonOrders(left: LessonShort, right: LessonShort): Future[Int] = {
    val leftQuery =
      sqlu"""
        update lessons
        set "order" = ${right.order}
        where id = ${left.id.toString}::uuid
          """
    val rightQuery =
      sqlu"""
        update lessons
        set "order" = ${left.order}
        where id = ${right.id.toString}::uuid
          """
    val query = for {
      _ <- leftQuery
      _ <- rightQuery
    } yield 2
    db.run(query.transactionally)
  }
}