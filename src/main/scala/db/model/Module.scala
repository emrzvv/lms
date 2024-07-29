package db.model

import java.time.LocalDateTime
import java.util.UUID

case class Module(id: UUID,
                  name: String,
                  courseId: UUID,
                  description: Option[String],
                  order: Int,
                  createdAt: LocalDateTime)
