import db.DbServiceImpl

object Main {

  def main(args: Array[String]): Unit = {
    val service = new DbServiceImpl()
    service.insertUser()
  }
}