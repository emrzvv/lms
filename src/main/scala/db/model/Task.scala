package db.model

import java.util.UUID

case class Task(id: UUID,
                lessonId: UUID,
                question: String,
                suggestedAnswer: String,
                points: Int)
