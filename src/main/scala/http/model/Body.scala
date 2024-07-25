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

case class CreateLessonRequest(name: String)

case class UpdateLessonRequest(name: String)