package http.model

case class UpdateCourseRequest(name: String,
                                shortDescription: String,
                                description: String,
                                previewImageUrl: Option[String],
                                estimatedTime: Int,
                                isFree: Boolean)

case class UpdateModuleRequest(name: String,
                               description: Option[String])

case class CreateLessonRequest(name: String)

case class UpdateLessonRequest(name: String)