package controllers
import play.api._
import play.api.Play.current
import controllers._
import models._
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import org.apache.poi.ss.usermodel._

object ExcelUtility {
  val docRoot = "/report_template/"

  private def prepareTemplate(templateFile: String) = {
    val templatePath = Paths.get(current.path.getAbsolutePath + docRoot + templateFile)
    val reportFilePath = Files.createTempFile("temp", ".xlsx");

    Files.copy(templatePath, reportFilePath, StandardCopyOption.REPLACE_EXISTING)

    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(reportFilePath.toAbsolutePath().toString()))
    val wb = new XSSFWorkbook(pkg);

    (reportFilePath, pkg, wb)
  }

  def finishExcel(reportFilePath: Path, pkg: OPCPackage, wb: XSSFWorkbook) = {
    val out = new FileOutputStream(reportFilePath.toAbsolutePath().toString());
    wb.write(out);
    out.close();
    pkg.close();

    new File(reportFilePath.toAbsolutePath().toString())
  }
  
  def toDozenStr(v:Option[Int]):String={
    if(v.isEmpty)
      "-"
    else
      toDozenStr(v.get)
  }
  
  def toDozenStr(v:Int)={
    val dozen = v/12
    val fract = v%12
    val dozenStr = "%d".format(dozen)
    if(fract == 0)
      dozenStr
    else{
      val fractStr = "%02d".format(fract)
      s"$dozenStr.$fractStr"
    }
  }
  def getTidyReport(cardList:Seq[TidyCard], workCardMap:Map[String, WorkCard], orderMap:Map[String, Order], 
      start:DateTime, end:DateTime) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("tidyReport.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat();

    val sheet = wb.getSheetAt(0)
    val timeRow = sheet.getRow(1)
    timeRow.createCell(1).setCellValue(start.toString("YY-MM-dd"))
    timeRow.createCell(3).setCellValue(end.toString("YY-MM-dd"))
    for{card_idx<-cardList.zipWithIndex
      card = card_idx._1
      rowN = card_idx._2 + 3
      }{
      val row = sheet.createRow(rowN)
      val date = new DateTime(card.date)
      row.createCell(0).setCellValue(date.toString("MM-dd"))
      row.createCell(1).setCellValue(workCardMap(card.workCardID).orderId)
      row.createCell(2).setCellValue(orderMap(workCardMap(card.workCardID).orderId).name)
      row.createCell(3).setCellValue(card.workCardID)
      row.createCell(4).setCellValue(card.phase)
      row.createCell(5).setCellValue(toDozenStr(card.good))
      row.createCell(6).setCellValue(toDozenStr(card.sub))
      row.createCell(7).setCellValue(toDozenStr(card.stain))
      row.createCell(8).setCellValue(toDozenStr(card.broken))
      row.createCell(9).setCellValue(toDozenStr(card.subNotPack))
      row.createCell(10).setCellValue(card.operator)
    }
    
    finishExcel(reportFilePath, pkg, wb)
  }
}