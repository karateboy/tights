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

case class WorkCard(var _id: String, orderId: String, detailIndex: Int, quantity: Int,
                    startTime: Option[Long], endTime: Option[Long],
                    var dyeCardID: Option[String], finishedDyeCard: Option[DyeCard]) {
  def toDocument = {
    Document("_id" -> _id,
      "orderId" -> orderId,
      "detailIndex" -> detailIndex,
      "startTime" -> startTime,
      "endTime" -> endTime,
      "dyeCardID" -> dyeCardID,
      "finishedDyeCard" -> finishedDyeCard.map { _.toDocument },
      "quantity" -> quantity)
  }

  def updateID: Unit = {
    import java.util.concurrent.ThreadLocalRandom
    val randomNum = ThreadLocalRandom.current().nextInt(1, 1000000)
    val newID = "%06d".format(randomNum)
    val f = WorkCard.getCard(newID)
    val ret = waitReadyResult(f)

    if (ret.isEmpty)
      _id = newID
    else
      updateID
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

    val startTime = getOptionTime("startTime")
    val endTime = getOptionTime("endTime")
    val dyeCardID = getOptionStr("dyeCardID")
    val finishedDyeCard = getOptionDoc("finishedDyeCard") map { DyeCard.toDyeCard(_) }
    val quantity = doc.getInteger("quantity")
    WorkCard(_id, orderId, detailIndex, quantity,
      startTime, endTime, dyeCardID, finishedDyeCard)
  }

  def newCard(card: WorkCard) = {
    collection.insertOne(card.toDocument).toFuture()
  }

  def insertCards(cards : Seq[WorkCard]) = {
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
    val f = collection.find(in("_id", ids:_*)).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield {
      cards map { toWorkCard(_)}
    }    
  }
  
  def getActiveWorkCards() = {
    val f = collection.find().toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map { toWorkCard(_) }
  }
}
