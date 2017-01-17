package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class RefineProcess(refine: Option[Double], refineTime: Option[Int],
                         milk: Option[Double], refineTemp: Option[Double],
                         noBubblePotion: Option[Double]) {
  def toDocument =
    Document("refine" -> refine, "refineTime" -> refineTime,
      "milk" -> milk, "refineTemp" -> refineTemp,
      "noBubblePotion" -> noBubblePotion)

}
case class DyeProcess(evenDye: Option[Double], dyeTime: Option[Int],
                      vNH3: Option[Double], dyeTemp: Option[Double],
                      iceV: Option[Double], dyePotion: Option[String]) {
  def toDocument = Document("evenDye" -> evenDye, "dyeTime" -> dyeTime,
    "vNH3" -> vNH3, "dyeTemp" -> dyeTemp,
    "iceV" -> iceV, "dyePotion" -> dyePotion)
}

case class PostProcess(fixedPotion: Option[Double], fixedTime: Option[Int],
                       postIceV: Option[Double], fixedTemp: Option[Double],
                       postiveSoftener: Option[Double], softenTime: Option[Int],
                       silicon: Option[Double],
                       dryTemp: Option[Double],
                       dryTime: Option[Int]) {
  def toDocument = Document("fixedPotion" -> fixedPotion, "fixedTime" -> fixedTime,
    "postIceV" -> postIceV, "fixedTemp" -> fixedTemp,
    "postiveSoftener" -> postiveSoftener, "softenTime" -> softenTime,
    "silicon" -> silicon,
    "dryTemp" -> dryTemp,
    "dryTime" -> dryTime)
}

case class DyeCard(var _id: String, var workIdList: Seq[String], color: String,
                   startTime: Option[Long], endTime: Option[Long],
                   weight: Option[Double], water: Option[Double], operator: Option[String],
                   refineProcess: Option[RefineProcess],
                   dyeProcess: Option[DyeProcess],
                   postProcess: Option[PostProcess]) {
  def toDocument = {
    Document("_id" -> _id, "workIdList" -> workIdList,
      "startTime" -> startTime, "endTime" -> endTime,
      "color" -> color, "weight" -> weight, "water" -> water, "operator" -> operator,
      "refineProcess" -> refineProcess.map { _.toDocument },
      "dyeProcess" -> dyeProcess.map(_.toDocument),
      "postProcess" -> postProcess.map(_.toDocument))
  }

  def updateID:Unit = {
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
}

object DyeCard {

  val ColName = "dyeCards"
  val collection = MongoDB.database.getCollection(ColName)

  implicit val refineRead = Json.reads[RefineProcess]
  implicit val refineWrite = Json.writes[RefineProcess]
  implicit val dpRead = Json.reads[DyeProcess]
  implicit val dpWrite = Json.writes[DyeProcess]
  implicit val ppRead = Json.reads[PostProcess]
  implicit val ppWrite = Json.writes[PostProcess]

  implicit val dyeRead = Json.reads[DyeCard]
  implicit val dyeWrite = Json.writes[DyeCard]

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }
  }

  import org.mongodb.scala.bson._
  def toRefineProcess(implicit doc: Document) = {
    val refine = getOptionDouble("refine")
    val refineTime = getOptionInt("refineTime")
    val milk = getOptionDouble("milk")
    val refineTemp = getOptionDouble("refineTemp")
    val noBubblePotion = getOptionDouble("noBubblePotion")

    RefineProcess(refine, refineTime, milk, refineTemp, noBubblePotion)
  }

  def toDyeProcess(implicit doc: Document) = {
    val evenDye = getOptionDouble("evenDye")
    val dyeTime = getOptionInt("dyeTime")
    val vNH3 = getOptionDouble("vNH3")
    val dyeTemp = getOptionDouble("dyeTemp")
    val iceV = getOptionDouble("iceV")
    val dyePotion = getOptionStr("dyePotion")

    DyeProcess(evenDye, dyeTime, vNH3, dyeTemp, iceV, dyePotion)
  }

  def toPostProcess(implicit doc: Document) = {
    val fixedPotion = getOptionDouble("fixedPotion")
    val fixedTime = getOptionInt("fixedTime")
    val postIceV = getOptionDouble("postIceV")
    val fixedTemp = getOptionDouble("fixedTemp")
    val postiveSoftener = getOptionDouble("postiveSoftener")
    val softenTime = getOptionInt("softenTime")
    val silicon = getOptionDouble("silicon")
    val dryTemp = getOptionDouble("dryTemp")
    val dryTime = getOptionInt("dryTemp")

    PostProcess(fixedPotion, fixedTime, postIceV, fixedTemp,
      postiveSoftener, softenTime,
      silicon, dryTemp, dryTime)
  }

  def toDyeCard(implicit doc: Document) = {
    val _id = doc.getString("_id")
    val workIdList = getArray("workIdList", (v: BsonValue) => { v.asString().getValue })
    val startTime = getOptionTime("startTime")
    val endTime = getOptionTime("endTime")
    val color = doc.getString("color")
    val weight = getOptionDouble("weight")
    val water = getOptionDouble("water")
    val operator = getOptionStr("operator")
    val refineProcess = getOptionDoc("refineProcess") map { toRefineProcess(_) }
    val dyeProcess = getOptionDoc("dyeProcess") map { toDyeProcess(_) }
    val postProcess = getOptionDoc("postProcess") map { toPostProcess(_) }

    DyeCard(_id = _id, workIdList = workIdList,
      startTime = startTime, endTime = endTime,
      color = color,
      weight = weight,
      water = water,
      operator = operator,
      refineProcess = refineProcess,
      dyeProcess = dyeProcess,
      postProcess = postProcess)
  }

  def newCard(card: DyeCard) = {
    collection.insertOne(card.toDocument).toFuture()
  }

  import org.mongodb.scala.model.Filters._
  def deleteCard(id: String) = {
    collection.deleteOne(equal("_id", id)).toFuture()
  }

  def updateCard(card: DyeCard) = {
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
        Some(toDyeCard(cards(0)))
    }
  }

  def getActiveDyeCards() = {
    val f = collection.find().toFuture()
    f.onFailure { errorHandler }
    for (cards <- f) yield cards.map {
      doc =>
        toDyeCard(doc)
    }
  }
}
