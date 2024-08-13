package db.model

import org.json4s.JValue

import java.time.LocalDateTime
import java.util.UUID

case class Lesson(id: UUID,
                  name: String,
                  moduleId: UUID,
                  order: Int,
                  content: JValue,
                  createdAt: LocalDateTime,
                  passPointsPercentage: Int)
