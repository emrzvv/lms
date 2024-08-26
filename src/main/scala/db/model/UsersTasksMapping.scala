package db.model

import java.time.LocalDateTime
import java.util.UUID

trait UsersTasks {
  def userId: UUID
  def taskId: UUID
}

case class UsersTasksMapping(userId: UUID,
                             taskId: UUID,
                             answer: String,
                             submittedAt: LocalDateTime,
                             points: Int)

case class UsersTasksSimpleAnswer(userId: UUID,
                                  taskId: UUID,
                                  answer: String) extends UsersTasks

case class UsersTasksChooseOne(userId: UUID,
                               taskId: UUID,
                               variants: Seq[String],
                               selectedVariant: String) extends UsersTasks

case class UsersTasksChooseMany(userId: UUID,
                                taskId: UUID,
                                variants: Seq[String],
                                selectedVariants: Seq[String]) extends UsersTasks