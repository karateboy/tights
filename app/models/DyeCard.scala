package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class RefineProcess(refinePotion: Option[String], refine: Option[Double],
                         milk: Option[Double], refineTime: Option[Int], refineTemp: Option[Double]) {
  def toDocument =
    Document("refinePotion" -> refinePotion, "refine" -> refine,
      "milk" -> milk, "refineTime" -> refineTime, "refineTemp" -> refineTemp)

}
object RefineProcess {
  val default = RefineProcess(None, None, None, None, None)
}

case class DyePotion(y: Option[Double], r: Option[Double], b: Option[Double],
                     Fluorescent: Option[Double], Brightener: Option[Double]) {
  def toDocument = Document("y" -> y, "r" -> r, "b" -> b,
    "Fluorescent" -> Fluorescent, "Brightener" -> Brightener)

}
object DyePotion {
  val default = DyePotion(None, None, None, None, None)
}

case class DyeProcess(evenDye: Option[Double], vNH3: Option[Double], nh3: Option[Double], iceV: Option[Double],
                      other: Option[String],
                      dyeTime: Option[Int], dyeTemp: Option[Double],
                      phStart: Option[Double], phEnd: Option[Double]) {
  def toDocument = Document("evenDye" -> evenDye, "vNH3" -> vNH3, "nh3" -> nh3,
    "iceV" -> iceV, "other" -> other, "dyeTime" -> dyeTime, "dyeTemp" -> dyeTemp,
    "phStart" -> phStart, "phEnd" -> phEnd)

}
object DyeProcess {
  val default = DyeProcess(None, None, None, None, None, None, None, None, None)
}

case class PostProcess(fixedPotion: Option[Double],
                       iceV: Option[Double], silicon: Option[Double], postiveSoftener: Option[Double],
                       softenTime: Option[Int]) {
  def toDocument = Document("fixedPotion" -> fixedPotion,
    "iceV" -> iceV, "silicon" -> silicon, "postiveSoftener" -> postiveSoftener,
    "softenTime" -> softenTime)

}
object PostProcess {
  val default = PostProcess(None, None, None, None, None)
}

case class SizeChart(size: String, before: Option[Double], after: Option[Double]) {
  def toDocument = Document("size" -> size, "before" -> before, "after" -> after)
  implicit object TransformBigDecimal extends BsonTransformer[SizeChart] {
    def apply(value: SizeChart): BsonDocument = value.toDocument.toBsonDocument
  }
}

case class DyeCard(var _id: String, var workIdList: Seq[String], color: String,
                   startTime: Option[Long], var updateTime: Option[Long], var active: Boolean,
                   operator: Option[String], date: Option[Long], pot: Option[String], weight: Option[Double],
                   refineProcess: Option[RefineProcess],
                   dyePotion: Option[DyePotion],
                   dyeProcess: Option[DyeProcess],
                   postProcess: Option[PostProcess],
                   dryTemp: Option[Double], dryTime: Option[Long],
                   var sizeCharts: Option[Seq[SizeChart]]) {

  implicit object TransformSizeChart extends BsonTransformer[SizeChart] {
    def apply(value: SizeChart): BsonDocument = value.toDocument.toBsonDocument
  }

  implicit object TransformDyePotion extends BsonTransformer[DyePotion] {
    def apply(value: DyePotion): BsonDocument = value.toDocument.toBsonDocument
  }

  implicit object TransformRefineProcess extends BsonTransformer[RefineProcess] {
    def apply(value: RefineProcess): BsonDocument = value.toDocument.toBsonDocument
  }

  implicit object TransformDyeProcess extends BsonTransformer[DyeProcess] {
    def apply(value: DyeProcess): BsonDocument = value.toDocument.toBsonDocument
  }

  implicit object TransformPostProcess extends BsonTransformer[PostProcess] {
    def apply(value: PostProcess): BsonDocument = value.toDocument.toBsonDocument
  }

  def toDocument = {
    Document("_id" -> _id, "workIdList" -> workIdList, "color" -> color,
      "startTime" -> startTime, "updateTime" -> updateTime,
      "operator" -> operator, "date" -> date, "pot" -> pot, "weight" -> weight,
      "refineProcess" -> refineProcess,
      "dyePotion" -> dyePotion,
      "dyeProcess" -> dyeProcess,
      "postProcess" -> postProcess,
      "dryTime" -> dryTime, "dryTemp" -> dryTemp,
      "sizeCharts" -> sizeCharts,
      "active" -> active)
  }

  def updateID: Unit = {
    import java.util.concurrent.ThreadLocalRandom
    val randomNum = ThreadLocalRandom.current().nextInt(1, 1000000)
    val newID = "%06d".format(randomNum)
    val f = DyeCard.getCard(newID)
    val ret = waitReadyResult(f)

    if (ret.isEmpty)
      _id = newID
    else
      updateID
  }
  def init = {
    if (_id == "")
      updateID

    DyeCard(
      _id = _id,
      workIdList = workIdList,
      color = color,
      startTime = Some(DateTime.now.getMillis),
      updateTime = None,
      active = true,
      operator = None,
      date = None, 
      pot = None, weight = None,
      refineProcess = Some(RefineProcess.default),
      dyePotion = Some(DyePotion.default),
      dyeProcess = Some(DyeProcess.default),
      postProcess = Some(PostProcess.default),
      dryTemp = None, dryTime = None,
      sizeCharts = None)
  }
}

object DyeCard {

  val ColName = "dyeCards"
  val collection = MongoDB.database.getCollection(ColName)

  import WorkCard._
  implicit val refineRead = Json.reads[RefineProcess]
  implicit val refineWrite = Json.writes[RefineProcess]
  implicit val dpRead = Json.reads[DyeProcess]
  implicit val dpWrite = Json.writes[DyeProcess]
  implicit val ppRead = Json.reads[PostProcess]
  implicit val ppWrite = Json.writes[PostProcess]
  implicit val dyePread = Json.reads[DyePotion]
  implicit val dyePwrite = Json.writes[DyePotion]
  implicit val sizeChartReads = Json.reads[SizeChart]
  implicit val sizeChartWrites = Json.writes[SizeChart]
  implicit val dyeRead = Json.reads[DyeCard]
  implicit val dyeWrite = Json.writes[DyeCard]

  def init(colNames: Seq[String]) {
    import org.mongodb.scala.model.Indexes.ascending
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>
          val cf2 = collection.createIndex(ascending("active")).toFuture()
      })
    }
  }

  import org.mongodb.scala.bson._
  def toRefineProcess(implicit doc: Document) = {
    val refinePotion = getOptionStr("refinePotion")
    val refine = getOptionDouble("refine")
    val refineTime = getOptionInt("refineTime")
    val milk = getOptionDouble("milk")
    val refineTemp = getOptionDouble("refineTemp")

    RefineProcess(refinePotion = refinePotion, refine = refine,
      milk = milk,
      refineTime = refineTime, refineTemp = refineTemp)
  }

  def toDyePotion(implicit doc: Document) = {
    val y = getOptionDouble("y")
    val r = getOptionDouble("r")
    val b = getOptionDouble("b")
    val Fluorescent = getOptionDouble("Fluorescent")
    val Brightener = getOptionDouble("Brightener")
    DyePotion(y = y, r = r, b = b,
      Fluorescent = Fluorescent,
      Brightener = Brightener)
  }

  def toDyeProcess(implicit doc: Document) = {
    val evenDye = getOptionDouble("evenDye")
    val dyeTime = getOptionInt("dyeTime")
    val vNH3 = getOptionDouble("vNH3")
    val nh3 = getOptionDouble("nh3")
    val other = getOptionStr("other")
    val dyeTemp = getOptionDouble("dyeTemp")
    val iceV = getOptionDouble("iceV")
    val phStart = getOptionDouble("phStart")
    val phEnd = getOptionDouble("phEnd")

    DyeProcess(evenDye = evenDye, vNH3 = vNH3, nh3 = nh3, iceV = iceV, other = other,
      dyeTemp = dyeTemp, dyeTime = dyeTime,
      phStart = phStart, phEnd = phEnd)
  }

  def toPostProcess(implicit doc: Document) = {
    val fixedPotion = getOptionDouble("fixedPotion")
    val iceV = getOptionDouble("iceV")
    val postiveSoftener = getOptionDouble("postiveSoftener")
    val softenTime = getOptionInt("softenTime")
    val silicon = getOptionDouble("silicon")

    PostProcess(fixedPotion = fixedPotion, iceV = iceV, silicon = silicon,
      postiveSoftener = postiveSoftener, softenTime = softenTime)
  }

  def toSizeChart(implicit doc: Document) = {
    val size = doc.getString("size")
    val before = getOptionDouble("before")
    val after = getOptionDouble("after")
    SizeChart(size, before, after)
  }

  def toDyeCard(implicit doc: Document) = {
    val _id = doc.getString("_id")
    val workIdList = getArray("workIdList", (v: BsonValue) => { v.asString().getValue })
    val startTime = getOptionTime("startTime")
    val updateTime = getOptionTime("updateTime")
    val color = doc.getString("color")
    val operator = getOptionStr("operator")
    val date = getOptionTime("date")
    val pot = getOptionStr("pot")
    val weight = getOptionDouble("weight")

    val refineProcess = getOptionDoc("refineProcess") map { toRefineProcess(_) }
    val dyePotion = getOptionDoc("dyePotion") map { toDyePotion(_) }
    val dyeProcess = getOptionDoc("dyeProcess") map { toDyeProcess(_) }
    val postProcess = getOptionDoc("postProcess") map { toPostProcess(_) }
    val dryTemp = getOptionDouble("dryTemp")
    val dryTime = getOptionTime("dryTime")
    val sizeCharts = getOptionArray("sizeCharts", (v) => { toSizeChart(v.asDocument()) })
    val active = doc.getBoolean("active")

    DyeCard(_id = _id, workIdList = workIdList, color = color,
      startTime = startTime, updateTime = updateTime,
      operator = operator, date = date, pot = pot, weight = weight,
      refineProcess = refineProcess,
      dyePotion = dyePotion,
      dyeProcess = dyeProcess,
      postProcess = postProcess,
      dryTemp = dryTemp,
      dryTime = dryTime,
      sizeCharts = sizeCharts,
      active = active)
  }

  def newCard(card: DyeCard) = {
    collection.insertOne(card.toDocument).toFuture()
  }

  import org.mongodb.scala.model.Filters._
  def deleteCard(id: String) = {
    collection.deleteOne(equal("_id", id)).toFuture()
  }

  def updateCard(card: DyeCard) = {
    collection.replaceOne(equal("_id", card._id), card.toDocument).toFuture()
  }

  def getCard(id: String) = {
    val f = collection.find(equal("_id", id)).first().toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield {
      if (cards.length == 0)
        None
      else
        Some(toDyeCard(cards(0)))
    }
  }

  def getActiveDyeCards() = {
    val f = collection.find(equal("active", true)).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map {
      doc =>
        toDyeCard(doc)
    }
  }
}
