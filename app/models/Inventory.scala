package models
import play.api._
import models.ModelHelper._
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class Inventory(factoryID: String, color: String, size: String, quantity: Int,
                     var loan: Option[Int], var workCardList: Option[Seq[String]])
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

  import org.mongodb.scala.model._
  def upsert(inventory: Inventory) = {
    val filter = getFilter(inventory.factoryID, inventory.color, inventory.size)
    val opt = UpdateOptions().upsert(true)
    val f = collection.replaceOne(filter, toDocument(inventory)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def lend(factoryID: String, color: String, size: String, q: Int, workCardID: String) = {
    val filter = getFilter(factoryID, color, size)
    val update = Updates.combine(
      Updates.inc("loan", q),
      Updates.addToSet("workCardList", workCardID))

    val f = collection.updateOne(filter, update).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def freeWorkCardLoan(workCardID: String) = {
    val f = collection.findOneAndUpdate(Filters.eq("workCardList", workCardID),
      Updates.pull("workCardList", workCardID)).toFuture()

    for {
      updatedDocs <- f if !updatedDocs.isEmpty
      inventory = toInventory(updatedDocs.head)
      workCardListOpt = inventory.workCardList if workCardListOpt.isDefined
      workCardList = workCardListOpt.get
      (newLoan, newWorkCardList) <- recalculateLoan(workCardList)
    } {
      inventory.loan = Some(newLoan)
      inventory.workCardList = Some(newWorkCardList)
      val filter = getFilter(inventory)
      collection.replaceOne(filter, toDocument(inventory)).toFuture()
    }
    
    f
  }

  def closePosition(factoryID: String, color: String, size: String, q1: Int, workCardID: String) = {
    val filter = getFilter(factoryID, color, size)

    val update = Updates.combine(Updates.inc("quantity", -q1), Updates.pull("workCardList", workCardID))

    val f = collection.findOneAndUpdate(filter, update).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case _ =>
        refreshLoan(factoryID, color, size)
    })
    f
  }

  def refreshLoan(factoryID: String, color: String, size: String) = {
    val filter = getFilter(factoryID, color, size)

    for {
      docSeq <- collection.find(filter).toFuture()
      doc <- docSeq
      inventory = toInventory(doc)
      workCardList <- inventory.workCardList
      (newLoan, newWorkCardList) <- recalculateLoan(workCardList)
    } {
      if (inventory.loan != Some(newLoan)) {
        inventory.loan = Some(newLoan)
        inventory.workCardList = Some(newWorkCardList)
        collection.replaceOne(filter, toDocument(inventory)).toFuture()
      }
    }
  }

  def recalculateLoan(workCardIdList: Seq[String]) = {
    val workCardListF = WorkCard.getCards(workCardIdList)(0, 100)
    for (workCardList <- workCardListF) yield {
      val activeWorkCard = workCardList.filter { workCard => workCard.active }
      val activeWorkCardIDs = activeWorkCard map { _._id }
      val inventories = activeWorkCard flatMap { _.inventory }
      (inventories.sum, activeWorkCardIDs)
    }
  }

  def canLend(factoryID: String, color: String, size: String) = {
    val filter = getFilter(factoryID, color, size)

    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for (docs <- f) yield {
      if (docs.isEmpty)
        0
      else {
        val inventory = toInventory(docs.head)
        inventory.quantity - inventory.loan.getOrElse(0)
      }
    }
  }

  def getFilter(factoryID: String, color: String, size: String) = {
    val filter1 = Filters.equal("factoryID", factoryID)
    val filter2 = Filters.equal("color", color)
    val filter3 = Filters.equal("size", size)
    Filters.and(filter1, filter2, filter3)
  }

  import org.mongodb.scala.bson.conversions._
  def getFilter(inv: Inventory): Bson = getFilter(inv.factoryID, inv.color, inv.size)

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

    filter
  }

  def query(param: QueryInventoryParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val filter = getFilter(param)

    val f = collection.find(filter).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }

    for (records <- f)
      yield records map {
      doc => toInventory(doc)
    }
  }

  def count(param: QueryInventoryParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val filter = getFilter(param)

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }

    for (countSeq <- f)
      yield countSeq(0)
  }
}
