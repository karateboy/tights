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
  def queryInventory(paramJson: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]
      val result = Json.parse(paramJson).validate[QueryInventoryParam]

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

  def queryInventoryCount(json: String) = Security.Authenticated.async {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]

      val result = Json.parse(json).validate[QueryInventoryParam]

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

  def queryInventoryTotal(json: String) = Security.Authenticated.async {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]

      val result = Json.parse(json).validate[QueryInventoryParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val fCount = Inventory.total(param)
        for (count <- fCount) yield {
          Ok(Json.toJson(count))
        }
      })
  }

  def getInventoryReport(json: String) = Security.Authenticated.async {
    implicit request =>
      implicit val reads = Json.reads[QueryInventoryParam]

      val result = Json.parse(json).validate[QueryInventoryParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val reportF = Inventory.query(param)(0, 10000)
        for (reports <- reportF) yield {
          val p1 = param.factoryID.getOrElse("")
          val p2 = param.customerID.getOrElse("")
          val p3 = param.brand.getOrElse("")
          val title = s"${p1}${p2}${p3}庫存報表"
          val excel = ExcelUtility.getInventoryReport(reports, title)
          Ok.sendFile(excel, fileName = _ =>
            play.utils.UriEncoding.encodePathSegment(title + ".xlsx", "UTF-8"))
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
          Ok(Json.obj("ok" -> true))
        }
      })
  }

  def deleteInventory(param: String) = Security.Authenticated.async {
    import Inventory._
    val result = Json.parse(param).validate[QueryInventoryParam]
    result.fold(err => {
      Future {
        Logger.error(JsError.toJson(err).toString())
        BadRequest(JsError.toJson(err).toString())
      }
    }, param => {
      val f = Inventory.delete(param)
      for (ret <- f) yield Ok(Json.obj("ok" -> true))
    })
  }

  def refreshInventoryLoan = Security.Authenticated(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Inventory]

      result.fold(err => {
        Logger.error(JsError.toJson(err).toString())
        BadRequest(JsError.toJson(err).toString())
      }, inv => {
        val retF = Inventory.refreshLoan(inv.factoryID, inv.color, inv.size)

        Ok(Json.obj("ok" -> true))
      })
  }

}