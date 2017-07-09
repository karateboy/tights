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

  def toDozenStr(v: Option[Int]): String = {
    if (v.isEmpty)
      "-"
    else
      toDozenStr(v.get)
  }

  def toDozenStr(v: Int) = {
    val dozen = v / 12
    val fract = v % 12
    val dozenStr = "%d".format(dozen)
    if (fract == 0)
      dozenStr
    else {
      val fractStr = "%02d".format(fract)
      s"$dozenStr.$fractStr"
    }
  }
  def getTidyReport(cardList: Seq[TidyCard], workCardMap: Map[String, WorkCard], orderMap: Map[String, Order],
                    start: DateTime, end: DateTime) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("tidyReport.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat();

    val sheet = wb.getSheetAt(0)
    val timeRow = sheet.getRow(1)
    timeRow.createCell(1).setCellValue(start.toString("YY-MM-dd"))
    timeRow.createCell(3).setCellValue(end.toString("YY-MM-dd"))
    for {
      card_idx <- cardList.zipWithIndex
      card = card_idx._1
      rowN = card_idx._2 + 3
    } {
      val row = sheet.createRow(rowN)
      val date = new DateTime(card.date)
      val workCard = workCardMap(card.workCardID)
      val order = orderMap(workCard.orderId)
      
      row.createCell(0).setCellValue(date.toString("MM-dd"))
      row.createCell(1).setCellValue(workCard.orderId)
      row.createCell(2).setCellValue(order.customerId)
      row.createCell(3).setCellValue(order.factoryId)
      row.createCell(4).setCellValue(order.details(workCard.detailIndex).color)
      row.createCell(5).setCellValue(order.details(workCard.detailIndex).size)
      row.createCell(6).setCellValue(order.name)
      row.createCell(7).setCellValue(card.workCardID)
      row.createCell(8).setCellValue(card.phase)
      row.createCell(9).setCellValue(toDozenStr(card.good))
      row.createCell(10).setCellValue(toDozenStr(card.sub))
      row.createCell(11).setCellValue(toDozenStr(card.stain))
      row.createCell(12).setCellValue(toDozenStr(card.broken))
      row.createCell(13).setCellValue(toDozenStr(card.subNotPack))
      row.createCell(14).setCellValue(card.operator)
    }

    finishExcel(reportFilePath, pkg, wb)
  }

  def getStylingReport(cardList: Seq[WorkCard], operatorList: List[String], orderMap: Map[String, Order],
                       start: DateTime, end: DateTime) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("stylingReport.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat();

    val sheet = wb.getSheetAt(0)
    val timeRow = sheet.getRow(1)
    timeRow.createCell(1).setCellValue(start.toString("YY-MM-dd"))
    timeRow.createCell(3).setCellValue(end.toString("YY-MM-dd"))
    val titleRow = sheet.getRow(2)
    for {
      operator_idx <- operatorList.zipWithIndex
      operator = operator_idx._1
      idx = operator_idx._2
      col = 13 + idx
    } {
      titleRow.createCell(col).setCellValue(operator)
    }

    for {
      card_idx <- cardList.zipWithIndex
      workCard = card_idx._1
      card = workCard.stylingCard.get
      rowN = card_idx._2 + 3
    } {
      val row = sheet.createRow(rowN)
      val date = new DateTime(card.date)
      val order = orderMap(workCard.orderId)
      row.createCell(0).setCellValue(date.toString("MM-dd"))
      row.createCell(1).setCellValue(workCard.orderId)
      row.createCell(2).setCellValue(order.name)
      row.createCell(3).setCellValue(workCard._id)
      row.createCell(4).setCellValue(order.customerId)
      row.createCell(5).setCellValue(order.factoryId)
      row.createCell(6).setCellValue(order.details(workCard.detailIndex).color)
      row.createCell(7).setCellValue(order.details(workCard.detailIndex).size)      
      row.createCell(8).setCellValue(toDozenStr(card.good))
      row.createCell(9).setCellValue(toDozenStr(card.sub))
      row.createCell(10).setCellValue(toDozenStr(card.stain))
      row.createCell(11).setCellValue(toDozenStr(card.broken))
      row.createCell(12).setCellValue(toDozenStr(card.notEven))

      for {
        operator_idx <- operatorList.zipWithIndex
        operator = operator_idx._1
        idx = operator_idx._2
        col = 13 + idx
      } {
        val cell = row.createCell(col)
        if(card.operator.contains(operator))
          cell.setCellValue(operator)
      }
    }

    finishExcel(reportFilePath, pkg, wb)
  }

}