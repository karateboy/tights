package models

import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import org.mongodb.scala.model._
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class StylingCard(operator: Seq[String], good: Int,
                       sub: Option[Int], subNotPack: Option[Int], stain: Option[Int], longShort: Option[Int],
                       broken: Option[Int], notEven: Option[Int], oil: Option[Int], head: Option[Int],
                       var date: Long, stylingDate: Option[Long]) {

  def total: Int = good + sub.getOrElse(0) + subNotPack.getOrElse(0) + stain.getOrElse(0) + longShort.getOrElse(0)

  +broken.getOrElse(0) + notEven.getOrElse(0) + oil.getOrElse(0) + head.getOrElse(0)
}

object StylingCard {
  implicit val read = Json.reads[StylingCard]
  implicit val write = Json.writes[StylingCard]
}

case class WorkCard(var _id: String, orderId: String, detailIndex: Int, quantity: Int, good: Int, active: Boolean,
                    startTime: Option[Long], endTime: Option[Long],
                    var dyeCardID: Option[String], stylingCard: Option[StylingCard],
                    var remark: Option[String], inventory: Option[Int],
                    stylingDate: Option[Long],
                    var order:Option[Order]=None) {

  def updateID(): Unit = {
    //import java.util.concurrent.ThreadLocalRandom
    //val randomNum = ThreadLocalRandom.current().nextInt(1, 1000000)
    val idF = Identity.getNewID("workCard")
    val id: Identity = waitReadyResult(idF)
    val newID = "%06d".format(id.seq)

    val f = WorkCard.getCard(newID)
    val ret = waitReadyResult(f)

    if (ret.isEmpty)
      _id = newID
    else
      updateID
  }

  def init: WorkCard = {
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
      inventory = inventory,
      stylingDate = None)
  }
}

object WorkCard {

  import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.model.Indexes.ascending

  val ColName = "workCards"
  val codecRegistry = fromRegistries(fromProviders(classOf[WorkCard], classOf[StylingCard]), DEFAULT_CODEC_REGISTRY)
  val collection = MongoDB.database.getCollection[WorkCard](ColName).withCodecRegistry(codecRegistry)

  // val collection = MongoDB.database.getCollection(ColName)
  implicit val workRead = Json.reads[WorkCard]
  implicit val workWrite = Json.writes[WorkCard]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f onComplete {
        case Success(value) =>
          val cf2 = collection.createIndex(ascending("orderId", "detailIndex")).toFuture()
        case Failure(exception) =>
          Logger.error("failed", exception)
      }
    }
  }

  def newCard(card: WorkCard) = {
    collection.insertOne(card).toFuture()
  }

  def insertCards(cards: Seq[WorkCard]) = {
    collection.insertMany(cards).toFuture()
  }

  import org.mongodb.scala.model.Filters._

  def deleteCard(id: String) = {
    Inventory.freeWorkCardLoan(id)
    collection.deleteOne(equal("_id", id)).toFuture()
  }

  def updateCard(card: WorkCard) = {
    collection.replaceOne(equal("_id", card._id), card).toFuture()
  }

  def updateDyeCardId(_id: String, newDyeCardId: String) = {
    import org.mongodb.scala.model._
    collection.updateOne(equal("_id", _id), Updates.set("dyeCardID", newDyeCardId)).toFuture()
  }

  def countCards(ids: Seq[String]): Future[Long] = {
    val f = collection.countDocuments(in("_id", ids: _*)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (countSeq <- f) yield countSeq
  }

  def getCards(ids: Seq[String])(skip: Int, limit: Int): Future[Seq[WorkCard]] = {
    val f = collection.find(in("_id", ids: _*)).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (cards <- f) yield
      cards
  }

  def getActiveWorkCard(skip: Int, limit: Int): Future[Seq[WorkCard]] = {
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (cards <- f) yield cards
  }

  def getActiveWorkCardCount(): Future[Long] = {
    val f = collection.countDocuments(equal("active", true)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (counts <- f) yield counts
  }

  def checkOrderDetailComplete(orderId: String, detailIndex: Int) {
    val f = getOrderWorkCards(orderId, detailIndex)
    val orderF = Order.getOrder(orderId)
    for {
      cards <- f
      orderOpt <- orderF
      order = orderOpt.get
    } yield {
      val finishedCards = cards.filter {
        !_.active
      }
      val finishedGood = finishedCards.map {
        _.good
      }
      val finished = finishedGood.sum
      if (finished >= order.details(detailIndex).quantity)
        Order.setOrderDetailComplete(orderId, detailIndex, true)
    }
  }

  def updateStylingCard(workCardID: String, stylingCard: StylingCard) = {
    val now = DateTime.now().getMillis
    val update1 = Updates.combine(
      Updates.set("stylingCard", stylingCard),
      Updates.min("good", stylingCard.good),
      Updates.set("quantity", stylingCard.total),
      Updates.set("active", stylingCard.good != 0),
      Updates.set("endTime", now))
    val update = if (stylingCard.stylingDate.nonEmpty) {
      TidyCard.updateStylingDateByWorkCard(workCardID, stylingCard.stylingDate.get)
      Updates.combine(update1, Updates.set("stylingDate", stylingCard.stylingDate.get))
    } else
      update1

    val f = collection.updateOne(
      equal("_id", workCardID), update).toFuture()
    f.onFailure {
      errorHandler
    }
    f
  }

  def updateGoodAndActive(workCardID: String, good: Int, inventory: Int, quantity: Int,
                          active: Boolean, overWrite: Boolean = false) = {
    import org.mongodb.scala.model.Updates
    val workCardF = WorkCard.getCard(workCardID)
    var refreshInventory = false
    val minGoodFF =
      for (workCardOpt <- workCardF) yield {
        val workCard = workCardOpt.get
        if (workCard.inventory != Some(inventory))
          refreshInventory = true

        if (overWrite) {
          Future {
            good
          }
        } else {
          if (workCard.good >= good) {
            Future {
              good
            }
          } else {
            val tidyCardsF = TidyCard.getTidyCardOfWorkCard(workCardID)
            for (tidyCards <- tidyCardsF) yield {
              val goodSeq = tidyCards map {
                _.good
              }
              goodSeq.foldLeft(good)(Math.min)
            }
          }
        }
      }
    val minGoodF = minGoodFF.flatMap { x => x }

    val retFF =
      for (minGood <- minGoodF) yield {
        val now = DateTime.now().getMillis
        val updateList = List(
          Updates.set("good", minGood),
          Updates.set("active", active),
          Updates.set("inventory", inventory),
          Updates.set("endTime", now))
        val updates =
          if (!overWrite)
            Updates.combine(updateList: _*)
          else
            Updates.combine(updateList.:+(Updates.set("quantity", quantity)): _*)
        val f = collection.updateOne(
          equal("_id", workCardID), updates).toFuture()
        f.onFailure {
          errorHandler
        }
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
                    val finishedCards = cards.filter {
                      !_.active
                    }
                    val finishedGood = finishedCards.map {
                      _.good
                    }
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

  def getCard(id: String): Future[Option[WorkCard]] = {
    val f = collection.find(equal("_id", id)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (cards <- f) yield {
      if (cards.isEmpty)
        None
      else
        Some(cards(0))
    }
  }

  def getOrderWorkCards(orderId: String, detailIndex: Int): Future[Seq[WorkCard]] = {
    val f = collection.find(and(equal("orderId", orderId), equal("detailIndex", detailIndex))).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (cards <- f) yield cards
  }

  def getOrderWorkCardShipment(orderId: String, detailIndex: Int): Future[Seq[Int]] = {
    import org.mongodb.scala.model._
    val f = collection.find(and(equal("orderId", orderId), equal("detailIndex", detailIndex))).toFuture()
    f.onFailure {
      errorHandler
    }
    for (shipment <- f) yield shipment.map {
      doc =>
        val inventory = doc.inventory.getOrElse(0)
        val good = doc.good
        good + inventory
    }
  }

  def getOrderProductionWorkCards(orderId: String): Future[Seq[WorkCard]] = {
    val f = collection.find(equal("orderId", orderId)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (cards <- f) yield cards
  }

  def query(param: QueryWorkCardParam)(skip: Int, limit: Int): Future[Seq[WorkCard]] = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val orderIdFilter = param.orderId map { orderId => regex("orderId", orderId) }
    val startFilter = param.start map {
      gte("startTime", _)
    }
    val endFilter = param.end map {
      lt("startTime", _)
    }

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
      yield records
  }

  def count(param: QueryWorkCardParam): Future[Long] = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val orderIdFilter = param.orderId map { orderId => regex("orderId", orderId) }
    val startFilter = param.start map {
      gte("startTime", _)
    }
    val endFilter = param.end map {
      lt("startTime", _)
    }

    val filterList = List(idFilter, orderIdFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.countDocuments(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (countSeq <- f)
      yield countSeq
  }

  def queryStylingCard(start: Long, end: Long): Future[Seq[WorkCard]] = {
    import org.mongodb.scala.model.Filters._

    val filter = and(gte("stylingCard.date", start), lt("stylingCard.date", end))
    val f = collection.find(filter).sort(ascending("stylingCard.date")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def queryStylingCardByStylingDate(start: Long, end: Long): Future[Seq[WorkCard]] = {
    import org.mongodb.scala.model.Filters._

    val filter = and(gte("stylingDate", start), lt("stylingDate", end))
    val f = collection.find(filter).sort(ascending("stylingCard.date")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  case class QueryWorkCardParam(_id: Option[String], orderId: Option[String], start: Option[Long], end: Option[Long])
}
