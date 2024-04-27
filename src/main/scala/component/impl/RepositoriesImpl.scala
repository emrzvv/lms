package component.impl

import com.github.tminglei.slickpg.{ExPostgresProfile, PgArraySupport, PgDate2Support, PgHStoreSupport, PgLTreeSupport, PgNetSupport, PgPlayJsonSupport, PgRangeSupport, PgSearchSupport}
import component.{ActorSystemComponent, DatabaseComponent, Repositories}
import db.model.{UserRepository, UserRepositoryImpl}
import utils.Logging

trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgPlayJsonSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  def pgjson = "jsonb"

  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends ExtPostgresAPI with ArrayImplicits
    with Date2DateTimeImplicitsDuration
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }
}

object MyPostgresProfile extends MyPostgresProfile

trait RepositoriesImpl extends Repositories {
  this: DatabaseComponent with Logging with ActorSystemComponent =>
  override val userRepository: UserRepository = UserRepositoryImpl(db, MyPostgresProfile)
}