package db.model

import java.util.UUID

case class ModuleShort(id: UUID,
                                  name: String,
                                  description: String,
                                  order: Int)

case class LessonShort(id: UUID,
                       name: String,
                       order: Int)

case class ModuleLessonOptShort(id: UUID,
                                name: String,
                                description: Option[String],
                                order: Int,
                                lesson: Option[LessonShort])

case class ModuleWithLessonsShort(id: UUID,
                                  name: String,
                                  description: Option[String],
                                  order: Int,
                                  lessons: Seq[LessonShort])