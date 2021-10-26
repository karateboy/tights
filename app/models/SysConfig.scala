package models
import play.api._
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

  }

  import org.mongodb.scala.model._
  def upsert(_id: String, doc: Document) = {
    val uo = new ReplaceOptions().upsert(true)
    val f = collection.replaceOne(Filters.equal("_id", _id), doc, uo).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def get(_id: String, defaultDoc: Document) = {
    val f = collection.find(Filters.equal("_id", _id)).toFuture()
    f.onFailure(errorHandler)
    for (docs <- f) yield {
      if (docs.length == 0) {
        upsert(_id, defaultDoc)
        defaultDoc(_id)
      } else {
        docs.head(_id)
      }
    }
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
    val doc = Document("_id" -> ColorSeqID, ColorSeqID -> colorSeq)
    upsert(ColorSeqID, doc)
  }

  def addColorSeq(colorSeq: Seq[String]) =
    collection.updateOne(
      Filters.equal("_id", ColorSeqID),
      Updates.addEachToSet(ColorSeqID, colorSeq: _*)).toFuture()

  def delColorSeq(colorSeq: Seq[String]) = {
    Logger.debug(colorSeq.toString)
    collection.updateOne(
      Filters.equal("_id", ColorSeqID),
      Updates.pullAll(ColorSeqID, colorSeq: _*)).toFuture()
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

  val BrandList = "BrandList"
  def getBrandList() = {
    val configF = collection.find(Filters.equal("_id", BrandList)).toFuture()
    configF.onFailure(errorHandler)
    for (config <- configF) yield {
      if (config.isEmpty) {
        Seq.empty[String]
      } else {
        implicit val doc = config.head
        val brandListOpt = getOptionArray(BrandList, _.asString().getValue)
        brandListOpt.getOrElse(Seq.empty[String])
      }
    }
  }
  def addBrandList(brands: Seq[String]) =
    collection.updateOne(
      Filters.equal("_id", BrandList),
      Updates.addEachToSet(BrandList, brands: _*), UpdateOptions().upsert(true)).toFuture()

  val TrimOrderKey = "TrimOrder"
  def getTrimOrderConfig = get(TrimOrderKey, Document(TrimOrderKey -> false))
  def setTrimOrderConfig(v: Boolean) = upsert(TrimOrderKey, Document(TrimOrderKey -> v))

  val TrimColorSeqKey = "TrimColorSeq"
  def getTrimColorSeq = get(TrimColorSeqKey, Document(TrimColorSeqKey -> false))
  def setTrimColorSeq(v: Boolean) = upsert(TrimColorSeqKey, Document(TrimColorSeqKey -> v))
  def trimColorSeq = {
    val colorSeqF = getColorSeq
    val trimSetF =
      for (colorSeq <- colorSeqF) yield {
        val trimSeq =
          colorSeq map { _.trim }

        Set(trimSeq: _*)
      }

    for (trimSet <- trimSetF) {
      val f =
        setColorSeq(trimSet.toSeq)

      f.onComplete { case _ => Logger.info("ColorSeq has been trimmed!") }
    }
  }

  val FixNullInventoryKey = "FixNullInventory"
  def getFixNullInventory = get(FixNullInventoryKey, Document(FixNullInventoryKey -> false))
  def setFixNullInventory(v: Boolean) = upsert(FixNullInventoryKey, Document(FixNullInventoryKey -> v))
}