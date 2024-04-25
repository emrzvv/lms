package component.impl

import component.DatabaseComponent
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile

trait DatabaseComponentImpl extends DatabaseComponent {
  override val db = Database.forConfig("postgres")
  override val profile = new PostgresProfile {}
}
