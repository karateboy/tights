package models

import models.ModelHelper._
import org.mongodb.scala.model.ReplaceOptions
import play.api._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

case class TidyID(workCardID: String, phase: String)

case class TidyCard(_id: TidyID, workCardID: String, phase: String, operator: String, good: Int,
                    sub: Option[Int], subNotPack: Option[Int], stain: Option[Int], longShort: Option[Int],
                    broken: Option[Int], notEven: Option[Int], oil: Option[Int], head: Option[Int],
                    var date: Long, stylingDate: Option[Long], finishDate: Option[Long])


object TidyCard {

  import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
  import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.model.Indexes.ascending

  val ColName = "tidyCards"
  val codecRegistry = fromRegistries(fromProviders(classOf[TidyCard], classOf[TidyID]), DEFAULT_CODEC_REGISTRY)
  val collection = MongoDB.database.getCollection[TidyCard](ColName).withCodecRegistry(codecRegistry)

  def init(colNames: Seq[String]) {
    import org.mongodb.scala.model.IndexOptions
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f onComplete {
        case Success(value) =>
          collection.createIndex(ascending("workCardID", "phase"), IndexOptions().unique(true)).toFuture()
        case Failure(exception) =>
          Logger.error("failed", exception)
      }
    }
  }

  def default(workCardID: String, phase: String, stylingDate: Option[Long]) =
    TidyCard(TidyID(workCardID, phase), workCardID, phase, "", 0, None, None, None,
      None, None, None, None, None, 0, stylingDate = stylingDate, finishDate = None)

  implicit val idRead = Json.reads[TidyID]
  implicit val idWrite = Json.writes[TidyID]
  implicit val read = Json.reads[TidyCard]
  implicit val write = Json.writes[TidyCard]

  def newCard(card: TidyCard) = {
    val f = collection.insertOne(card).toFuture()
    f onFailure (errorHandler)
    f
  }

  def upsertCard(card: TidyCard, inventory: Int, quantity: Int, active: Boolean) = {
    import org.mongodb.scala.model.Filters._
    val workCardF =
      WorkCard.updateGoodAndActive(card.workCardID, card.good, inventory, quantity,
        active && (card.good + inventory) != 0, card.phase == "整理包裝")
    workCardF.onFailure {
      errorHandler
    }

    val f = collection.replaceOne(equal("_id", card._id), card, ReplaceOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def queryCards(begin: Long, end: Long): Future[Seq[TidyCard]] = {
    import org.mongodb.scala.model.Filters._

    val f = collection.find(and(gte("date", begin), lt("date", end)))
      .sort(org.mongodb.scala.model.Sorts.ascending("date")).limit(500).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def queryCardsByStylingDate(begin: Long, end: Long): Future[Seq[TidyCard]] = {
    import org.mongodb.scala.model.Filters._

    val f = collection.find(and(gte("stylingDate", begin), lt("stylingDate", end)))
      .sort(org.mongodb.scala.model.Sorts.ascending("stylingDate")).limit(500).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }

  def getTidyCard(workCardID: String, phase: String): Future[Seq[TidyCard]] = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(and(equal("workCardID", workCardID), equal("phase", phase))).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield
        records
  }


  def getTidyCardOfWorkCard(workCardID: String) = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(equal("workCardID", workCardID)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records
  }
  import org.mongodb.scala.model._
  def updateStylingDateByWorkCard(workCardID: String, stylingDate:Long) = {
    val f = collection.updateMany(Filters.equal("workCardID", workCardID),
      Updates.set("stylingDate", stylingDate)).toFuture()
    f onFailure errorHandler
    f
  }
}