package db.model

import slick.jdbc.{GetResult, PositionedResult}

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
}
