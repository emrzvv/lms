package http.model

import java.util.UUID

case class UpdateCourseRequest(name: String,
                                shortDescription: String,
                                description: String,
                                previewImageUrl: Option[String],
                                estimatedTime: Int,
                                isFree: Boolean)

case class CreateModuleRequest(name: String,
                               description: Option[String])

case class UpdateModuleRequest(id: UUID,
                               name: String,
                               description: Option[String])

case class DeleteModuleRequest(id: UUID)

case class MoveModuleRequest(id: UUID, direction: String)

case class CreateLessonRequest(moduleId: UUID, name: String)

case class UpdateLessonRequest(id: UUID, moduleId: UUID, name: String)

case class DeleteLessonRequest(id: UUID)