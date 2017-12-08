package controllers
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._
import scala.concurrent.ExecutionContext.Implicits.global

case class Stat(
    avg: Option[Double],
    min: Option[Double],
    max: Option[Double],
    count: Int,
    total: Int,
    overCount: Int) {
  val effectPercent = {
    if (total > 0)
      Some(count.toDouble * 100 / total)
    else
      None
  }

  val isEffective = {
    effectPercent.isDefined && effectPercent.get > 75
  }
  val overPercent = {
    if (count > 0)
      Some(overCount.toDouble * 100 / total)
    else
      None
  }
}

object Query extends Controller {
  def getPeriods(start: DateTime, endTime: DateTime, d: Period): List[DateTime] = {
    import scala.collection.mutable.ListBuffer

    val buf = ListBuffer[DateTime]()
    var current = start
    while (current < endTime) {
      buf.append(current)
      current += d
    }

    buf.toList
  }

  def getPeriodCount(start: DateTime, endTime: DateTime, p: Period) = {
    var count = 0
    var current = start
    while (current < endTime) {
      count += 1
      current += p
    }

    count
  }

  import scala.concurrent._
  def queryInventory(skip:Int, limit:Int) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]
      val result = request.body.validate[QueryInventoryParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val fInventory = Inventory.query(param)(skip, limit)
        for (Inventory <- fInventory) yield {
          Ok(Json.toJson(Inventory))
        }
      })
  }

  def queryInventoryCount = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]
      val result = request.body.validate[QueryInventoryParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val fCount = Inventory.count(param)
        for (count <- fCount) yield {
          Ok(Json.toJson(count))
        }
      })
  }

  def upsertInventory = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Inventory]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, inventory => {
        val retF = Inventory.upsert(inventory)
        for (ret <- retF) yield {
          Ok(Json.obj("ok"->true))
        }
      })

  }
}