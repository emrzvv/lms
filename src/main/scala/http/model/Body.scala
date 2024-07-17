package http.model

case class UpdateCourseRequest(name: String,
                                shortDescription: String,
                                description: String,
                                previewImageUrl: Option[String],
                                estimatedTime: Int,
                                isFree: Boolean)
