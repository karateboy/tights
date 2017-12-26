package models
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

object SysConfig {
  val ColName = "sysConfig"
  val collection = MongoDB.database.getCollection(ColName)
  import org.mongodb.scala._

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

  import org.mongodb.scala.model._
  def upsert(_id: String, doc: Document) = {
    val uo = new UpdateOptions()
    val f = collection.replaceOne(Filters.equal("_id", _id), doc, uo.upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  val ColorSeqID = "ColorSeq"
  def getColorSeq() = {
    val configF = collection.find(Filters.equal("_id", ColorSeqID)).toFuture()
    configF.onFailure(errorHandler)
    for (config <- configF) yield {
      if (config.isEmpty) {
        waitReadyResult(initColorSeq)
      } else {
        implicit val doc = config.head
        val colorSeqOpt = getOptionArray(ColorSeqID, _.asString().getValue)
        colorSeqOpt.getOrElse(Seq.empty[String])
      }
    }
  }

  def setColorSeq(colorSeq: Seq[String]) = {
    val doc = Document(ColorSeqID -> colorSeq)
    upsert(ColorSeqID, doc)
  }

  def initColorSeq() = {
    var colorSet = Set.empty[String]
    val param = QueryOrderParam(None, None, None,
      None, None,
      None, None)

    val ordersF = Order.queryOrder(param)(0, 10000)
    val allColorSeqF =
      for (orders <- ordersF) yield {
        for {
          order <- orders
          detail <- order.details
        } {
          colorSet += detail.color
        }
        colorSet.toSeq
      }
    
    for (colorSeq <- allColorSeqF) yield {
      setColorSeq(colorSeq)
    }
    
    allColorSeqF
  }
}