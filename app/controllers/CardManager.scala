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

  def getDyeCardPdf(id: String) = Security.Authenticated {
    import PdfUtility._
    val output = views.html.dyeCard("")
    val fileName = s"漂染$id"
    Ok.sendFile(creatPdfWithReportHeader(fileName, output),
      fileName = _ =>
        play.utils.UriEncoding.encodePathSegment(s"${fileName}.pdf", "UTF-8"))
  }

  def getDyeCardWeb(id: String) = Action {
    import PdfUtility._
    val output = views.html.dyeCard("")
    Ok(output)
  }
  
  def getBarcode(fileName:String) = Action {
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
        onClose = ()=>{Files.deleteIfExists(barcodeFile.toPath())})
  }
}