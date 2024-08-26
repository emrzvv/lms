package db.model

import slick.jdbc.{GetResult, PositionedResult}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import java.util.UUID

object Results {
  implicit val getCourseResult: GetResult[Course] = GetResult(r =>
    Course(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextString(),
      r.nextStringOption(),
      r.nextInt(),
      r.nextTimestamp().toLocalDateTime,
      r.nextBoolean(),
      r.nextBoolean()
    )
  )

  implicit val getUsersCoursesMapping: GetResult[UsersCoursesMapping] = GetResult(r =>
    UsersCoursesMapping(
      r.nextObject().asInstanceOf[UUID],
      r.nextObject().asInstanceOf[UUID],
      r.nextBoolean()
    )
  )

  implicit val strList = GetResult[List[String]] (
    r => (1 to r.numColumns).map(_ => r.nextString()).toList
  )

  implicit val getUserResult: GetResult[User] = GetResult((r: PositionedResult) =>
    User(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextString(),
      r.nextString(),
      r.nextString().split(" ").toList,
      r.nextTimestamp().toLocalDateTime.toLocalDate
    )
  )

  implicit val getModuleShortResult: GetResult[ModuleShort] = GetResult(r =>
    ModuleShort(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextString(),
      r.nextInt()
    )
  )

  implicit val getLessonShort: GetResult[LessonShort] = GetResult(r =>
    LessonShort(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextInt()
    )
  )

  implicit val getModuleLessonOptShort: GetResult[ModuleLessonOptShort] = GetResult { r =>
    val id = r.nextObject().asInstanceOf[UUID]
    val name = r.nextString()
    val description = r.nextStringOption()
    val order = r.nextInt()

    val lessonFieldsExist = !r.rs.isAfterLast
    val lesson = if (lessonFieldsExist) {
      val lessonId = r.nextObjectOption().map(_.asInstanceOf[UUID])
      val lessonName = r.nextStringOption()
      val lessonOrder = r.nextIntOption()

      (lessonId, lessonName, lessonOrder) match {
        case (Some(id), Some(name), Some(order)) => Some(LessonShort(id, name, order))
        case _ => None
      }
    } else {
      None
    }

    ModuleLessonOptShort(id, name, description, order, lesson)
  }

  implicit val getModule: GetResult[Module] = GetResult(r =>
    Module(
      r.nextObject().asInstanceOf[UUID],
      r.nextString(),
      r.nextObject().asInstanceOf[UUID],
      r.nextStringOption(),
      r.nextInt(),
      r.nextTimestamp().toLocalDateTime
    )
  )

  implicit val getLesson: GetResult[Lesson] = GetResult(r =>
    Lesson(
      id = r.nextObject().asInstanceOf[UUID],
      name = r.nextString(),
      moduleId = r.nextObject().asInstanceOf[UUID],
      order = r.nextInt(),
      content = parse(r.nextString()), // String -> JValue
      createdAt = r.nextTimestamp().toLocalDateTime,
      passPointsPercentage = r.nextInt()
    )
  )

  implicit val getBaseTask: GetResult[BaseTask] = GetResult(r =>
    BaseTask(
      id = r.nextObject().asInstanceOf[UUID],
      lessonId = r.nextObject().asInstanceOf[UUID],
      question = r.nextString(),
      points = r.nextInt(),
      taskType = r.nextString()
    )
  )

  implicit val getTaskSimpleAnswerDB: GetResult[TaskSimpleAnswerDB] = GetResult(r =>
    TaskSimpleAnswerDB(
      id = r.nextObject().asInstanceOf[UUID],
      suggestedAnswer = r.nextString()
    )
  )
  implicit val getTaskChooseOneDB: GetResult[TaskChooseOneDB] = GetResult(r =>
    TaskChooseOneDB(
      id = r.nextObject().asInstanceOf[UUID],
      variants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq,
      suggestedVariant = r.nextString()
    )
  )
  implicit val getTaskChooseManyDB: GetResult[TaskChooseManyDB] = GetResult(r =>
    TaskChooseManyDB(
      id = r.nextObject().asInstanceOf[UUID],
      variants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq,
      suggestedVariants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq
    ))

  implicit val getUsersLessonsMapping: GetResult[UsersLessonsMapping] = GetResult(r =>
    UsersLessonsMapping(
      userId = r.nextObject().asInstanceOf[UUID],
      lessonId = r.nextObject().asInstanceOf[UUID]
    )
  )

  implicit val getTaskExt: GetResult[TaskExt] = GetResult(r =>
    TaskExt(
      taskId = r.nextObject().asInstanceOf[UUID],
      userId = r.nextObjectOption().asInstanceOf[Option[UUID]],
      question = r.nextString(),
      taskType = r.nextString(),
      points = r.nextInt(),
      userPoints = r.nextIntOption().getOrElse(0),
      variants = r.nextStringOption().map { str =>
        val cleanedStr = str.stripPrefix("{").stripSuffix("}")
        if (cleanedStr.isEmpty) Seq.empty[String]
        else cleanedStr.split(",").map(_.trim).toSeq
      }
    )
  )

  implicit val getUsersTasksMapping: GetResult[UsersTasksMapping] = GetResult(r =>
    UsersTasksMapping(
      userId = r.nextObject().asInstanceOf[UUID],
      taskId = r.nextObject().asInstanceOf[UUID],
      answer = r.nextString(),
      submittedAt = r.nextTimestamp().toLocalDateTime,
      points = r.nextInt()
    )
  )

  implicit val getUsersTasksSimpleAnswer: GetResult[UsersTasksSimpleAnswer] = GetResult(r =>
    UsersTasksSimpleAnswer(
      userId = r.nextObject().asInstanceOf[UUID],
      taskId = r.nextObject().asInstanceOf[UUID],
      answer = r.nextString()
    )
  )

  implicit val getUsersTasksChooseOne: GetResult[UsersTasksChooseOne] = GetResult(r =>
    UsersTasksChooseOne(
      userId = r.nextObject().asInstanceOf[UUID],
      taskId = r.nextObject().asInstanceOf[UUID],
      variants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq,
      selectedVariant = r.nextString()
    )
  )

  implicit val getUsersTasksChooseMany: GetResult[UsersTasksChooseMany] = GetResult(r =>
    UsersTasksChooseMany(
      userId = r.nextObject().asInstanceOf[UUID],
      taskId = r.nextObject().asInstanceOf[UUID],
      variants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq,
      selectedVariants = r.nextString().stripPrefix("{").stripSuffix("}").split(",").map(_.trim).toSeq
    )
  )

  implicit val strListParameter: slick.jdbc.SetParameter[Seq[UUID]] =
    slick.jdbc.SetParameter[Seq[UUID]]{ (param, pointedParameters) =>
      pointedParameters.setString(f"{${param.mkString(", ")}}")
    }
}
