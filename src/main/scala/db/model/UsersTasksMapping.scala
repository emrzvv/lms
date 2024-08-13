package db.model

import java.time.LocalDateTime
import java.util.UUID

case class UsersTasksMapping(userId: UUID,
                             taskId: UUID,
                             answer: String,
                             submittedAt: LocalDateTime,
                             points: Int)
