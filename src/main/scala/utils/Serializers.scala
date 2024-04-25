package utils

import db.model.UserSerializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.native.Serialization
import org.json4s.{Formats, NoTypeHints, jackson}

trait Serializers extends Json4sSupport {
  implicit val serialization: jackson.Serialization.type = jackson.Serialization

  implicit val formats: Formats =
    Serialization.formats(NoTypeHints) +
      new UserSerializer
}
