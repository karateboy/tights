package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
//import scala.language.implicitConversions
import org.mongodb.scala.bson._

case class StylingCard(operator: Seq[String], good: Int,
                       sub: Option[Int], subNotPack: Option[Int], stain: Option[Int], longShort: Option[Int],
                       broken: Option[Int], notEven: Option[Int], oil: Option[Int], head: Option[Int],
                       var date: Long) {
  def toDocument = {
    Document("operator" -> operator, "good" -> good, "sub" -> sub, "subNotPack" -> subNotPack, "stain" -> stain, "longShort" -> longShort,
      "broken" -> broken, "notEven" -> notEven, "oil" -> oil, "head" -> head,
      "date" -> date)
  }
}
object StylingCard {
  implicit val read = Json.reads[StylingCard]
  implicit val write = Json.writes[StylingCard]

  implicit object TransformStylingCard extends BsonTransformer[StylingCard] {
    def apply(sc: StylingCard): BsonDocument = sc.toDocument.toBsonDocument
  }

  def toStylingCard(implicit doc: Document) = {
    val operator = getArray("operator", (v) => { v.asString().getValue })
    val good = doc.getInteger("good")
    val sub = getOptionInt("sub")
    val subNotPack = getOptionInt("subNotPack")
    val stain = getOptionInt("stain")
    val longShort = getOptionInt("longShort")
    val broken = getOptionInt("broken")
    val oil = getOptionInt("oil")
    val notEven = getOptionInt("notEven")
    val head = getOptionInt("head")
    val date = doc.getLong("date")
    StylingCard(
      operator = operator,
      good = good,
      sub = sub,
      subNotPack = subNotPack,
      stain = stain,
      longShort = longShort,
      broken = broken,
      notEven = notEven,
      oil = oil,
      head = head,
      date = date)
  }
}

case class WorkCard(var _id: String, orderId: String, detailIndex: Int, quantity: Int, good: Int, active: Boolean,
                    startTime: Option[Long], endTime: Option[Long],
                    var dyeCardID: Option[String], stylingCard: Option[StylingCard],
                    var remark: Option[String], inventory: Option[Int]) {
  def toDocument = {
    Document(
      "_id" -> _id,
      "orderId" -> orderId,
      "detailIndex" -> detailIndex,
      "quantity" -> quantity,
      "good" -> good,
      "active" -> active,
      "startTime" -> startTime,
      "endTime" -> endTime,
      "dyeCardID" -> dyeCardID,
      "stylingCard" -> stylingCard,
      "remark" -> remark,
      "inventory" -> inventory)
  }

  def updateID: Unit = {
    //import java.util.concurrent.ThreadLocalRandom
    //val randomNum = ThreadLocalRandom.current().nextInt(1, 1000000)
    val idF = Identity.getNewID("workCard")
    val id = waitReadyResult(idF)
    val newID = "%06d".format(id.seq)

    val f = WorkCard.getCard(newID)
    val ret = waitReadyResult(f)

    if (ret.isEmpty)
      _id = newID
    else
      updateID
  }

  def init = {
    if (_id == "")
      updateID

    WorkCard(
      _id = _id,
      orderId = orderId,
      detailIndex = detailIndex,
      quantity = quantity,
      good = quantity,
      active = true,
      startTime = Some(DateTime.now.getMillis),
      endTime = None,
      dyeCardID = dyeCardID,
      stylingCard = stylingCard,
      remark = remark,
      inventory = inventory)
  }
}

object WorkCard {
  import DyeCard._
  import org.mongodb.scala.model.Indexes.ascending

  val ColName = "workCards"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val workRead = Json.reads[WorkCard]
  implicit val workWrite = Json.writes[WorkCard]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>
          val cf2 = collection.createIndex(ascending("orderId", "detailIndex")).toFuture()
      })
    }
  }

  def toWorkCard(implicit doc: Document) = {

    val _id = doc.getString("_id")
    val orderId = doc.getString("orderId")
    val detailIndex = doc.getInteger("detailIndex")
    val quantity = doc.getInteger("quantity")
    val good = doc.getInteger("good")
    val active = doc.getBoolean("active")
    val startTime = getOptionTime("startTime")
    val endTime = getOptionTime("endTime")
    val dyeCardID = getOptionStr("dyeCardID")
    val stylingCard = getOptionDoc("stylingCard") map { StylingCard.toStylingCard(_) }
    val remark = getOptionStr("remark")
    val inventory = getOptionInt("inventory")

    WorkCard(
      _id = _id,
      orderId = orderId,
      detailIndex = detailIndex,
      quantity = quantity,
      good = good,
      active = active,
      startTime = startTime,
      endTime = endTime,
      dyeCardID = dyeCardID,
      stylingCard = stylingCard,
      remark = remark,
      inventory = inventory)
  }

  def newCard(card: WorkCard) = {
    collection.insertOne(card.toDocument).toFuture()
  }

  def insertCards(cards: Seq[WorkCard]) = {
    val docs = cards map { _.toDocument }
    collection.insertMany(docs).toFuture()
  }

  import org.mongodb.scala.model.Filters._
  def deleteCard(id: String) = {
    Inventory.freeWorkCardLoan(id)
    collection.deleteOne(equal("_id", id)).toFuture()
  }

  def updateCard(card: WorkCard) = {
    collection.replaceOne(equal("_id", card._id), card.toDocument).toFuture()
  }

  def updateDyeCardId(_id: String, newDyeCardId: String) = {
    import org.mongodb.scala.model._
    collection.updateOne(equal("_id", _id), Updates.set("dyeCardID", newDyeCardId)).toFuture()
  }

  def getCard(id: String) = {
    val f = collection.find(equal("_id", id)).first().toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield {
      if (cards.length == 0)
        None
      else
        Some(toWorkCard(cards(0)))
    }
  }

  def countCards(ids: Seq[String]) = {
    import org.mongodb.scala.model._
    val f = collection.count(in("_id", ids: _*)).toFuture()
    f.onFailure { errorHandler }
    for (countSeq <- f) yield countSeq(0)
  }

  def getCards(ids: Seq[String])(skip: Int, limit: Int) = {
    import org.mongodb.scala.model._
    val f = collection.find(in("_id", ids: _*)).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield {
      cards map { toWorkCard(_) }
    }
  }

  def getActiveWorkCard(skip: Int, limit: Int) = {
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
  }

  def getActiveWorkCardCount() = {
    val f = collection.count(equal("active", true)).toFuture()
    f.onFailure { errorHandler }
    for (counts <- f) yield counts(0)
  }

  def checkOrderDetailComplete(orderId: String, detailIndex: Int) {
    val f = getOrderWorkCards(orderId, detailIndex)
    val orderF = Order.getOrder(orderId)
    for {
      cards <- f
      orderOpt <- orderF
      order = orderOpt.get
    } yield {
      val finishedCards = cards.filter { !_.active }
      val finishedGood = finishedCards.map { _.good }
      val finished = finishedGood.sum
      if (finished >= order.details(detailIndex).quantity)
        Order.setOrderDetailComplete(orderId, detailIndex, true)
    }
  }

  def updateStylingCard(workCardID: String, stylingCard: StylingCard) = {
    import org.mongodb.scala.model.Updates
    val now = DateTime.now().getMillis
    val f = collection.updateOne(
      equal("_id", workCardID),
      Updates.combine(
        Updates.set("stylingCard", stylingCard.toDocument),
        Updates.min("good", stylingCard.good),
        Updates.set("active", stylingCard.good != 0),
        Updates.set("endTime", now))).toFuture()
    f.onFailure { errorHandler }
    f
  }

  def updateGoodAndActive(workCardID: String, good: Int, inventory: Int, active: Boolean, overWrite: Boolean = false) = {
    import org.mongodb.scala.model.Updates
    val workCardF = WorkCard.getCard(workCardID)
    var refreshInventory = false
    val minGoodFF =
      for (workCardOpt <- workCardF) yield {
        val workCard = workCardOpt.get
        if (workCard.inventory != Some(inventory))
          refreshInventory = true

        if (overWrite) {
          Future { good }
        } else {
          if (workCard.good >= good) {
            Future { good }
          } else {
            val tidyCardsF = TidyCard.getTidyCardOfWorkCard(workCardID)
            for (tidyCards <- tidyCardsF) yield {
              val goodSeq = tidyCards map { _.good }
              goodSeq.foldLeft(good)(Math.min)
            }
          }
        }
      }
    val minGoodF = minGoodFF.flatMap { x => x }

    val retFF =
      for (minGood <- minGoodF) yield {
        val now = DateTime.now().getMillis
        val f = collection.updateOne(
          equal("_id", workCardID),
          Updates.combine(
            Updates.set("good", minGood),
            Updates.set("active", active),
            Updates.set("inventory", inventory),
            Updates.set("endTime", now))).toFuture()
        f.onFailure { errorHandler }
        f.onSuccess({
          case _ =>
            for {
              workCardOpt <- WorkCard.getCard(workCardID)
              workCard <- workCardOpt
              orderOptF = Order.getOrder(workCard.orderId)
              orderOpt <- orderOptF
              order <- orderOpt
              detail = order.details(workCard.detailIndex)
            } {
              if (active) {
                if (refreshInventory) {
                  Inventory.refreshLoan(order.factoryId, detail.color, detail.size)
                }
              } else {
                Inventory.closePosition(order.factoryId, detail.color, detail.size, inventory, workCardID)
                if (good > 0) {
                  for {
                    cards <- getOrderWorkCards(workCard.orderId, workCard.detailIndex)
                  } yield {
                    val finishedCards = cards.filter { !_.active }
                    val finishedGood = finishedCards.map { _.good }
                    val finished = finishedGood.sum
                    if (finished >= order.details(workCard.detailIndex).quantity)
                      Order.setOrderDetailComplete(workCard.orderId, workCard.detailIndex, true)
                  }
                }
              }
            }
        })
        f
      }
    retFF.flatMap { x => x }
  }

  def getOrderWorkCards(orderId: String, detailIndex: Int) = {
    val f = collection.find(and(equal("orderId", orderId), equal("detailIndex", detailIndex))).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
  }

  def getOrderWorkCardShipment(orderId: String, detailIndex: Int) = {
    import org.mongodb.scala.model._
    val f = collection.find(and(equal("orderId", orderId), equal("detailIndex", detailIndex))).projection(Projections.include("good", "inventory")).toFuture()
    f.onFailure { errorHandler }
    for (shipment <- f) yield shipment.map {
      doc =>
        val inventory = doc.getInteger("inventory", 0)
        val good = doc.getInteger("good", 0)
        good + inventory
    }
  }

  def getOrderProductionWorkCards(orderId: String) = {
    val f = collection.find(equal("orderId", orderId)).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
  }

  case class QueryWorkCardParam(_id: Option[String], orderId: Option[String], start: Option[Long], end: Option[Long])
  def query(param: QueryWorkCardParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val orderIdFilter = param.orderId map { orderId => regex("orderId", orderId) }
    val startFilter = param.start map { gte("startTime", _) }
    val endFilter = param.end map { lt("startTime", _) }

    val filterList = List(idFilter, orderIdFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.find(filter).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc => toWorkCard(doc)
    }
  }

  def count(param: QueryWorkCardParam) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val orderIdFilter = param.orderId map { orderId => regex("orderId", orderId) }
    val startFilter = param.start map { gte("startTime", _) }
    val endFilter = param.end map { lt("startTime", _) }

    val filterList = List(idFilter, orderIdFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.count(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (countSeq <- f)
      yield countSeq(0)
  }

  def queryStylingCard(start: Long, end: Long) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val filter = and(gte("stylingCard.date", start), lt("stylingCard.date", end))
    val f = collection.find(filter).sort(ascending("stylingCard.date")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc => toWorkCard(doc)
    }
  }
}
