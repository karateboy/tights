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
                     Fluorescent: Option[Double], Brightener: Option[Double], black: Option[Double],
                     otherDyeType: Option[String], otherDye: Option[Double]) {
  def toDocument = Document("y" -> y, "r" -> r, "b" -> b,
    "Fluorescent" -> Fluorescent, "Brightener" -> Brightener, "black" -> black,
    "otherDyeType" -> otherDyeType, "otherDye" -> otherDye)

}
object DyePotion {
  val default = DyePotion(None, None, None, None, None, None, None, None)
}

case class DyeProcess(evenDye: Option[Double], vNH3: Option[Double], nh3: Option[Double], iceV: Option[Double],
                      evenDyeType: Option[String],
                      dyeTime: Option[Int], dyeTemp: Option[Double],
                      phStart: Option[Double], phEnd: Option[Double]) {
  def toDocument = Document("evenDye" -> evenDye, "vNH3" -> vNH3, "nh3" -> nh3,
    "iceV" -> iceV, "evenDyeType" -> evenDyeType, "dyeTime" -> dyeTime, "dyeTemp" -> dyeTemp,
    "phStart" -> phStart, "phEnd" -> phEnd)

}
object DyeProcess {
  val default = DyeProcess(None, None, None, None, None, None, None, None, None)
}

case class PostProcess(fixedPotion: Option[Double],
                       iceV: Option[Double], silicon: Option[Double], postiveSoftener: Option[Double],
                       softenTime: Option[Int], temp: Option[Double]) {
  def toDocument = Document("fixedPotion" -> fixedPotion,
    "iceV" -> iceV, "silicon" -> silicon, "postiveSoftener" -> postiveSoftener,
    "softenTime" -> softenTime, "temp" -> temp)

}
object PostProcess {
  val default = PostProcess(None, None, None, None, None, None)
}

case class SizeChart(size: String, before: Option[Double], after: Option[Double]) {
  def toDocument = Document("size" -> size, "before" -> before, "after" -> after)
  implicit object TransformBigDecimal extends BsonTransformer[SizeChart] {
    def apply(value: SizeChart): BsonDocument = value.toDocument.toBsonDocument
  }
}

case class DyeCard(var _id: String, var workIdList: Seq[String], color: String,
                   startTime: Option[Long], endTime: Option[Long], var updateTime: Option[Long], var active: Boolean, remark: Option[String],
                   operator: Option[String], date: Option[Long], pot: Option[String], weight: Option[Double],
                   refineProcess: Option[RefineProcess],
                   dyePotion: Option[DyePotion],
                   dyeProcess: Option[DyeProcess],
                   postProcess: Option[PostProcess],
                   dryTemp: Option[Double], dryTime: Option[Long], machine: Option[String],
                   var sizeCharts: Option[Seq[SizeChart]],
                   dep: Option[String]) {

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
      "startTime" -> startTime, "endTime" -> endTime, "updateTime" -> updateTime,
      "operator" -> operator, "date" -> date, "pot" -> pot, "weight" -> weight,
      "refineProcess" -> refineProcess,
      "dyePotion" -> dyePotion,
      "dyeProcess" -> dyeProcess,
      "postProcess" -> postProcess,
      "dryTime" -> dryTime, "dryTemp" -> dryTemp, "machine" -> machine,
      "sizeCharts" -> sizeCharts,
      "active" -> active, "remark" -> remark, "dep" -> dep)
  }

  def updateID: Unit = {
    //import java.util.concurrent.ThreadLocalRandom
    //val randomNum = ThreadLocalRandom.current().nextInt(1, 1000000)
    val idF = Identity.getNewID("dyeCard")
    val id = waitReadyResult(idF)
    val newID = "%06d".format(id.seq)
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
      startTime = None,
      endTime = None,
      updateTime = None,
      active = true,
      operator = None,
      date = None,
      pot = None, weight = None,
      refineProcess = Some(RefineProcess.default),
      dyePotion = Some(DyePotion.default),
      dyeProcess = Some(DyeProcess.default),
      postProcess = Some(PostProcess.default),
      dryTemp = None, dryTime = None, machine = None,
      sizeCharts = None,
      remark = remark,
      dep = dep)
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
    val black = getOptionDouble("black")
    val otherDyeType = getOptionStr("otherDyeType")
    val otherDye = getOptionDouble("otherDye")
    DyePotion(y = y, r = r, b = b,
      Fluorescent = Fluorescent,
      Brightener = Brightener,
      black = black,
      otherDye = otherDye,
      otherDyeType = otherDyeType)
  }

  def toDyeProcess(implicit doc: Document) = {
    val evenDye = getOptionDouble("evenDye")
    val dyeTime = getOptionInt("dyeTime")
    val vNH3 = getOptionDouble("vNH3")
    val nh3 = getOptionDouble("nh3")
    val evenDyeType = getOptionStr("evenDyeType")
    val dyeTemp = getOptionDouble("dyeTemp")
    val iceV = getOptionDouble("iceV")
    val phStart = getOptionDouble("phStart")
    val phEnd = getOptionDouble("phEnd")

    DyeProcess(evenDye = evenDye, vNH3 = vNH3, nh3 = nh3, iceV = iceV, evenDyeType = evenDyeType,
      dyeTemp = dyeTemp, dyeTime = dyeTime,
      phStart = phStart, phEnd = phEnd)
  }

  def toPostProcess(implicit doc: Document) = {
    val fixedPotion = getOptionDouble("fixedPotion")
    val iceV = getOptionDouble("iceV")
    val postiveSoftener = getOptionDouble("postiveSoftener")
    val softenTime = getOptionInt("softenTime")
    val silicon = getOptionDouble("silicon")
    val temp = getOptionDouble("temp")

    PostProcess(fixedPotion = fixedPotion, iceV = iceV, silicon = silicon,
      postiveSoftener = postiveSoftener, softenTime = softenTime, temp = temp)
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
    val endTime = getOptionTime("endTime")
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
    val machine = getOptionStr("machine")
    val sizeCharts = getOptionArray("sizeCharts", (v) => { toSizeChart(v.asDocument()) })
    val active = doc.getBoolean("active")
    val remark = getOptionStr("remark")
    val dep = getOptionStr("dep")

    DyeCard(_id = _id, workIdList = workIdList, color = color,
      startTime = startTime, endTime = endTime, updateTime = updateTime, remark = remark,
      operator = operator, date = date, pot = pot, weight = weight,
      refineProcess = refineProcess,
      dyePotion = dyePotion,
      dyeProcess = dyeProcess,
      postProcess = postProcess,
      dryTemp = dryTemp,
      dryTime = dryTime,
      machine = machine,
      sizeCharts = sizeCharts,
      active = active,
      dep = dep)
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

  def getActiveDyeCards(skip: Int, limit: Int) = {
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.descending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map {
      doc =>
        toDyeCard(doc)
    }
  }

  def getActiveDyeCardCount() = {
    import org.mongodb.scala.model._
    val f = collection.count(equal("active", true)).toFuture()
    f.onFailure { errorHandler }
    for (countSeq <- f) yield countSeq(0)
  }

  case class QueryDyeCardParam(_id: Option[String], color: Option[String],
                               start: Option[Long], end: Option[Long], active: Option[Boolean], orderID: Option[String])

  def getFilter(param: QueryDyeCardParam) = {
    import org.mongodb.scala.model.Filters._
    val idFilter = param._id map { _id => regex("_id", _id) }
    val colorFilter = param.color map { color => regex("color", color) }
    val startFilter = param.start map { gte("startTime", _) }
    val endFilter = param.end map { lt("startTime", _) }
    val activeFilter = param.active map { active => equal("active", active) }
    val filterList = List(idFilter, colorFilter, startFilter, endFilter, activeFilter).flatMap { f => f }
    if (param.orderID.isDefined) {
      val orderID = param.orderID.get
      val workCardIdFilterFuture =
        for {
          workCards <- WorkCard.getOrderProductionWorkCards(orderID)
          workCardIDs = workCards map { _._id }
        } yield in("workIdList", workCardIDs: _*)

      workCardIdFilterFuture map {
        workCardIdFilter =>
          and(workCardIdFilter :: filterList: _*)
      }
    } else {
      val filter = if (!filterList.isEmpty)
        and(filterList: _*)
      else
        exists("_id")

      import scala.concurrent._
      Future {
        filter
      }
    }
  }

  def query(param: QueryDyeCardParam)(skip: Int, limit: Int) = {
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
      doc => toDyeCard(doc)
    }
  }

  def count(param: QueryDyeCardParam) = {
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

  import org.mongodb.scala.model._

  def transferDep(_id: String, dep: String) = {
    collection.updateOne(equal("_id", _id), Updates.set("dep", dep)).toFuture()
  }

  def startDye(_id: String, operator: String) = {
    import org.mongodb.scala.model._
    collection.updateOne(equal("_id", _id),
      and(Updates.set("operator", operator), Updates.set("startTime", DateTime.now.getMillis))).toFuture()
  }

  def endDye(_id: String) = {
    import org.mongodb.scala.model._
    collection.updateOne(equal("_id", _id),
      and(Updates.set("endTime", DateTime.now.getMillis), Updates.set("active", false))).toFuture()
  }

  def moveWorkCard(workCardId: String, moveOutDyeCardId: String, moveInDyeCardId: String) = {
    import org.mongodb.scala.model._

    val f1 = collection.updateOne(equal("_id", moveOutDyeCardId), Updates.pull("workIdList", workCardId)).toFuture()
    val f2 = collection.updateOne(equal("_id", moveInDyeCardId), Updates.addToSet("workIdList", workCardId)).toFuture()
    import scala.concurrent._
    Future.sequence(List(f1, f2))
  }
}
