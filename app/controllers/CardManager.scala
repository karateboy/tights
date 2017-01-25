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

  def getWorkCard = Security.Authenticated.async(BodyParsers.parse.json) {
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
        val f = DyeCard.updateCard(card)
        for (ret <- f) yield {
          Ok(Json.obj("ok" -> true))
        }
      })
  }

  def getStylingCard(workCardID: String) = Security.Authenticated.async {
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
        val f = TidyCard.getTidyCard(param.workCardID, param.phase)
        for (rets <- f) yield {
          if (rets.isEmpty)
            Ok(Json.toJson(TidyCard.default(param.workCardID, param.phase)))
          else
            Ok(Json.toJson(rets(0)))
        }
      })
  }
}