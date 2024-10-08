package service

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import db.model.{BaseTask, Course, CourseRepository, Lesson, Module, ModuleLessonOptShort, ModuleWithLessonsShort, ModuleWithLessonsShortExt, Task, TaskChooseMany, TaskChooseManyDB, TaskChooseOne, TaskChooseOneDB, TaskExt, TaskFactory, TaskSimpleAnswer, TaskSimpleAnswerDB, User, UsersCoursesMapping, UsersTasks}
import http.model.{CreateTaskRequest, UpdateTaskRequest}
import org.json4s.{JObject, JValue}
import utils.Pimp.RichString

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait CourseService {
  def createCourse(user: User)(name: String,
                               shortDescription: String,
                               description: String,
                               previewImageUrl: Option[String],
                               estimatedTime: Int): Future[UUID]
  def getById(id: UUID): Future[Option[Course]]
  def updateCourse(id: UUID,
                    name: String,
                    shortDescription: String,
                    description: String,
                    previewImageUrl: Option[String],
                    estimatedTime: Int,
                    isFree: Boolean): Future[UUID]
  def allFreeAndPublished(limit: Int, offset: Int): Future[(Seq[Course], Int)]
  def isAbleToEdit(userId: UUID, courseId: UUID): Future[Boolean]
  def getUsersOnCourse(courseId: UUID): Future[Seq[User]]
  def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]]
  def addUserToCourse(userId: UUID, courseId: UUID): Future[Int]
  def removeUserFromCourse(userId: UUID, courseId: UUID): Future[Int]
  def grantCourseAccessToUser(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int]
  def publishCourse(courseId: UUID): Future[Int]
  def hideCourse(courseId: UUID): Future[Int]
  def getModulesWithLessons(courseId: UUID): Future[Seq[ModuleWithLessonsShort]]
  def getModulesWithLessonsPrettified(courseId: UUID): Future[Seq[ModuleWithLessonsShort]]
  def addModule(name: String, description: Option[String], courseId: UUID): Future[Int]
  def getModuleById(id: UUID): Future[Option[Module]]
  def deleteModule(id: UUID): Future[Int]
  def updateModule(id: UUID, name: String, description: Option[String]): Future[Int]
  def moveModule(courseId: UUID, moduleId: UUID, direction: String): Future[Int]
  def addLesson(moduleId: UUID, name: String): Future[Int]
  def updateLesson(lessonId: UUID, name: String): Future[Int]
  def deleteLesson(lessonId: UUID): Future[Int]
  def moveLesson(lessonId: UUID, moduleId: UUID, direction: String): Future[Int]
  def getLesson(lessonId: UUID): Future[Option[Lesson]]
  def updateLessonContent(lessonId: UUID, content: JValue): Future[Int]
  def getCoursesByUser(userId: UUID): Future[Seq[Course]]
  def getCreatedCoursesByUser(userId: UUID): Future[Seq[Course]]
  def checkIfUserOnCourse(userId: UUID, courseId: UUID): Future[Boolean]
  def getRelatedCoursesByUser(userId: UUID): Future[(Seq[Course], Seq[Course])]
  def defaultCourseAccessChecks(userId: UUID, courseId: UUID): Future[(Boolean, Option[Course])]
  def getTasksByLessonId(lessonId: UUID): Future[Seq[Task]]
  def createTask(lessonId: UUID, body: CreateTaskRequest): Future[Int]
  def updateTask(taskId: UUID, body: UpdateTaskRequest): Future[Int]
  def getTaskById(taskId: UUID): Future[Option[Task]]
  def deleteTask(taskId: UUID): Future[Int]
  def registerUserOnLesson(userId: UUID, lessonId: UUID): Future[Int]
  def getUserTasksByLessonId(userId: UUID, lessonId: UUID): Future[Seq[TaskExt]]
  def getUserAnswersByTasks(userId: UUID, tasksIds: Seq[UUID]): Future[Seq[UsersTasks]]
  def registerAnswerOnTask(userId: UUID, task: Task, answer: String): Future[Int]
  def checkIfCompletedForEachLesson(userId: UUID, modulesWithLessons: Seq[ModuleWithLessonsShort]): Future[Seq[ModuleWithLessonsShortExt]]
}

object CourseServiceImpl {
  def apply(courseRepository: CourseRepository, executionContext: ExecutionContext) =
    new CourseServiceImpl(courseRepository, executionContext)
}

class CourseServiceImpl(courseRepository: CourseRepository,
                        executionContext: ExecutionContext) extends CourseService {
  implicit val ec: ExecutionContext = executionContext

  def createCourse(user: User)
                  (name: String,
                   shortDescription: String,
                   description: String,
                   previewImageUrl: Option[String],
                   estimatedTime: Int): Future[UUID] = {
    val dateTime = LocalDateTime.now()
    val id = UUID.randomUUID()
    val maybeCourse = Course(
      id = id,
      name = name,
      creatorId = user.id,
      shortDescription = shortDescription,
      description = description,
      previewImageUrl = previewImageUrl,
      estimatedTime = estimatedTime,
      createdAt = dateTime,
      isPublished = false,
      isFree = false
    )
    val maybeUserCourseMapping = UsersCoursesMapping(userId = user.id, courseId = id, ableToEdit = true)
    courseRepository.addWithMapping(maybeCourse, maybeUserCourseMapping)
  }

  def getById(id: UUID): Future[Option[Course]] = {
    courseRepository.getById(id)
  }

  def updateCourse(id: UUID,
                   name: String,
                   shortDescription: String,
                   description: String,
                   previewImageUrl: Option[String],
                   estimatedTime: Int,
                   isFree: Boolean): Future[UUID] = {
    val oldCourse = courseRepository.getById(id)
    oldCourse.flatMap {
      case Some(course) =>
        val newCourse = course.copy(
          name = name,
          shortDescription = shortDescription,
          description = description,
          previewImageUrl = previewImageUrl,
          estimatedTime = estimatedTime,
          isFree = isFree
        )
        courseRepository.update(newCourse).flatMap(amount =>
          if (amount > 0) Future.successful(id)
          else Future.failed(new Exception())
        )
      case None => Future.failed(new Exception())
    }
  }

  def allFreeAndPublished(limit: Int = 0, offset: Int = 0): Future[(Seq[Course], Int)] = {
    courseRepository.allFreeAndPublished(limit, offset)
  }

  def isAbleToEdit(userId: UUID, courseId: UUID): Future[Boolean] = {
    courseRepository.getUserCourse(userId, courseId).map(_.exists(_.ableToEdit))
  }

  def getUsersOnCourse(courseId: UUID): Future[Seq[User]] = {
    courseRepository.getUsersOnCourse(courseId)
  }

  def getUsersOnCourseWithRights(courseId: UUID): Future[Seq[(User, Boolean)]] = {
    courseRepository.getUsersOnCourseWithRights(courseId)
  }

  override def addUserToCourse(userId: UUID, courseId: UUID): Future[Int] = {
    courseRepository.addToMapping(userId, courseId)
  }

  override def removeUserFromCourse(userId: UUID, courseId: UUID): Future[Int] = {
    courseRepository.removeFromMapping(userId, courseId)
  }

  override def grantCourseAccessToUser(userId: UUID, courseId: UUID, ableToEdit: Boolean): Future[Int] = {
    courseRepository.setValuesInMapping(userId, courseId, ableToEdit = ableToEdit)
  }

  override def publishCourse(courseId: UUID): Future[Int] = {
    for {
      newCourse <- courseRepository.getById(courseId) if newCourse.nonEmpty
      result <- courseRepository.update(newCourse.get.copy(isPublished = true))
    } yield result
  }


  override def hideCourse(courseId: UUID): Future[Int] = {
    for {
      newCourse <- courseRepository.getById(courseId) if newCourse.nonEmpty
      result <- courseRepository.update(newCourse.get.copy(isPublished = false))
    } yield result
  }

  override def getModulesWithLessons(courseId: UUID): Future[Seq[ModuleWithLessonsShort]] = {
    def groupLessonsByModule(modulesWithLessons: Seq[ModuleLessonOptShort]): Seq[ModuleWithLessonsShort] = {
      val grouped = modulesWithLessons.groupBy(m => (m.id, m.name, m.description, m.order))

      grouped.map { case ((id, name, description, order), lessons) =>
        val lessonList = lessons.flatMap(_.lesson)
        ModuleWithLessonsShort(id, name, description, order, lessonList)
      }.toSeq
    }

    for {
      moduleLessonSeq <- courseRepository.getModulesWithLessonsShort(courseId)
      moduleLessonGrouped = groupLessonsByModule(moduleLessonSeq)
    } yield moduleLessonGrouped
  }

  override def addModule(name: String, description: Option[String], courseId: UUID): Future[Int] = {
    for {
      modulesOrdered <- courseRepository.getModulesOrdered(courseId)
      lastModuleOrder = modulesOrdered.lastOption.map(_.order).getOrElse(0)
      newModule = Module(
        id = UUID.randomUUID(),
        name = name,
        description = description,
        order = lastModuleOrder + 1,
        courseId = courseId,
        createdAt = LocalDateTime.now()
      )
      result <- courseRepository.addModule(newModule)
    } yield result
  }

  override def getModuleById(id: UUID): Future[Option[Module]] = {
    courseRepository.getModuleById(id)
  }

  override def deleteModule(id: UUID): Future[Int] = {
    for {
      module <- getModuleById(id) if module.nonEmpty
      result <- courseRepository.deleteModule(id)
    } yield result
  }

  override def updateModule(id: UUID, name: String, description: Option[String]): Future[Int] = {
    courseRepository.updateModule(id, name, description)
  }


  override def moveModule(courseId: UUID, moduleId: UUID, direction: String): Future[Int] = {
    for {
      module <- getModuleById(moduleId) if module.nonEmpty
      moduleToSwapOpt <-
        if (direction == "up") courseRepository.getUpperModuleByOrder(module.get)
        else if (direction == "down") courseRepository.getLowerModuleByOrder(module.get)
        else Future(None)
      result <-
        moduleToSwapOpt match {
          case Some(moduleToSwap) => courseRepository.swapModuleOrders(module.get, moduleToSwap)
          case None => Future.successful(0)
        }
    } yield result
  }

  override def addLesson(moduleId: UUID, name: String): Future[Int] = {
    for {
      lessons <- courseRepository.getLessonsShortByModuleOrdered(moduleId)
      lastLessonOrder = lessons.lastOption.map(_.order).getOrElse(0) + 1
      lesson = Lesson(
        id = UUID.randomUUID(),
        name = name,
        moduleId = moduleId,
        order = lastLessonOrder,
        content = JObject(),
        createdAt = LocalDateTime.now(),
        passPointsPercentage = 100
      )
      result <- courseRepository.addLesson(lesson)
    } yield result
  }

  override def updateLesson(lessonId: UUID, name: String): Future[Int] = {
    courseRepository.updateLesson(lessonId, name)
  }

  override def deleteLesson(lessonId: UUID): Future[Int] = {
    courseRepository.deleteLesson(lessonId)
  }

  override def moveLesson(lessonId: UUID, moduleId: UUID, direction: String): Future[Int] = {
    for {
      lessonShort <- courseRepository.getLessonShortById(lessonId) if lessonShort.nonEmpty
      lessonToSwapOpt <-
        if (direction == "up") courseRepository.getUpperLessonShortByOrder(lessonShort.get, moduleId)
        else if (direction == "down") courseRepository.getLowerLessonShortByOrder(lessonShort.get, moduleId)
        else Future(None)

      result <-
        lessonToSwapOpt match {
          case Some(lessonToSwap) => courseRepository.swapLessonOrders(lessonShort.get, lessonToSwap)
          case None => Future.successful(0)
        }
    } yield result
  }

  override def getLesson(lessonId: UUID): Future[Option[Lesson]] = {
    courseRepository.getLessonById(lessonId)
  }

  override def updateLessonContent(lessonId: UUID, content: JValue): Future[Int] = {
    courseRepository.updateLessonContent(lessonId, content)
  }

  override def getCoursesByUser(userId: UUID): Future[Seq[Course]] = {
    courseRepository.getCoursesByUser(userId)
  }

  override def checkIfUserOnCourse(userId: UUID, courseId: UUID): Future[Boolean] = {
    for {
      courseUsers <- getUsersOnCourse(courseId)
      result = courseUsers.exists(_.id == userId)
    } yield result
  }

  override def getCreatedCoursesByUser(userId: UUID): Future[Seq[Course]] = {
    courseRepository.getCreatedCoursesByUser(userId)
  }

  override def getRelatedCoursesByUser(userId: UUID): Future[(Seq[Course], Seq[Course])] = {
    for {
      enrolledCourses <- courseRepository.getCoursesByUser(userId)
      createdCourses <- courseRepository.getCreatedCoursesByUser(userId)
    } yield (enrolledCourses, createdCourses)
  }

  override def defaultCourseAccessChecks(userId: UUID, courseId: UUID): Future[(Boolean, Option[Course])] = {
    for {
      courseOpt <- getById(courseId)
      isUserOnCourse <- checkIfUserOnCourse(userId, courseId)
    } yield (courseOpt.nonEmpty && isUserOnCourse, courseOpt)
  }

  override def getModulesWithLessonsPrettified(courseId: UUID): Future[Seq[ModuleWithLessonsShort]] = {
    getModulesWithLessons(courseId).map(_.sortBy(_.order)
      .map { mls =>
        mls.copy(
          name = mls.name.truncateWithEllipsis(),
          lessons = mls.lessons.sortBy(_.order).map(l => l.copy(name = l.name.truncateWithEllipsis())))
      })
  }

  override def getTasksByLessonId(lessonId: UUID): Future[Seq[Task]] = {
    for {
      tasksGeneral <- courseRepository.getTasksByLessonId(lessonId)
      tasks <- Future.sequence(tasksGeneral.map { baseTask =>
        courseRepository.getTaskByIdAndType(baseTask.id, baseTask.taskType).map {
          case Some(taskDB) => Some(TaskFactory.fromBaseTask(baseTask, taskDB))
          case None => None
        }
      })
    } yield tasks.flatten
  }

  override def createTask(lessonId: UUID, body: CreateTaskRequest): Future[Int] = {
    val task: Task = body.taskType match {
      case "TaskSimpleAnswer" =>
        TaskSimpleAnswer(
          id = UUID.randomUUID(),
          lessonId = lessonId,
          question = body.question,
          points = body.points,
          taskType = "simple_answer",
          suggestedAnswer = body.suggestedAnswer.getOrElse("")
        )
      case "TaskChooseOne" =>
        TaskChooseOne(
          id = UUID.randomUUID(),
          lessonId = lessonId,
          question = body.question,
          points = body.points,
          taskType = "choose_one",
          variants = body.variants.getOrElse(Seq[String]()),
          suggestedVariant = body.suggestedVariant.getOrElse("")
        )
      case "TaskChooseMany" =>
        TaskChooseMany(
          id = UUID.randomUUID(),
          lessonId = lessonId,
          question = body.question,
          points = body.points,
          taskType = "choose_many",
          variants = body.variants.getOrElse(Seq[String]()),
          suggestedVariants = body.suggestedVariants.getOrElse(Seq[String]())
        )
    }

    courseRepository.createTask(task)
  }

  override def updateTask(taskId: UUID, body: UpdateTaskRequest): Future[Int] = {
    val updatedTask = body.taskType match {
      case "TaskSimpleAnswer" =>
        TaskSimpleAnswer(
          id = taskId,
          lessonId = body.lessonId,
          question = body.question,
          points = body.points,
          taskType = "simple_answer",
          suggestedAnswer = body.suggestedAnswer.getOrElse("")
        )
      case "TaskChooseOne" =>
        TaskChooseOne(
          id = taskId,
          lessonId = body.lessonId,
          question = body.question,
          points = body.points,
          taskType = "choose_one",
          variants = body.variants.getOrElse(Seq[String]()),
          suggestedVariant = body.suggestedVariant.getOrElse("")
        )
      case "TaskChooseMany" =>
        TaskChooseMany(
          id = taskId,
          lessonId = body.lessonId,
          question = body.question,
          points = body.points,
          taskType = "choose_many",
          variants = body.variants.getOrElse(Seq[String]()),
          suggestedVariants = body.suggestedVariants.getOrElse(Seq[String]())
        )
    }

    courseRepository.updateTask(updatedTask)
  }

  override def getTaskById(taskId: UUID): Future[Option[Task]] = {
    courseRepository.getTaskById(taskId)
  }

  override def deleteTask(taskId: UUID): Future[Int] = {
    courseRepository.deleteTask(taskId)
  }

  override def registerUserOnLesson(userId: UUID, lessonId: UUID): Future[Int] = {
    for {
      userLessonMapping <- courseRepository.getUserLessonMapping(userId, lessonId)
      result <- userLessonMapping match {
        case Some(_) => Future.successful(0)
        case None => courseRepository.registerUserOnLesson(userId, lessonId)
      }
    } yield result
  }

  override def getUserTasksByLessonId(userId: UUID, lessonId: UUID): Future[Seq[TaskExt]] = {
    courseRepository.getLessonTasksByUser(userId, lessonId)
  }

  override def registerAnswerOnTask(userId: UUID, task: Task, answer: String): Future[Int] = {
    val points = 0
    for {
      previousAnswerOpt <- courseRepository.getUserTask(userId, task.id)
      result <- previousAnswerOpt match {
        case Some(_) => courseRepository.updateUserTaskAnswer(userId, task.id, answer, points)
        case None => courseRepository.registerUserTaskAnswer(userId, task.id, answer, points)
      }
    } yield result
  }

  override def checkIfCompletedForEachLesson(userId: UUID,
                                             modulesWithLessons: Seq[ModuleWithLessonsShort]): Future[Seq[ModuleWithLessonsShortExt]] = {
    Future.sequence {
      modulesWithLessons.map { module =>
        val lessonsWithCompletion = Future.sequence {
          module.lessons.map { lesson =>
            for {
              lessonFullOpt <- courseRepository.getLessonById(lesson.id)
              lessonFull = lessonFullOpt.get
              allTasks <- courseRepository.getLessonTasksByUser(userId, lesson.id)
              score = allTasks.map(_.userPoints).sum
              max = allTasks.map(_.points).sum
              isCompleted = (score >= (lessonFull.passPointsPercentage * max) / 100)
            } yield (lesson, isCompleted)
          }
        }

        lessonsWithCompletion.map { lessons =>
          ModuleWithLessonsShortExt(
            id = module.id,
            order = module.order,
            lessonsCompleted = lessons
          )
        }
      }
    }
  }

  override def getUserAnswersByTasks(userId: UUID, tasksIds: Seq[UUID]): Future[Seq[UsersTasks]] = {
    courseRepository.getTasksAnswersByUser(userId, tasksIds)
  }
}
