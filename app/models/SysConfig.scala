package models
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class SysConfig(color: Seq[String])
object SysConfig {
  val ColName = "sysConfig"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val userRead = Json.reads[SysConfig]
  implicit val userWrite = Json.writes[SysConfig]
  import org.mongodb.scala._
  
  def toDocument(config: SysConfig) = Document(Json.toJson(config).toString())

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }
    
    val f = collection.count().toFuture()
    f.onSuccess({
      case count: Seq[Long] =>
        if (count(0) == 0) {
        }
    })
    f.onFailure(errorHandler)
  }
}