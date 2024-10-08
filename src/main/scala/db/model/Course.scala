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
  def getLessonById(id: UUID): Future[Option[Lesson]]
  def updateLessonContent(id: UUID, content: JValue): Future[Int]
  def getCoursesByUser(id: UUID): Future[Seq[Course]]
  def getCreatedCoursesByUser(id: UUID): Future[Seq[Course]]
  def getTasksByLessonId(id: UUID): Future[Seq[BaseTask]]
  def createTask(task: Task): Future[Int]
  def updateTask(task: Task): Future[Int]
  def getTaskById(taskId: UUID): Future[Option[BaseTask]]
  def getTaskByIdAndType(taskId: UUID, taskType: String): Future[Option[TaskDB]]
  def deleteTask(taskId: UUID): Future[Int]
  def getUserLessonMapping(userId: UUID, lessonId: UUID): Future[Option[UsersLessonsMapping]]
  def registerUserOnLesson(userId: UUID, lessonId: UUID): Future[Int]
  def getLessonTasksByUser(userId: UUID, lessonId: UUID): Future[Seq[TaskExt]]
  def getTasksAnswersByUser(userId: UUID, tasks: Seq[UUID]): Future[Seq[UsersTasks]]
  def getUserTask(userId: UUID, taskId: UUID): Future[Option[UsersTasksMapping]]
  def updateUserTaskAnswer(userId: UUID, taskId: UUID, answer: String, points: Int): Future[Int]
  def registerUserTaskAnswer(userId: UUID, taskId: UUID, answer: String, points: Int): Future[Int]
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
            insert into lessons (id, name, module_id, "order", content, created_at, pass_points_percentage) values
              (
              ${lesson.id.toString}::uuid,
              ${lesson.name},
              ${lesson.moduleId.toString}::uuid,
              ${lesson.order},
              ${lesson.content},
              ${Timestamp.valueOf(lesson.createdAt)},
              ${lesson.passPointsPercentage}
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

  override def getLessonById(id: UUID): Future[Option[Lesson]] = {
    val query =
      sql"""
           select * from lessons
           where id = ${id.toString}::uuid
         """.as[Lesson].headOption
    db.run(query)
  }

  override def updateLessonContent(id: UUID, content: JValue): Future[Int] = {
    val query =
      sqlu"""
            update lessons
            set content = ${content}
            where id = ${id.toString}::uuid
          """

    db.run(query)
  }

  override def getCoursesByUser(id: UUID): Future[Seq[Course]] = {
    val query =
      sql"""
          select c.id,
             c.name,
             c.creator_id,
             c.short_description,
             c.description,
             c.preview_image_url,
             c.estimated_time,
             c.created_at,
             c.is_published,
             c.is_free from users_courses uc join
          courses c on uc.course_id = c.id
          where uc.user_id = ${id.toString}::uuid
         """.as[Course]

    db.run(query)
  }

  override def getCreatedCoursesByUser(id: UUID): Future[Seq[Course]] = {
    val query =
      sql"""
           select * from courses
           where creator_id = ${id.toString}::uuid
         """.as[Course]

    db.run(query)
  }

  override def getTasksByLessonId(id: UUID): Future[Seq[BaseTask]] = {
    val query =
      sql"""
           select * from tasks
           where lesson_id = ${id.toString}::uuid
         """.as[BaseTask]

    db.run(query)
  }

  override def createTask(task: Task): Future[Int] = {
    val queryBaseTask =
      sqlu"""
            insert into tasks (id, lesson_id, question, points, task_type) values
              (
                ${task.id.toString}::uuid,
                ${task.lessonId.toString}::uuid,
                ${task.question},
                ${task.points},
                ${task.taskType}
              )
          """
    val querySubTask = task match {
      case t: TaskSimpleAnswer =>
        sqlu"""
          insert into tasks_simple_answer (id, suggested_answer) values
            (
              ${t.id.toString}::uuid,
              ${t.suggestedAnswer}
            )
            """
      case t: TaskChooseOne =>
        sqlu"""
          insert into tasks_choose_one (id, variants, suggested_variant) values
            (
              ${t.id.toString}::uuid,
              ${t.variants.mkString("{", ",", "}")}::text[],
              ${t.suggestedVariant}
            )
            """
      case t: TaskChooseMany =>
        sqlu"""
              insert into tasks_choose_many (id, variants, suggested_variants) values
              (
                ${t.id.toString}::uuid,
                ${t.variants.mkString("{", ",", "}")}::text[],
                ${t.suggestedVariants.mkString("{", ",", "}")}::text[]
              )
            """
    }

    val query = for {
      _ <- queryBaseTask
      _ <- querySubTask
    } yield 2
    db.run(query.transactionally)
  }

  override def updateTask(task: Task): Future[Int] = {
    val queryBaseTask =
      sqlu"""
            update tasks
            set question = ${task.question},
                points = ${task.points}
            where id = ${task.id.toString}::uuid
          """

    val querySubTask = task match {
      case t: TaskSimpleAnswer =>
        sqlu"""
          update tasks_simple_answer
          set suggested_answer = ${t.suggestedAnswer}
          where id = ${task.id.toString}::uuid
            """
      case t: TaskChooseOne =>
        sqlu"""
          update tasks_choose_one
          set variants =  ${t.variants.mkString("{", ",", "}")}::text[],
              suggested_variant = ${t.suggestedVariant}
              where id = ${task.id.toString}::uuid
            """
      case t: TaskChooseMany =>
        sqlu"""
              update tasks_choose_many
              set variants = ${t.variants.mkString("{", ",", "}")}::text[],
                  suggested_variants = ${t.suggestedVariants.mkString("{", ",", "}")}::text[]
                  where id = ${task.id.toString}::uuid
            """
    }


    val query = for {
      _ <- queryBaseTask
      _ <- querySubTask
    } yield 2
    db.run(query.transactionally)
  }

  override def getTaskById(taskId: UUID): Future[Option[BaseTask]] = {
    val query =
      sql"""
           select * from tasks
           where id = ${taskId.toString}::uuid
         """.as[BaseTask].headOption

    db.run(query)
  }

  override def deleteTask(taskId: UUID): Future[Int] = {
    val query =
      sqlu"""
           delete from tasks
           where id = ${taskId.toString}::uuid
         """

    db.run(query)
  }

  override def getUserLessonMapping(userId: UUID, lessonId: UUID): Future[Option[UsersLessonsMapping]] = {
    val query =
      sql"""
           select * from users_lessons
           where user_id = ${userId.toString}::uuid and lesson_id = ${lessonId.toString}::uuid
         """.as[UsersLessonsMapping].headOption

    db.run(query)
  }

  override def registerUserOnLesson(userId: UUID, lessonId: UUID): Future[Int] = {
    val query =
      sqlu"""
            insert into users_lessons (user_id, lesson_id) values
            (
              ${userId.toString}::uuid,
              ${lessonId.toString}::uuid
            )
          """

    db.run(query)
  }

  override def getLessonTasksByUser(userId: UUID, lessonId: UUID): Future[Seq[TaskExt]] = {
    val simpleTasksQuery = sql"""
      select t.id, ut.user_id, t.question, t.task_type, t.points, coalesce(ut.points, 0) as user_points, null as variants
      from tasks t
      left join users_tasks ut on t.id = ut.task_id
      where t.lesson_id = ${lessonId.toString}::uuid and t.task_type = 'simple_answer'
    """.as[TaskExt]

    val chooseOneTasksQuery = sql"""
      select t.id, ut.user_id, t.question, t.task_type, t.points, coalesce(ut.points, 0) as user_points, tc.variants
      from tasks t
      left join users_tasks ut on t.id = ut.task_id
      join tasks_choose_one tc on t.id = tc.id
      where t.lesson_id = ${lessonId.toString}::uuid and t.task_type = 'choose_one'
    """.as[TaskExt]

    val chooseManyTasksQuery = sql"""
      select t.id, ut.user_id, t.question, t.task_type, t.points, coalesce(ut.points, 0) as user_points, tc.variants
      from tasks t
      left join users_tasks ut on t.id = ut.task_id
      join tasks_choose_many tc on t.id = tc.id
      where t.lesson_id = ${lessonId.toString}::uuid and t.task_type = 'choose_many'
    """.as[TaskExt]

    // Объединяем результаты всех запросов
    val query =
      for {
        simpleTasks <- simpleTasksQuery
        chooseOneTasks <- chooseOneTasksQuery
        chooseManyTasks <- chooseManyTasksQuery
      } yield (simpleTasks ++ chooseOneTasks ++ chooseManyTasks).distinctBy(_.taskId)

    db.run(query.transactionally)
  }

  override def getUserTask(userId: UUID, taskId: UUID): Future[Option[UsersTasksMapping]] = {
    val query =
      sql"""
        select * from users_tasks
        where user_id = ${userId.toString}::uuid and task_id = ${taskId.toString}::uuid
         """.as[UsersTasksMapping].headOption

    db.run(query)
  }

  override def updateUserTaskAnswer(userId: UUID, taskId: UUID, answer: String, points: Int): Future[Int] = {
    val query =
      sqlu"""
            update users_tasks
            set answer = ${answer}, points = ${points}, submitted_at = ${Timestamp.valueOf(LocalDateTime.now())}
            where user_id = ${userId.toString}::uuid and task_id = ${taskId.toString}::uuid
          """

    db.run(query)
  }

  override def registerUserTaskAnswer(userId: UUID, taskId: UUID, answer: String, points: Int): Future[Int] = {
    val query =
      sqlu"""
            insert into users_tasks(user_id, task_id, answer, points, submitted_at) values
            (
              ${userId.toString}::uuid,
              ${taskId.toString}::uuid,
              ${answer},
              ${points},
              ${Timestamp.valueOf(LocalDateTime.now())}
            )
          """

    db.run(query)
  }

  override def getTaskByIdAndType(taskId: UUID, taskType: String): Future[Option[TaskDB]] = {
    val query = taskType match {
      case "simple_answer" => sql"""
             select * from tasks_simple_answer
             where id = ${taskId.toString}::uuid
           """.as[TaskSimpleAnswerDB].headOption
      case "choose_one" => sql"""
             select * from tasks_choose_one
             where id = ${taskId.toString}::uuid""".as[TaskChooseOneDB].headOption
      case "choose_many" => sql"""
             select * from tasks_choose_many
             where id = ${taskId.toString}::uuid""".as[TaskChooseManyDB].headOption
    }

    db.run(query)
  }

  override def getTasksAnswersByUser(userId: UUID, tasks: Seq[UUID]): Future[Seq[UsersTasks]] = {
    val querySimpleAnswer = sql"""
      select user_id, task_id, answer
      from users_tasks_simple_answer
      where user_id = ${userId.toString}::uuid and task_id = any(${tasks}::uuid[])
    """.as[UsersTasksSimpleAnswer]

    val queryChooseOne = sql"""
      select u.user_id, u.task_id, u.selected_variant, t.variants::text[] as variants
      from users_tasks_choose_one u
      join tasks_choose_one t on u.task_id = t.id
      where u.user_id = ${userId.toString}::uuid and u.task_id = any(${tasks}::uuid[])
    """.as[UsersTasksChooseOne]

    val queryChooseMany = sql"""
      select u.user_id, u.task_id, u.selected_variants::text[], t.variants::text[] as variants
      from users_tasks_choose_many u
      join tasks_choose_many t on u.task_id = t.id
      where u.user_id = ${userId.toString}::uuid and u.task_id = any(${tasks}::uuid[])
    """.as[UsersTasksChooseMany]

    val combined = for {
      simple <- querySimpleAnswer
      chooseOne <- queryChooseOne
      chooseMany <- queryChooseMany
    } yield (simple ++ chooseOne ++ chooseMany)

    db.run(combined.transactionally)
  }
}