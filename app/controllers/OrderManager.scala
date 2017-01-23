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
          val f = Order.upsertOrder(order)
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
      val f = Order.findOrder(orderId)
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

  def getOrder(orderId:String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.findOrder(orderId)
      f.recover({
        case ex: Throwable =>
          Logger.error("checkOrderId failed", ex)
          Ok(Json.obj("ok" -> false))
      })

      for (records <- f) yield {
        if(records.isEmpty)
          NoContent
        else{
          Ok(Json.toJson(records.head))  
        }        
      }
  }

  def getDepartmentInfoList = Security.Authenticated {
    implicit request =>
      implicit val writer = Json.writes[DeparmentInfo]
      Ok(Json.toJson(Department.getInfoList))
  }

  def myActiveOrder(userId: String) = Security.Authenticated.async {
    implicit request =>
      val f = Order.myActiveOrder(userId)
      for (orderList <- f) yield {
        Ok(Json.toJson(orderList))
      }

  }

  case class WorkCardSpec(orderId: String, index: Int, detail: OrderDetail, due: Long, need: Int)
  case class DyeCardSpec(color: String, due: Long, workCardSpecList: Seq[WorkCardSpec])

  def getDyeCardSpec = Security.Authenticated.async {
    implicit request =>
      val f = Order.listActiveOrder()
      val f2 = WorkCard.getActiveWorkCards()
      for {
        orderList <- f
        workCardList <- f2
      } yield {
        val workCardPairs =
          for (workCard <- workCardList) yield workCard._id -> workCard
        val workCardMap = workCardPairs.toMap

        import scala.collection.mutable.Map
        val dyeWorkCardMap = Map.empty[String, List[WorkCardSpec]]
        def needToProduce(detail: OrderDetail) = {
          def inProduction = {
            val quantities = detail.workCardIDs map {
              id =>
                val workCard = workCardMap(id)
                workCard.quantity
            }
            quantities.foldLeft(0)((a, b) => a + b)
          }

          detail.quantity - inProduction
        }

        for {
          order <- orderList
          detail_idx <- order.details.zipWithIndex if !detail_idx._1.complete && needToProduce(detail_idx._1) > 0
          detail = detail_idx._1
          index = detail_idx._2
          need = needToProduce(detail)
        } {
          val specList = dyeWorkCardMap.getOrElseUpdate(detail.color, List.empty[WorkCardSpec])
          val newSpecList = WorkCardSpec(order._id, index, detail, order.expectedDeliverDate, need) :: specList
          val sortedSpecList = newSpecList.sortBy { wcs => wcs.due }
          dyeWorkCardMap.put(detail.color, sortedSpecList)
        }
        val dyeCardSpecList = dyeWorkCardMap map {
          kv =>
            val color = kv._1
            val workSpecList = kv._2
            DyeCardSpec(color, workSpecList.head.due, workSpecList)
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
          val dyeCard = param.dyeCard
          if (dyeCard._id == "") {
            dyeCard.updateID
          }

          val workCards = param.workCards
          workCards.foreach { workCard =>
            if (workCard._id == "")
              workCard.updateID

            if (workCard.dyeCardID.isEmpty)
              workCard.dyeCardID = Some(dyeCard._id)
          }

          val workCardId = workCards.map { _._id }
          dyeCard.workIdList = workCardId

          val f1 = DyeCard.newCard(dyeCard)
          val f2 = WorkCard.insertCards(workCards)
          val f3 = Future.sequence(workCards.map {
            workCard =>
              Order.addOrderDetailWorkID(workCard.orderId, workCard.detailIndex, workCard._id)
          })

          val f4 = Future.sequence(List(f1, f2, f3))
          for (ret <- f3) yield {
            Ok(Json.obj("ok" -> true))
          }
        })
  }
}
