package controllers

import play.api.mvc.Controller
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import models._
import scala.concurrent.ExecutionContext.Implicits.global
import models.ModelHelper._

/**
 * Created by user on 2017/1/13.
 */
object OrderManager extends Controller {
  def upsertOrder = Action.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Order]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        order => {
          val trimedOrder = order.trim
          val f = Order.upsertOrder(trimedOrder)
          if (order.date.isEmpty)
            order.date = Some(DateTime.now().getMillis)

          f.recover({
            case ex: Throwable =>
              Logger.error("upsertOrder failed", ex)
              Ok(Json.obj("ok" -> false))
          })

          for (result <- f)
            yield Ok(Json.obj("ok" -> true))
        })
  }

  def checkOrderId(orderId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.getOrder(orderId)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        val ok = records.isEmpty
        Ok(Json.obj("ok" -> ok))
      }
  }

  def getOrder(orderId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.getOrder(orderId)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (orderOpt <- f) yield {
        if (orderOpt.isEmpty)
          NoContent
        else {
          Ok(Json.toJson(orderOpt.get))
        }
      }
  }

  def getDepartmentInfoList = Security.Authenticated {
    implicit request =>
      implicit val writer = Json.writes[DeparmentInfo]
      Ok(Json.toJson(Department.getInfoList))
  }

  def myActiveOrder(userId: String, skip: Int, limit: Int) = Security.Authenticated.async {
    implicit request =>
      val f = Order.myActiveOrder(userId)(skip, limit)
      for (orderList <- f) yield {
        Ok(Json.toJson(orderList))
      }
  }

  def myActiveOrderCount(userId: String) = Security.Authenticated.async {
    val f = Order.myActiveOrderCount(userId)
    for (count <- f) yield {
      Ok(Json.toJson(count))
    }
  }

  case class WorkCardSpec(orderId: String, factoryId: String, index: Int, detail: OrderDetail, due: Long, need: Int, inventory: Int)
  case class DyeCardSpec(color: String, due: Long, workCardSpecList: Seq[WorkCardSpec])

  def getDyeCardSpec = Security.Authenticated.async {
    implicit request =>
      val f = Order.listActiveOrder()

      for {
        orderList <- f
      } yield {
        def needToProduceF(orderId: String, detailIndex: Int, detail: OrderDetail) = {
          def shipmentFuture = {
            val f = WorkCard.getOrderWorkCardShipment(orderId, detailIndex)
            for (shipment <- f) yield {
              shipment.sum
            }
          }
          for (shipment <- shipmentFuture)
            yield detail.quantity - shipment
        }

        val colorWorkCardSpecSeq =
          for {
            order <- orderList
            (detail, detailIndex) <- order.details.zipWithIndex if !detail.complete
            inventoryF = Inventory.canLend(order.factoryId, detail.color, detail.size)
            needF = needToProduceF(order._id, detailIndex, detail)
          } yield {
            val f =
              for {
                inventory <- inventoryF
                need <- needF
              } yield {
                if (need > 0)
                  Some(detail.color ->
                    WorkCardSpec(order._id, order.factoryId, detailIndex, detail,
                      order.expectedDeliverDate, need, inventory))
                else
                  None
              }
            waitReadyResult(f)
          }

        import scala.collection.mutable.Map
        val dyeWorkCardMap = Map.empty[String, List[WorkCardSpec]]

        for {
          colorWorkCardSpec <- colorWorkCardSpecSeq.flatMap(x => x)
          color = colorWorkCardSpec._1
          workCardSpec = colorWorkCardSpec._2
        } {
          val specList = dyeWorkCardMap.getOrElseUpdate(color, List.empty[WorkCardSpec])
          dyeWorkCardMap.put(color, workCardSpec :: specList)
        }

        val dyeCardSpecList = dyeWorkCardMap map {
          kv =>
            val color = kv._1
            val workSpecList = kv._2.sortBy { _.due }
            val due = workSpecList.head.due
            DyeCardSpec(color, due, workSpecList)
        }

        val sortedDyeCardSpecList = dyeCardSpecList.toSeq.sortBy { x => x.due }

        import Order._
        implicit val workCardSpecWrite = Json.writes[WorkCardSpec]
        implicit val dyeCardSpecWrite = Json.writes[DyeCardSpec]

        Ok(Json.toJson(sortedDyeCardSpecList))
      }
  }

  def checkDyeCardId(id: String) = Security.Authenticated.async {
    implicit request =>
      val f = DyeCard.getCard(id)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        val ok = records.isEmpty
        Ok(Json.obj("ok" -> ok))
      }
  }

  def checkWorkCardId(id: String) = Security.Authenticated.async {
    implicit request =>
      val f = WorkCard.getCard(id)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        val ok = records.isEmpty
        Ok(Json.obj("ok" -> ok))
      }
  }

  case class ScheduleParam(dyeCard: DyeCard, workCards: Seq[WorkCard])

  def scheduleDyeWork = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      import WorkCard._
      implicit val scheduleParamRead = Json.reads[ScheduleParam]
      val result = request.body.validate[ScheduleParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val now = DateTime.now()
          val rawDyeCard = param.dyeCard
          val remark = rawDyeCard.remark
          val dyeCard = rawDyeCard.init

          val rawWorkCards = param.workCards
          val workCards = rawWorkCards map { raw =>
            val workCard = raw.init
            workCard.dyeCardID = Some(dyeCard._id)
            workCard.remark = remark
            workCard
          }

          val workCardId = workCards.map { _._id }
          dyeCard.workIdList = workCardId
          def updateDyeCardSizeChart = {
            var orderSet = Set.empty[String]
            for (workCard <- workCards)
              orderSet += (workCard.orderId)

            val f = Order.findOrders(orderSet.toSeq)
            for (orders <- f) yield {
              val pair = orders map { order => order._id -> order }
              pair.toMap
            }
          }

          val orderMap = waitReadyResult(updateDyeCardSizeChart)

          val sizeList = workCards.map {
            work =>
              val order = orderMap(work.orderId)
              order.details(work.detailIndex).size
          }
          val sizeSet = Set(sizeList.toSeq: _*)
          val sizeCharts = sizeSet.toSeq map { SizeChart(_, None, None) }
          dyeCard.sizeCharts = Some(sizeCharts)

          val f1 = DyeCard.newCard(dyeCard)
          val f2 = WorkCard.insertCards(workCards)
          val f3 = Future.sequence(workCards.map {
            workCard =>
              Order.addOrderDetailWorkID(workCard.orderId, workCard.detailIndex, workCard._id)
          })
          val f4 = Future.sequence(workCards flatMap {
            workCard =>
              workCard.inventory map {
                borrow =>
                  val order = orderMap(workCard.orderId)
                  val detail = order.details(workCard.detailIndex)
                  Inventory.lend(order.factoryId, detail.color, detail.size, borrow, workCard._id)
              }
          })
          val f5 = Future.sequence(List(f1, f2, f3, f4))
          for (ret <- f5) yield {
            Ok(Json.obj("ok" -> true))
          }
        })
  }

  import Order._
  def queryOrder(skip: Int, limit: Int) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryOrderParam]
      val result = request.body.validate[QueryOrderParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = Order.queryOrder(param)(skip, limit)
          for (orderList <- f)
            yield Ok(Json.toJson(orderList))
        })
  }
  def queryOrderCount() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryOrderParam]
      val result = request.body.validate[QueryOrderParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = Order.queryOrderCount(param)
          for (count <- f)
            yield Ok(Json.toJson(count))
        })
  }

  def closeOrder() = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[Order]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        order => {
          val f = Order.closeOrder(order._id)
          for (rets <- f) yield {
            if (rets.isEmpty)
              Ok(Json.obj("ok" -> false, "msg" -> "找不到訂單"))
            else {
              Ok(Json.obj("ok" -> true))
            }
          }
        })
  }

  def reopenOrder(_id: String) = Security.Authenticated.async {
    val f = Order.reopenOrder(_id)
    for (rets <- f) yield {
      if (rets.isEmpty)
        Ok(Json.obj("ok" -> false, "msg" -> "找不到訂單"))
      else {
        Ok(Json.obj("ok" -> true))
      }
    }
  }

  def deleteOrder(_id: String) = Security.Authenticated.async {
    import WorkCard._
    val param = QueryWorkCardParam(_id = None, orderId = Some(_id), start = None, end = None)
    val f = WorkCard.query(param)(0, 100)
    for (workCardList <- f) yield {
      if (workCardList.isEmpty) {
        Order.deleteOrder(_id)
        Ok(Json.obj("ok" -> true))
      } else {
        Ok(Json.obj("ok" -> false, "msg" -> "訂單已排入生產, 無法刪除"))
      }
    }
  }

  def getOrderPdf(id: String) = Security.Authenticated.async {
    import PdfUtility._
    val orderF = Order.getOrder(id)
    for (orderOpt <- orderF) yield {
      if (orderOpt.isDefined)
        Ok.sendFile(createItextPdf(orderProc(orderOpt.get)),
          fileName = _ =>
            play.utils.UriEncoding.encodePathSegment(s"訂單${id}.pdf", "UTF-8"))
      else
        BadRequest("No such order!")
    }
  }

  def getColorSeq() = Security.Authenticated.async {
    val colorSeqF = SysConfig.getColorSeq()
    for (colorSeq <- colorSeqF) yield {
      val sortedSeq = colorSeq.sortWith(_.compareTo(_) < 0)
      Ok(Json.toJson(sortedSeq))
    }
  }

  case class ColorSeqParam(colorSeq: Seq[String])
  def deleteColorSeq(json: String) = Security.Authenticated.async {
    implicit val reads = Json.reads[ColorSeqParam]
    val ret = Json.parse(json).validate[ColorSeqParam]
    ret.fold(err => {
      Logger.error(JsError.toJson(err).toString())
      Future {
        BadRequest("")
      }
    }, param => {
      for (f <- SysConfig.delColorSeq(param.colorSeq))
        yield Ok(Json.obj("ok" -> true))
    })
  }

  def getBrandList(): Action[AnyContent] = Security.Authenticated.async {
    val brandListF = SysConfig.getBrandList()
    for (brandList <- brandListF) yield {
      val sortedSeq = brandList.sortWith(_.compareTo(_) < 0)
      Ok(Json.toJson(sortedSeq))
    }
  }

  def deleteBrandList(brands:String): Action[AnyContent] = Security.Authenticated.async {
    val brandListF = SysConfig.getBrandList()
    for (brandList <- brandListF) yield {
      val toBeDeleted = brands.split(",").toSeq
      val newBrands = brandList.filterNot(toBeDeleted.contains(_))
      SysConfig.setBrandList(newBrands)
      Ok(Json.obj("ok" -> true))
    }
  }
}
