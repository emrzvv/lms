package http.model

import org.json4s.JValue

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

case class MoveLessonRequest(id: UUID, moduleId: UUID, direction: String)

case class UpdateLessonContentRequest(content: JValue)

case class CreateTaskRequest(lessonId: UUID,
                             question: String,
                             taskType: String,
                             points: Int,
                             suggestedAnswer: Option[String],
                             variants: Option[Seq[String]],
                             suggestedVariant: Option[String],
                             suggestedVariants: Option[Seq[String]])

case class UpdateTaskRequest(taskId: UUID,
                             lessonId: UUID,
                             question: String,
                             taskType: String,
                             points: Int,
                             suggestedAnswer: Option[String],
                             variants: Option[Seq[String]],
                             suggestedVariant: Option[String],
                             suggestedVariants: Option[Seq[String]])

case class DeleteTaskRequest(id: UUID)

case class SubmitTaskRequest(taskId: UUID,
                             answer: String)