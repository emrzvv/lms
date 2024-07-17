package db.model

import java.util.UUID

case class UsersCoursesMapping(userId: UUID,
                               courseId: UUID,
                               ableToEdit: Boolean)
