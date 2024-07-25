package http.model

import org.json4s.{CustomSerializer, JString}

import java.util.UUID

class UUIDSerializer extends CustomSerializer[UUID](format => (
  {
    case JString(uuid) => UUID.fromString(uuid)
    case _ => throw new IllegalArgumentException("Invalid UUID format")
  },
  {
    case uuid: UUID => JString(uuid.toString)
  }
))
