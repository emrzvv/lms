package db.model

import java.util.UUID

case class Category(id: UUID,
                    name: String,
                    description: Option[String])
