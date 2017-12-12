package models
import play.api._
import models.ModelHelper._
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class Inventory(factoryID: String, color: String, size: String, quantity: Int)
case class QueryInventoryParam(factoryID: Option[String], color: Option[String], size: Option[String])

object Inventory {
  import scala.concurrent._
  import scala.concurrent.duration._

  val ColName = "inventory"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val reads = Json.reads[Inventory]
  implicit val write = Json.writes[Inventory]
  implicit val readQ = Json.reads[QueryInventoryParam]

  def toDocument(inv: Inventory) = Document(Json.toJson(inv).toString())

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>
          val opt = new IndexOptions().unique(true)
          val cf1 = collection.createIndex(Indexes.ascending("factoryID", "color", "size"), opt).toFuture()
          cf1.onFailure(errorHandler)
      })
    }
  }

  def toInventory(doc: Document) = {
    val ret = Json.parse(doc.toJson()).validate[Inventory]

    ret.fold(error => {
      Logger.error(JsError.toJson(error).toString())
      throw new Exception(JsError.toJson(error).toString)
    },
      id => id)
  }

  def upsert(inventory: Inventory) = {
    import org.mongodb.scala.model._
    val filter1 = Filters.equal("factoryID", inventory.factoryID)
    val filter2 = Filters.equal("color", inventory.color)
    val filter3 = Filters.equal("size", inventory.size)
    val filter = Filters.and(filter1, filter2, filter3)
    val opt = UpdateOptions().upsert(true)
    val f = collection.updateOne(filter, Updates.set("quantity", inventory.quantity), opt).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def getFilter(param: QueryInventoryParam) = {
    import org.mongodb.scala.model.Filters._
    val factoryIdFilter = param.factoryID map { factorID => regex("factoryID", "(?i)" + factorID) }
    val colorFilter = param.color map { color => regex("color", "(?i)" + color) }
    val sizeFilter = param.size map { equal("size", _) }
    val filterList = List(factoryIdFilter, colorFilter, sizeFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      exists("_id")

    import scala.concurrent._
    Future {
      filter
    }
  }

  def query(param: QueryInventoryParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val filterFuture = getFilter(param)

    val docF = filterFuture flatMap {
      filter =>
        val f = collection.find(filter).skip(skip).limit(limit).toFuture()
        f.onFailure {
          errorHandler
        }
        f
    }

    for (records <- docF)
      yield records map {
      doc => toInventory(doc)
    }
  }

  def count(param: QueryInventoryParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val filterFuture = getFilter(param)

    val retF = filterFuture flatMap {
      filter =>
        val f = collection.count(filter).toFuture()
        f.onFailure {
          errorHandler
        }
        f
    }

    for (countSeq <- retF)
      yield countSeq(0)
  }
}
