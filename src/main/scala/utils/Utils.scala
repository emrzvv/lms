package utils

object Pimp {
  implicit class RichString(val s: String) extends AnyVal {
    def truncateWithEllipsis(maxLength: Int = 15): String = {
      if (s.length > maxLength) {
        s.substring(0, maxLength) + "..."
      } else {
        s
      }
    }
  }
}
