package models
import models.ModelHelper._
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import play.api._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.util.{Failure, Success}

case class Inventory(factoryID: String, color: String, size: String, var quantity: Int,
                     var loan: Int, var workCardList: Seq[String], customerID: Option[String],
                     brand: Option[String])
case class QueryInventoryParam(factoryID: Option[String], color: Option[String], size: Option[String],
                               customerID: Option[String], brand: Option[String])

object Inventory {
  import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.mongodb.scala.bson.codecs.Macros._

  import scala.concurrent._

  val ColName = "inventory"
  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[Inventory]), DEFAULT_CODEC_REGISTRY)
  val collection: MongoCollection[Inventory] = MongoDB.database.getCollection[Inventory](ColName).withCodecRegistry(codecRegistry)

  implicit val reads: Reads[Inventory] = Json.reads[Inventory]
  implicit val write: Writes[Inventory] = Json.writes[Inventory]
  implicit val readQ: Reads[QueryInventoryParam] = Json.reads[QueryInventoryParam]

  def init(colNames: Seq[String]): Unit = {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f onComplete {
        case Success(value)=>
          val opt = new IndexOptions().unique(true)
          val cf1 = collection.createIndex(Indexes.ascending("factoryID", "color", "size"), opt).toFuture()
          cf1.onFailure(errorHandler)
        case Failure(exception)=>
          Logger.error("failed", exception)
      }
    }
  }

  import org.mongodb.scala.model._
  def upsert(inventory: Inventory): Future[UpdateResult] = {
    Logger.debug("upsert inventory=>" + inventory.toString)
    for(brand<-inventory.brand)
      SysConfig.addBrandList(Seq(brand.trim))

    val filter = getFilter(inventory.factoryID, inventory.color, inventory.size)
    val opt = ReplaceOptions().upsert(true)
    val f = collection.replaceOne(filter, inventory, opt).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def lend(factoryID: String, color: String, size: String, q: Int, workCardID: String): Future[Seq[UpdateResult]] = {
    Logger.debug(s"WorkCard:$workCardID inventory lend $factoryID $color $size $q")
    val inventoryFilter = getFilter(factoryID, color, size)
    val nonLoanFilter = Filters.or(Filters.equal("loan", null), Filters.exists("loan", exists = false))
    val loanExistFilter = Filters.exists("loan", exists = true)
    val filter1 = Filters.and(inventoryFilter, nonLoanFilter)
    val filter2 = Filters.and(inventoryFilter, loanExistFilter)

    val setUpdate = Updates.combine(
      Updates.set("loan", q),
      Updates.addToSet("workCardList", workCardID))

    val incUpdate = Updates.combine(
      Updates.inc("loan", q),
      Updates.addToSet("workCardList", workCardID))

    val fNonLoan = collection.updateOne(filter1, setUpdate).toFuture()
    fNonLoan onFailure errorHandler
    val fLoan = collection.updateOne(filter2, incUpdate).toFuture()
    fLoan onFailure errorHandler
    Future.sequence(Seq(fNonLoan, fLoan))
  }

  def freeWorkCardLoan(workCardID: String): Unit = {
    import com.mongodb.client.model.ReturnDocument.AFTER

    val f = collection.findOneAndUpdate(
      Filters.all("workCardList", workCardID),
      Updates.pull("workCardList", workCardID), FindOneAndUpdateOptions().returnDocument(AFTER)).toFuture()

    for {
      inventory <- f
      (newLoan, newWorkCardList) <- calculateLoan(inventory.workCardList)
    } {
      inventory.loan = newLoan
      inventory.workCardList = newWorkCardList
      collection.replaceOne(getFilter(inventory), inventory).toFuture()
    }
  }

  def closePosition(factoryID: String, color: String, size: String, q1: Int, workCardID: String): Future[Inventory] = {
    val filter = getFilter(factoryID, color, size)
    val update = Updates.combine(Updates.inc("quantity", -q1), Updates.pull("workCardList", workCardID))


    val f = collection.findOneAndUpdate(filter, update).toFuture()
    f.onFailure(errorHandler)
    f.onSuccess({
      case _ =>
        Logger.debug(s"closePosition ${factoryID} ${color} ${size} ${q1} ${workCardID}")
        refreshLoan(factoryID, color, size)
    })
    f
  }

  def refreshLoan(factoryID: String, color: String, size: String): Unit = {
    val filter = getFilter(factoryID, color, size)

    for {
      docSeq <- collection.find(filter).toFuture()
      doc <- docSeq
      inventory = doc
      (newLoan, newWorkCardList) <- calculateLoan(inventory.workCardList)
    } {
      if (inventory.loan != newLoan) {
        if (inventory.quantity < 0){
          Logger.warn(s"Inventory ${inventory.toString} quantity is negative!")
          inventory.quantity = 0
        }
        
        inventory.loan = newLoan
        inventory.workCardList = newWorkCardList
        collection.replaceOne(filter, inventory).toFuture()
      }
    }
  }

  private def calculateLoan(workCardIdList: Seq[String]): Future[(Int, Seq[String])] = {
    val workCardListF = WorkCard.getCards(workCardIdList)(0, 100)
    for (workCardList <- workCardListF) yield {
      val workCardIDs = workCardList map { _._id }
      val inventories = workCardList flatMap { _.inventory }
      (inventories.sum, workCardIDs)
    }
  }

  def canLend(factoryID: String, color: String, size: String): Future[Int] = {
    val filter = getFilter(factoryID, color, size)

    val f = collection.find(filter).toFuture()
    f.onFailure(errorHandler)
    for (docs <- f) yield {
      if (docs.isEmpty)
        0
      else {
        val inventory = docs.head
        inventory.quantity - inventory.loan
      }
    }
  }

  def getFilter(factoryID: String, color: String, size: String): Bson = {
    val filter1 = Filters.equal("factoryID", factoryID)
    val filter2 = Filters.equal("color", color)
    val filter3 = Filters.equal("size", size)
    Filters.and(filter1, filter2, filter3)
  }

  import org.mongodb.scala.bson.conversions._
  def getFilter(inv: Inventory): Bson = getFilter(inv.factoryID, inv.color, inv.size)

  def getFilter(param: QueryInventoryParam): Bson = {
    import org.mongodb.scala.model.Filters._

    import java.util.regex.Pattern
    val factoryIdFilter = param.factoryID map { factorID => regex("factoryID", Pattern.quote(factorID)) }
    val colorFilter = param.color map { color => regex("color", Pattern.quote(color)) }
    val sizeFilter = param.size map { equal("size", _) }
    val customerIdFilter = param.customerID map { customerID =>
      regex("customerID", Pattern.quote(customerID))
    }
    val brandFilter = param.brand map { brand =>
      equal("brand", brand)
    }

    val filterList = List(factoryIdFilter, colorFilter, sizeFilter, customerIdFilter, brandFilter).flatMap { f => f }
    val filter = if (filterList.nonEmpty)
      and(filterList: _*)
    else
      exists("_id")

    filter
  }

  def query(param: QueryInventoryParam)(skip: Int, limit: Int): Future[Seq[Inventory]] = {
    val sort = Sorts.ascending("customerID", "color", "size")
    val filter = getFilter(param)

    val f = collection.find(filter).skip(skip).limit(limit).sort(sort).toFuture()
    f.onFailure {
      errorHandler
    }

    for (records <- f)
      yield records map {
      doc => doc
    }
  }

  def count(param: QueryInventoryParam): Future[Long] = {
    val filter = getFilter(param)

    val f = collection.countDocuments(filter).toFuture()
    f.onFailure {
      errorHandler
    }

    for (countSeq <- f)
      yield countSeq
  }

  def total(param: QueryInventoryParam): Future[Int] = {
    val filter = getFilter(param)

    val f = collection.find(filter).toFuture()
    f.onFailure {
      errorHandler
    }

    for (docs <- f) yield
      docs.map(_.quantity).sum

  }

  def delete(param: QueryInventoryParam): Future[DeleteResult] = {
    val filter = getFilter(param)
    val f = collection.deleteOne(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }
  
  def fixNullInventory(): Future[UpdateResult] = {
    val filter = Filters.not(Filters.exists("quantity"))
    val f = collection.updateMany(filter, Updates.set("quantity", 0)).toFuture()
    f.onFailure(errorHandler)
    f
  }
}
