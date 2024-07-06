package db.model

import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class UsersCoursesMapping(userId: UUID,
                               courseId: UUID,
                               ableToEdit: Boolean)
