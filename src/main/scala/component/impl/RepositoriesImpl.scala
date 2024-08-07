package component.impl

import com.github.tminglei.slickpg.{ExPostgresProfile, PgArraySupport, PgDate2Support, PgHStoreSupport, PgJson4sSupport, PgLTreeSupport, PgNetSupport, PgPlayJsonSupport, PgRangeSupport, PgSearchSupport}
import component.{ActorSystemComponent, DatabaseComponent, Repositories}
import db.Tables
import db.model.{CourseRepository, CourseRepositoryImpl, UserRepository, UserRepositoryImpl}
import org.json4s.native
import org.json4s.native.JsonMethods
import utils.Logging

import scala.concurrent.ExecutionContext

trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgJson4sSupport
  with PgPlayJsonSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  override val pgjson = "jsonb"

  // Use org.json4s.native.JsonMethods
  type DOCType = org.json4s.native.Document
  override val jsonMethods = native.JsonMethods.asInstanceOf[native.JsonMethods]


  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends ExtPostgresAPI with ArrayImplicits
    with Date2DateTimeImplicitsDuration
    with JsonImplicits
    with Json4sJsonPlainImplicits
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
  val tables: Tables = Tables(MyPostgresProfile)

  override val userRepository: UserRepository = UserRepositoryImpl(db, MyPostgresProfile, tables)
  override val courseRepository: CourseRepository = CourseRepositoryImpl(db, MyPostgresProfile, tables, executionContext: ExecutionContext)
}