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
}
