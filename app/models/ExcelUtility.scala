package models

import com.github.nscala_time.time.Imports.DateTime
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.{BorderStyle, HorizontalAlignment, IndexedColors}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import play.api.Play.current

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

object ExcelUtility {
  val docRoot = "/report_template/"

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
      stylingDate <- card.stylingDate
      rowN = card_idx._2 + 3
    } {
      val row = sheet.createRow(rowN)
      val date = new DateTime(stylingDate)
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
      card <- workCard.stylingCard
      stylingDate <- workCard.stylingDate
      rowN = card_idx._2 + 3
    } {
      val row = sheet.createRow(rowN)
      val date = new DateTime(stylingDate)
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
        operator_idx <- operatorList.flatMap { x =>
          x.split("[,.]")
        }.zipWithIndex
        operator = operator_idx._1
        idx = operator_idx._2
        col = 13 + idx
      } {
        val cell = row.createCell(col)
        if (card.operator.flatMap { x => x.split("[,.]") }.contains(operator))
          cell.setCellValue(operator)
      }
    }

    finishExcel(reportFilePath, pkg, wb)
  }

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

  def getInventoryReport(inventories: Seq[Inventory], title: String) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("inventory.xlsx")
    val sheet = wb.getSheetAt(0)
    sheet.getRow(0).getCell(0).setCellValue(title)
    sheet.getRow(1).getCell(0).setCellValue(DateTime.now().toString("YYYY/MM/dd"))
    val cellStyle = createRightAlignStyle()(wb)

    for ((inventory, idx) <- inventories.zipWithIndex) {
      val rowN = idx / 3 + 3
      val row = if (idx % 3 == 0)
        sheet.createRow(rowN)
      else
        sheet.getRow(rowN)

      def setInventory(col: Int, value: String): Unit = {
        val cell = row.createCell(col)
        cell.setCellStyle(cellStyle)
        cell.setCellValue(value)
      }

      val colStart = (idx % 3) * 5
      setInventory(colStart, inventory.factoryID)
      for(customerID <-inventory.customerID)
        setInventory(colStart + 1, customerID)

      setInventory(colStart + 2, inventory.color)
      setInventory(colStart + 3, inventory.size)
      setInventory(colStart + 4, toDozenStr(inventory.quantity))
    }
    val style = createSumStyle()(wb)
    val finalRowN = if(inventories.size % 3 == 0)
      inventories.size / 3 + 3
    else
      inventories.size / 3 + 4
    val finalRow = sheet.createRow(finalRowN)
    for(i <- 0 to 14){
      finalRow.createCell(i).setCellStyle(style)
    }

    sheet.addMergedRegion(new CellRangeAddress(finalRowN, finalRowN,
      0, 7))
    val totalCell = finalRow.getCell(0)
    totalCell.setCellValue("Total")
    totalCell.setCellStyle(style)
    val sum = inventories.map(_.quantity).sum
    sheet.addMergedRegion(new CellRangeAddress(finalRowN, finalRowN,
      8, 14))
    val sumCell = finalRow.getCell(8)
    sumCell.setCellStyle(style)
    sumCell.setCellValue(toDozenStr(sum))

    finishExcel(reportFilePath, pkg, wb)
  }

  def createRightAlignStyle()(implicit wb: XSSFWorkbook) = {
    val style = wb.createCellStyle();
    val format = wb.createDataFormat();
    // Create a new font and alter it.
    val font = wb.createFont();
    font.setFontHeightInPoints(16);
    font.setFontName("標楷體");

    style.setFont(font)
    style.setAlignment(HorizontalAlignment.RIGHT)
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.BLACK.getIndex());
    style
  }

  def createSumStyle()(implicit wb: XSSFWorkbook) = {
    val style = wb.createCellStyle();
    val format = wb.createDataFormat();
    // Create a new font and alter it.
    val font = wb.createFont();
    font.setFontHeightInPoints(20);
    font.setFontName("標楷體");
    font.setBold(true)

    style.setFont(font)
    style.setAlignment(HorizontalAlignment.CENTER)
    style.setBorderBottom(BorderStyle.DOUBLE);
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderLeft(BorderStyle.DOUBLE);
    style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderRight(BorderStyle.DOUBLE);
    style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderTop(BorderStyle.DOUBLE);
    style.setTopBorderColor(IndexedColors.BLACK.getIndex());
    style
  }
}
