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
case class TidyID(workCardID: String, phase: String) {
  def toDocument = Document("workCardID" -> workCardID, "phase" -> phase)
}
case class TidyCard(_id: TidyID, workCardID: String, phase: String, operator: String, good: Int,
                    sub: Option[Int], subNotPack: Option[Int], stain: Option[Int], longShort: Option[Int],
                    broken: Option[Int], notEven: Option[Int], oil: Option[Int], head: Option[Int],
                    var date: Long) {
  def toDocument = {
    Document("_id" -> _id.toDocument, "workCardID" -> workCardID, "phase" -> phase, "operator" -> operator,
      "good" -> good, "sub" -> sub, "subNotPack" -> subNotPack, "stain" -> stain, "longShort" -> longShort,
      "broken" -> broken, "notEven" -> notEven, "oil" -> oil, "head" -> head,
      "date" -> date)
  }
}

object TidyCard {
  import org.mongodb.scala.model.Indexes.ascending

  val ColName = "tidyCards"
  val collection = MongoDB.database.getCollection(ColName)
  def init(colNames: Seq[String]) {
    import org.mongodb.scala.model.IndexOptions
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>
          val cf2 = collection.createIndex(ascending("workCardID", "phase"), IndexOptions().unique(true)).toFuture()
      })
    }
  }

  def default(workCardID: String, phase: String) =
    TidyCard(TidyID(workCardID, phase), workCardID, phase, "", 0, None, None, None,
      None, None, None, None, None, 0)

  implicit val idRead = Json.reads[TidyID]
  implicit val idWrite = Json.writes[TidyID]
  implicit val read = Json.reads[TidyCard]
  implicit val write = Json.writes[TidyCard]

  implicit object TransformTidyRecord extends BsonTransformer[TidyCard] {
    def apply(tr: TidyCard): BsonDocument = tr.toDocument.toBsonDocument
  }

  def toTidyID(doc: BsonDocument) = {
    val workCardID = doc.getString("workCardID").getValue
    val phase = doc.getString("phase").getValue
    TidyID(workCardID, phase)
  }

  def toTidyCard(implicit doc: Document) = {
    val _id = toTidyID(doc("_id").asDocument())
    val workCardID = doc.getString("workCardID")
    val phase = doc.getString("phase")
    val operator = doc.getString("operator")
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
    TidyCard(
      _id = _id,
      workCardID = workCardID,
      phase = phase,
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

  def newCard(card: TidyCard) = {
    collection.insertOne(card.toDocument).toFuture()
  }

  def upsertCard(card: TidyCard, inventory: Int, quantity: Int, active: Boolean) = {
    import org.mongodb.scala.model.UpdateOptions
    import org.mongodb.scala.model.Filters._
    val workCardF =
      WorkCard.updateGoodAndActive(card.workCardID, card.good, inventory, quantity,
        active && (card.good + inventory) != 0, card.phase == "整理包裝")
    workCardF.onFailure { errorHandler }

    val f = collection.replaceOne(equal("_id", card._id.toDocument), card.toDocument, UpdateOptions().upsert(true)).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def queryCards(begin: Long, end: Long) = {
    import org.mongodb.scala.model.Filters._

    val f = collection.find(and(gte("date", begin), lt("date", end)))
      .sort(org.mongodb.scala.model.Sorts.ascending("date")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc =>
        toTidyCard(doc)
    }
  }

  def getTidyCard(workCardID: String, phase: String) = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(and(equal("workCardID", workCardID), equal("phase", phase))).first().toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc =>
        toTidyCard(doc)
    }

  }

  def getTidyCardOfWorkCard(workCardID: String) = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(equal("workCardID", workCardID)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      doc =>
        toTidyCard(doc)
    }
  }
}