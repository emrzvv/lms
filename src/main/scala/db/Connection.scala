package db

import com.typesafe.config.ConfigFactory
import slick.jdbc.PostgresProfile.api._


object Connection {
  val config = ConfigFactory.load()
  val db = Database.forConfig("postgres", config)
}
