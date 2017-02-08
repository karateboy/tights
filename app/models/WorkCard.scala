package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import org.mongodb.scala.bson._

case class StylingCard(operator: Seq[String], good: Int, sub: Option[Int],
                       stain: Option[Int], broken: Option[Int], notEven: Option[Int], var date: Long) {
  def toDocument = {
    Document("operator" -> operator, "good" -> good, "sub" -> sub,
      "stain" -> stain,
      "broken" -> broken, "notEven" -> notEven, "date" -> date)
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
    val stain = getOptionInt("stain")
    val broken = getOptionInt("broken")
    val notEven = getOptionInt("notEven")
    val date = doc.getLong("date")
    StylingCard(operator, good, sub, stain,
      broken, notEven, date)
  }
}

case class WorkCard(var _id: String, orderId: String, detailIndex: Int, quantity: Int, good: Int, active: Boolean,
                    startTime: Option[Long], endTime: Option[Long],
                    var dyeCardID: Option[String], tidyIDs: Option[Seq[Long]], stylingCard: Option[StylingCard]) {
  def toDocument = {
    Document("_id" -> _id,
      "orderId" -> orderId,
      "detailIndex" -> detailIndex,
      "quantity" -> quantity,
      "good" -> good,
      "active" -> active,
      "startTime" -> startTime,
      "endTime" -> endTime,
      "dyeCardID" -> dyeCardID,
      "tidyIDs" -> tidyIDs,
      "stylingCard" -> stylingCard)
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

    WorkCard(_id = _id,
      orderId = orderId,
      detailIndex = detailIndex,
      quantity = quantity,
      good = quantity,
      active = true,
      startTime = Some(DateTime.now.getMillis),
      endTime = None,
      dyeCardID = dyeCardID,
      tidyIDs = Some(Seq.empty[Long]),
      stylingCard = None)
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
    val tidyIDs = getOptionArray("tidyIDs", (v) => { v.asInt64().getValue })
    val stylingCard = getOptionDoc("stylingCard") map { StylingCard.toStylingCard(_) }

    WorkCard(_id = _id,
      orderId = orderId,
      detailIndex = detailIndex,
      quantity = quantity,
      good = good,
      active = active,
      startTime = startTime,
      endTime = endTime,
      dyeCardID = dyeCardID,
      tidyIDs = tidyIDs,
      stylingCard = stylingCard)
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
    collection.deleteOne(equal("_id", id)).toFuture()
  }

  def updateCard(card: WorkCard) = {
    val f = collection.replaceOne(equal("_id", card._id), card.toDocument).toFuture()
    waitReadyResult(f)
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

  def getCards(ids: Seq[String]) = {
    val f = collection.find(in("_id", ids: _*)).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield {
      cards map { toWorkCard(_) }
    }
  }

  def getActiveWorkCards() = {
    val f = collection.find(equal("active", true)).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
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
    val f = collection.updateOne(equal("_id", workCardID),
      Updates.combine(Updates.set("stylingCard", stylingCard.toDocument),
        Updates.min("good", stylingCard.good),
        Updates.set("active", stylingCard.good != 0),
        Updates.set("endTime", now))).toFuture()
    f.onFailure { errorHandler }
    f
  }

  def updateGoodAndActive(workCardID: String, good: Int, active: Boolean) = {
    import org.mongodb.scala.model.Updates
    val now = DateTime.now().getMillis
    val f = collection.updateOne(equal("_id", workCardID),
      Updates.combine(
        Updates.min("good", good),
        Updates.set("active", active),
        Updates.set("endTime", now))).toFuture()
    f.onFailure { errorHandler }
    f.onSuccess({
      case _ =>
        if (!active && good > 0) {
          val workCardF = WorkCard.getCard(workCardID)
          for (cardOpt <- workCardF) yield {
            val card = cardOpt.get
            checkOrderDetailComplete(card.orderId, card.detailIndex)
          }
        }
    })
    f
  }

  def getOrderWorkCards(orderId: String, detailIndex: Int) = {
    val f = collection.find(and(equal("orderId", orderId), equal("detailIndex", detailIndex))).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
  }

  case class QueryWorkCardParam(_id: Option[String], orderId: Option[String], start: Option[Long], end: Option[Long])
  def query(param: QueryWorkCardParam) = {
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

    val f = collection.find(filter).sort(ascending("orderId", "detailIndex")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc => toWorkCard(doc)
    }
  }
}
