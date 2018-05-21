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
  def getDyeCardList(skip: Int, limit: Int) = Security.Authenticated.async {
    import DyeCard._
    val f = DyeCard.getActiveDyeCards(skip, limit)
    for (cards <- f)
      yield Ok(Json.toJson(cards))
  }

  def getDyeCardListCount = Security.Authenticated.async {
    import DyeCard._
    val f = DyeCard.getActiveDyeCardCount
    for (count <- f)
      yield Ok(Json.toJson(count))
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

  def deleteDyeCard(id: String) = Security.Authenticated.async {
    import DyeCard._
    val f = DyeCard.getCard(id)

    for (dyeCardOpt <- f) yield {
      if (dyeCardOpt.isEmpty)
        Ok(Json.obj("ok" -> false, "msg" -> "漂染卡不存在"))
      else {
        val dyeCard = dyeCardOpt.get
        for (workCardId <- dyeCard.workIdList) {
          WorkCard.deleteCard(workCardId)
        }

        DyeCard.deleteCard(id)
        Ok(Json.obj("ok" -> true))
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

  def updateWorkCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val result = request.body.validate[WorkCard]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, card => {
        val f = WorkCard.updateGoodAndActive(card._id, card.good, card.inventory.getOrElse(0), card.active)
        for (ret <- f) yield {
          Ok(Json.obj("ok" -> true))
        }
      })
  }

  def moveWorkCard(workCardId: String, moveOutDyeCardId: String, moveInDyeCardId: String) = Security.Authenticated.async {
    implicit request =>
      val dyeCardF = DyeCard.moveWorkCard(workCardId, moveOutDyeCardId, moveInDyeCardId)
      dyeCardF.onFailure(errorHandler)
      val workCardF = WorkCard.updateDyeCardId(workCardId, moveInDyeCardId)
      workCardF.onFailure(errorHandler)

      for (ret <- Future.sequence(List(dyeCardF, workCardF)))
        yield Ok(Json.obj("Ok" -> true))
  }

  def getWorkCardCount = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[Seq[String]]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, ids => {
        val f = WorkCard.countCards(ids)
        for (count <- f) yield {
          Ok(Json.toJson(count))
        }
      })
  }

  def getAllWorkCards = getWorkCards(0, 1000)

  def getWorkCards(skip: Int, limit: Int) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>

      val result = request.body.validate[Seq[String]]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, ids => {
        val f = WorkCard.getCards(ids)(skip, limit)
        for (cards <- f) yield {
          Ok(Json.toJson(cards))
        }
      })
  }

  def getActiveWorkCard(skip: Int, limit: Int) = Security.Authenticated.async {
    val f = WorkCard.getActiveWorkCard(skip, limit)
    for (workCards <- f)
      yield Ok(Json.toJson(workCards))
  }

  def getActiveWorkCardCount() = Security.Authenticated.async {
    val f = WorkCard.getActiveWorkCardCount()
    for (count <- f)
      yield Ok(Json.toJson(count))
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
        val fWorkCards = WorkCard.getCards(dyeCard.workIdList)(0, 1000)
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
      Ok.sendFile(
        createItextPdf(dyeCardProc(dyeCard, workCards, orderPair.toMap)),
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
        val fWorkCards = WorkCard.getCards(dyeCard.workIdList)(0, 1000)
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

      /*
      Ok.sendFile(createWorkCardLabel(workCardLabelProc(workCards, orderPair.toMap)),
        fileName = _ =>
          play.utils.UriEncoding.encodePathSegment(s"${fileName}.pdf", "UTF-8"))
          *
          */

      Ok.sendFile(
        createWorkSheet(workSheetProc(workCards, orderPair.toMap)),
        fileName = _ =>
          play.utils.UriEncoding.encodePathSegment(s"${fileName}.pdf", "UTF-8"))

    }
  }

  def getBarcode(fileName: String) = Action {
    import play.api.Play.current
    import java.io.File
    import java.nio.file.Files

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
        val f2 = DyeCard.markDyeCardFinished(workCardID)
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
  case class GetTidyResp(quantity: Int, inventory: Int, card: TidyCard)
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
                GetTidyResp(workCard.quantity, workCard.inventory.getOrElse(0), TidyCard.default(param.workCardID, param.phase))
              else
                GetTidyResp(workCard.quantity, workCard.inventory.getOrElse(0), rets(0))

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

  case class UpsertTidyParam(tidyCard: TidyCard, inventory: Int)
  def upsertTidyCard(activeStr: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      val active = activeStr.toBoolean

      implicit val reads = Json.reads[UpsertTidyParam]
      val result = request.body.validate[UpsertTidyParam]

      result.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {

        param.tidyCard.date = DateTime.now.getMillis

        val f = TidyCard.upsertCard(param.tidyCard, param.inventory, active)

        for (rets <- f) yield {
          if (rets.isEmpty) {
            Logger.warn("update is empty!")
            Ok(Json.obj("ok" -> false))
          } else {
            val ret = rets.head
            Ok(Json.obj("ok" -> (ret.getModifiedCount == 1 || ret.getUpsertedId.isDocument() ||
              ret.getMatchedCount == 1)))
          }
        }
      })
  }

  case class OrderProductionSummary(dyed: Int, produced: Int, inProduction: Int, overhead: Int)
  def getOrderDetailProductionSummary(orderId: String, detailIndex: Int) = Security.Authenticated.async {
    val f = WorkCard.getOrderWorkCards(orderId, detailIndex)
    val param = DyeCard.QueryDyeCardParam(
      _id = None,
      color = None,
      start = None, end = None, active = None, orderID = Some(orderId))
    val dyeCardF = DyeCard.query(param)(0, 100)
    for {
      cards <- f
      dyeCards <- dyeCardF
      dyeCardPairs = dyeCards map { card => card._id -> card }
      dyeCardMap = dyeCardPairs.toMap
    } yield {
      val dyed = cards.filter { card => !dyeCardMap(card.dyeCardID.get).active }.map { _.good }.sum
      val produced = cards.filter { !_.active }.map { _.good }.sum
      val inProduction = cards.filter { _.active }.map { _.good }.sum
      val overhead = cards.map { x => x.quantity - x.good }.sum

      implicit val writer = Json.writes[OrderProductionSummary]

      Ok(Json.toJson(OrderProductionSummary(dyed, produced, inProduction, overhead)))
    }
  }

  def getOrderProductionSummary(orderId: String) = Security.Authenticated.async {
    val f = WorkCard.getOrderProductionWorkCards(orderId)
    val param = DyeCard.QueryDyeCardParam(
      _id = None,
      color = None,
      start = None, end = None, active = None, orderID = Some(orderId))
    val dyeCardF = DyeCard.query(param)(0, 100)
    for {
      cards <- f
      dyeCards <- dyeCardF
      dyeCardPairs = dyeCards map { card => card._id -> card }
      dyeCardMap = dyeCardPairs.toMap
    } yield {
      val dyed = cards.filter { card => !dyeCardMap(card.dyeCardID.get).active }.map { _.good }.sum
      val produced = cards.filter { !_.active }.map { _.good }.sum
      val inProduction = cards.filter { _.active }.map { _.good }.sum
      val overhead = cards.map { x => x.quantity - x.good }.sum

      implicit val writer = Json.writes[OrderProductionSummary]

      Ok(Json.toJson(OrderProductionSummary(dyed, produced, inProduction, overhead)))
    }
  }

  import DyeCard._
  def queryDyeCard(skip: Int, limit: Int) = Security.Authenticated.async(BodyParsers.parse.json) {
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
          val f = DyeCard.query(param)(skip, limit)
          for (cardList <- f)
            yield Ok(Json.toJson(cardList))
        })
  }
  def queryDyeCardCount() = Security.Authenticated.async(BodyParsers.parse.json) {
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
          val f = DyeCard.count(param)
          for (count <- f)
            yield Ok(Json.toJson(count))
        })
  }

  import WorkCard._
  def queryWorkCard(skip: Int, limit: Int) = Security.Authenticated.async(BodyParsers.parse.json) {
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
          val f = WorkCard.query(param)(skip, limit)
          for (cardList <- f)
            yield Ok(Json.toJson(cardList))
        })
  }
  def queryWorkCardCount = Security.Authenticated.async(BodyParsers.parse.json) {
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
          val f = WorkCard.count(param)
          for (count <- f)
            yield Ok(Json.toJson(count))
        })
  }

  def tidyCardReport(startL: Long, endL: Long, output: String) = Security.Authenticated.async {
    val outputType = OutputType.withName(output)
    val (start, end) = (new DateTime(startL), new DateTime(endL))
    val f = TidyCard.queryCards(startL, endL)
    for (cards <- f) yield {
      if (outputType == OutputType.html)
        Ok(Json.toJson(cards))
      else {
        val workCardIdList = cards.map { _.workCardID }
        val workCardIdSet = Set(workCardIdList: _*)
        val workCardF = WorkCard.getCards(workCardIdSet.toSeq)(0, 1000)
        val workCards = waitReadyResult(workCardF)
        val workCardPair = workCards map { card => card._id -> card }
        val orderIdSet = Set(workCards.map { _.orderId }: _*)
        val ordersF = Order.getOrders(orderIdSet.toSeq)
        val orders = waitReadyResult(ordersF)
        val orderPair = orders map { order => order._id -> order }

        val excel = ExcelUtility.getTidyReport(cards, workCardPair.toMap, orderPair.toMap, start, end)
        Ok.sendFile(excel, fileName = _ =>
          play.utils.UriEncoding.encodePathSegment("整理報表" + start.toString("MMdd") + "_" + end.toString("MMdd") + ".xlsx", "UTF-8"))
      }
    }
  }

  case class StylingReport(cards: Seq[WorkCard], operatorList: Seq[String])
  def stylingReport(startL: Long, endL: Long, output: String) = Security.Authenticated.async {
    val outputType = OutputType.withName(output)
    val (start, end) = (new DateTime(startL), new DateTime(endL))

    val f = WorkCard.queryStylingCard(startL, endL)
    for (cards <- f) yield {
      var operatorSet = Set.empty[String]
      for {
        card <- cards
        stylingCard = card.stylingCard.get
      } {
        operatorSet ++= stylingCard.operator.flatMap { token => token.split("[,.]") }.toSet
      }
      val operatorList = operatorSet.toList.sorted

      if (outputType == OutputType.html) {
        val report = StylingReport(cards, operatorList)
        implicit val write = Json.writes[StylingReport]
        Ok(Json.toJson(report))
      } else {
        val orderIdSet = Set(cards.map { _.orderId }: _*)
        val ordersF = Order.getOrders(orderIdSet.toSeq)
        val orders = waitReadyResult(ordersF)
        val orderPair = orders map { order => order._id -> order }

        val excel = ExcelUtility.getStylingReport(cards, operatorList, orderPair.toMap, start, end)
        Ok.sendFile(excel, fileName = _ =>
          play.utils.UriEncoding.encodePathSegment("定型報表" + start.toString("MMdd") + "_" + end.toString("MMdd") + ".xlsx", "UTF-8"))
      }
    }
  }
  case class TransferDyeCardParam(_id: String, dep: String)
  def transferDyeCard = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val reads = Json.reads[TransferDyeCardParam]
      val ret = request.body.validate[TransferDyeCardParam]
      ret.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val validDep = List("WhiteTight", "DyeDep")
        if (!validDep.contains(param.dep)) {
          Future {
            val msg = s"Invalid dep=${param.dep}"
            Logger.error(msg)
            BadRequest(msg)
          }
        } else {
          val f = DyeCard.transferDep(param._id, param.dep)
          for (ret <- f) yield Ok(Json.obj("ok" -> true))
        }
      })
  }

  case class StartDyeParam(_id: String, operator: String)
  def startDye = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      implicit val read = Json.reads[StartDyeParam]
      val ret = request.body.validate[StartDyeParam]
      ret.fold(err => {
        Future {
          Logger.error(JsError.toJson(err).toString())
          BadRequest(JsError.toJson(err).toString())
        }
      }, param => {
        val f = DyeCard.startDye(param._id, param.operator)
        for (ret <- f) yield Ok(Json.obj("ok" -> true))
      })
  }
  def endDye(id: String) = Security.Authenticated.async {
    val f = DyeCard.endDye(id)
    for (ret <- f) yield Ok(Json.obj("ok" -> true))
  }
}