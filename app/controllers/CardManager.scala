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
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

object CardManager extends Controller {
  def getDyeCardList() = Security.Authenticated.async {
    import DyeCard._
    val f = DyeCard.getActiveDyeCards
    for (cards <- f)
      yield Ok(Json.toJson(cards))
  }

  def getDyeCard(id: String) = Security.Authenticated.async {
    import DyeCard._
    val f = DyeCard.getCard(id)
    for (cardOpt <- f) yield {
      if (cardOpt.isDefined) {
        val card = cardOpt.get
        Ok(Json.toJson(card))
      } else {
        Results.NoContent
      }
    }
  }

  def getWorkCard(id: String) = Security.Authenticated.async {
    implicit request =>
      val f = WorkCard.getCard(id)
      for (card <- f) yield {
        if (card.isEmpty)
          Results.NoContent
        else
          Ok(Json.toJson(card))
      }
  }

  def getWorkCards = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[Seq[String]]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, ids => {
        val f = WorkCard.getCards(ids)
        for (cards <- f) yield {
          Ok(Json.toJson(cards))
        }
      })
  }

  def getActiveWorkCards = Security.Authenticated.async {
    val f = WorkCard.getActiveWorkCards()
    for (workCards <- f)
      yield Ok(Json.toJson(workCards))
  }

  def getDyeCardPdf(id: String) = Security.Authenticated.async {
    import PdfUtility._
    val fileName = s"漂染$id"
    val fDyeCard = DyeCard.getCard(id)
    val ffWorkCard = for (dyeCardOpt <- fDyeCard) yield {
      if (dyeCardOpt.isEmpty)
        Future(Seq.empty[WorkCard])
      else {
        val dyeCard = dyeCardOpt.get
        val fWorkCards = WorkCard.getCards(dyeCard.workIdList)
        for (workCards <- fWorkCards) yield workCards
      }
    }

    val fWorkCard = ffWorkCard.flatMap(f => f)

    val ffOrder = for (workCards <- fWorkCard) yield {
      var orderSet = Set.empty[String]
      for (workCard <- workCards)
        orderSet += (workCard.orderId)

      Order.findOrders(orderSet.toSeq)
    }
    val fOrder = ffOrder.flatMap { x => x }

    for {
      dyeCardOpt <- fDyeCard
      workCards <- fWorkCard
      orders <- fOrder
    } yield {
      val dyeCard = dyeCardOpt.get
      val orderPair = orders map { order =>
        order._id -> order
      }
      Ok.sendFile(createItextPdf(dyeCardProc(dyeCard, workCards, orderPair.toMap)),
        fileName = _ =>
          play.utils.UriEncoding.encodePathSegment(s"${fileName}.pdf", "UTF-8"))
    }
  }

  def getWorkCardLabelByDyeCard(dyeCardId: String) = Security.Authenticated.async {
    import PdfUtility._
    val fileName = s"工作卡標籤"
    val fDyeCard = DyeCard.getCard(dyeCardId)
    val ffWorkCard = for (dyeCardOpt <- fDyeCard) yield {
      if (dyeCardOpt.isEmpty)
        Future(Seq.empty[WorkCard])
      else {
        val dyeCard = dyeCardOpt.get
        val fWorkCards = WorkCard.getCards(dyeCard.workIdList)
        for (workCards <- fWorkCards) yield workCards
      }
    }

    val fWorkCard = ffWorkCard.flatMap(f => f)
    val ffOrder = for (workCards <- fWorkCard) yield {
      var orderSet = Set.empty[String]
      for (workCard <- workCards)
        orderSet += (workCard.orderId)

      Order.findOrders(orderSet.toSeq)
    }
    val fOrder = ffOrder.flatMap { x => x }

    for {
      workCards <- fWorkCard
      orders <- fOrder
    } yield {
      val orderPair = orders map { order =>
        order._id -> order
      }

      Ok.sendFile(createWorkCardLabel(workCardLabelProc(workCards, orderPair.toMap)),
        fileName = _ =>
          play.utils.UriEncoding.encodePathSegment(s"${fileName}.pdf", "UTF-8"))
    }
  }

  def getBarcode(fileName: String) = Action {
    import play.api.Play.current
    import java.io.File
    import java.nio.file.Files

    Logger.debug(fileName)
    val msgArray = fileName.split('.')
    val path = current.path.getAbsolutePath + "/barcode/" + s"$fileName"
    val barcodeFile = new File(path)
    PdfUtility.createBarcode(barcodeFile, msgArray(0))
    Ok.sendFile(barcodeFile, fileName = _ =>
      play.utils.UriEncoding.encodePathSegment(fileName, "UTF-8"),
      onClose = () => { Files.deleteIfExists(barcodeFile.toPath()) })
  }

  def updateDyeCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[DyeCard]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, card => {
        card.updateTime = Some(DateTime.now().getMillis)
        card.active = false
        val f = DyeCard.updateCard(card)
        for (ret <- f) yield {
          Ok(Json.obj("ok" -> true))
        }
      })
  }

  def getStylingCard(workCardID: String) = Security.Authenticated.async {
    import WorkCard._
    val f = WorkCard.getCard(workCardID)
    for (workCardOpt <- f) yield {
      if (workCardOpt.isEmpty) {
        Results.NoContent
      } else {
        val workCard = workCardOpt.get
        Ok(Json.toJson(workCard.stylingCard))
      }
    }
  }

  def updateStylingCard(workCardID: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[StylingCard]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, card => {
        if (card.date == 0)
          card.date = DateTime.now().getMillis

        val f = WorkCard.updateStylingCard(workCardID, card)
        for (rets <- f) yield {
          val ret = rets(0)
          if (ret.getMatchedCount != 1)
            Ok(Json.obj("ok" -> false, "msg" -> "找不到工作卡!"))
          else if (ret.getModifiedCount != 1)
            Ok(Json.obj("ok" -> false, "msg" -> "未修改"))
          else
            Ok(Json.obj("ok" -> true))
        }
      })
  }

  case class GetTidyParam(workCardID: String, phase: String)
  case class GetTidyResp(quantity: Int, card: TidyCard)
  def getTidyCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val read = Json.reads[GetTidyParam]

      val result = request.body.validate[GetTidyParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        //verify workCard
        val fWorkCard = WorkCard.getCard(param.workCardID)
        for (workCardOpt <- fWorkCard) yield {
          if (workCardOpt.isEmpty)
            BadRequest("工作卡未登錄!")
          else {
            val workCard = workCardOpt.get
            val f = TidyCard.getTidyCard(param.workCardID, param.phase)
            val rets = waitReadyResult(f)
            val tidyResp =
              if (rets.isEmpty)
                GetTidyResp(workCard.quantity, TidyCard.default(param.workCardID, param.phase))
              else
                GetTidyResp(workCard.quantity, rets(0))

            implicit val write = Json.writes[GetTidyResp]
            Ok(Json.toJson(tidyResp))
          }
        }
      })
  }

  def getTidyCardList(workCardID: String) = Security.Authenticated.async {
    implicit request =>
      val f = TidyCard.getTidyCardOfWorkCard(workCardID)
      for (cards <- f) yield {
        Ok(Json.toJson(cards))
      }

  }

  def upsertTidyCard(activeStr: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val active = activeStr.toBoolean

      val result = request.body.validate[TidyCard]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, card => {
        card.date = DateTime.now.getMillis

        val f = TidyCard.upsertCard(card, active)

        for (rets <- f) yield {
          val ret = rets(0)
          Ok(Json.obj("ok" -> (ret.getModifiedCount == 1 || ret.getUpsertedId.isDocument() ||
            ret.getMatchedCount == 1)))
        }
      })
  }

  def getOrderDetailWorkCards(orderId: String, detailIndex: Int) = Security.Authenticated.async {
    val f = WorkCard.getOrderWorkCards(orderId, detailIndex)
    for (cards <- f) yield {
      Ok(Json.toJson(cards))
    }
  }
  

  import DyeCard._
  def queryDyeCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryDyeCardParam]
      val result = request.body.validate[QueryDyeCardParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = DyeCard.query(param)
          for (cardList <- f)
            yield Ok(Json.toJson(cardList))
        })
  }
  
  import WorkCard._
  def queryWorkCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val paramRead = Json.reads[QueryWorkCardParam]
      val result = request.body.validate[QueryWorkCardParam]
      result.fold(
        err =>
          Future {
            Logger.error(JsError.toJson(err).toString())
            BadRequest(JsError.toJson(err).toString())
          },
        param => {
          val f = WorkCard.query(param)
          for (cardList <- f)
            yield Ok(Json.toJson(cardList))
        })
  }

}