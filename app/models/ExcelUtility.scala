package models

import com.github.nscala_time.time.Imports.DateTime
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.{BorderStyle, Cell, HorizontalAlignment, IndexedColors}
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import play.api.Play.current

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

object ExcelUtility {
  val docRoot = "/report_template/"

  def getTidyReport(title: String, cardList: Seq[TidyCard], workCardMap: Map[String, WorkCard], orderMap: Map[String, Order],
                    start: DateTime, end: DateTime): File = {
    val (reportFilePath, pkg, wb) = prepareTemplate("tidyReport.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat();

    val sheet = wb.getSheetAt(0)
    val titleRow = sheet.getRow(0)
    titleRow.getCell(0).setCellValue(title)
    val timeRow = sheet.getRow(1)
    timeRow.createCell(1).setCellValue(start.toString("YY-MM-dd"))
    timeRow.createCell(3).setCellValue(end.toString("YY-MM-dd"))
    for {
      card_idx <- cardList.zipWithIndex
      card = card_idx._1
      rowN = card_idx._2 + 3
    } {
      val row = sheet.createRow(rowN)

      val workCard = workCardMap(card.workCardID)
      val order = orderMap(workCard.orderId)

      for (stylingDate <- card.stylingDate) {
        val date = new DateTime(stylingDate)
        row.createCell(0).setCellValue(date.toString("MM-dd"))
      }
      {
        val date = new DateTime(card.date)
        row.createCell(1).setCellValue(date.toString("MM-dd"))
      }
      for (finishDate <- card.finishDate) {
        val date = new DateTime(finishDate)
        row.createCell(2).setCellValue(date.toString("MM-dd"))
      }

      row.createCell(3).setCellValue(workCard.orderId)
      row.createCell(4).setCellValue(order.customerId)
      row.createCell(5).setCellValue(order.factoryId)
      row.createCell(6).setCellValue(order.details(workCard.detailIndex).color)
      row.createCell(7).setCellValue(order.details(workCard.detailIndex).size)
      row.createCell(8).setCellValue(order.name)
      row.createCell(9).setCellValue(card.workCardID)
      row.createCell(10).setCellValue(card.phase)
      row.createCell(11).setCellValue(toDozenStr(card.good))
      row.createCell(12).setCellValue(toDozenStr(card.sub))
      row.createCell(13).setCellValue(toDozenStr(card.stain))
      row.createCell(14).setCellValue(toDozenStr(card.broken))
      row.createCell(15).setCellValue(toDozenStr(card.subNotPack))
      row.createCell(16).setCellValue(card.operator)
    }

    val sumStyle = createSumStyle()(wb)
    val summaryRow = sheet.createRow(cardList.size + 4)
    def createSummaryCell(col:Int, content:String): Unit ={
      val cell = summaryRow.createCell(col)
      cell.setCellValue(content)
      cell.setCellStyle(sumStyle)
    }

    createSummaryCell(0, "加總")
    createSummaryCell(11, toDozenStr(cardList.map(_.good).sum))
    createSummaryCell(12, toDozenStr(cardList.map(_.sub.getOrElse(0)).sum))
    createSummaryCell(13, toDozenStr(cardList.map(_.stain.getOrElse(0)).sum))
    createSummaryCell(14, toDozenStr(cardList.map(_.broken.getOrElse(0)).sum))
    createSummaryCell(15, toDozenStr(cardList.map(_.subNotPack.getOrElse(0)).sum))

    finishExcel(reportFilePath, pkg, wb)
  }

  def getStylingReport(cardList: Seq[WorkCard], operatorList: List[String], orderMap: Map[String, Order],
                       start: DateTime, end: DateTime): File = {
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

  def toDozenStr(v: Option[Int]): String = {
    if (v.isEmpty)
      "-"
    else
      toDozenStr(v.get)
  }

  def getInventoryReport(inventories: Seq[Inventory], title: String): File = {
    val (reportFilePath, pkg, wb) = prepareTemplate("inventory.xlsx")
    val sheet = wb.getSheetAt(0)
    sheet.getRow(0).getCell(0).setCellValue(title)
    sheet.getRow(1).getCell(0).setCellValue(DateTime.now().toString("YYYY/MM/dd"))
    val cellStyle = createRightAlignStyle()(wb)

    val totalRow = inventories.size / 2 + (inventories.size % 2)
    for ((inventory, idx) <- inventories.zipWithIndex) {
      val rowN = if (idx < totalRow)
        idx + 3
      else
        idx - totalRow + 3

      val row = if (idx < totalRow)
        sheet.createRow(rowN)
      else
        sheet.getRow(rowN)

      def setInventory(col: Int, value: String): Unit = {
        val cell = row.createCell(col)
        cell.setCellStyle(cellStyle)
        cell.setCellValue(value)
      }

      val colStart = (idx / totalRow) * 5
      setInventory(colStart, inventory.factoryID)
      for (customerID <- inventory.customerID)
        setInventory(colStart + 1, customerID)

      setInventory(colStart + 2, inventory.color)
      setInventory(colStart + 3, inventory.size)
      setInventory(colStart + 4, toDozenStr(inventory.quantity))
    }
    val style = createSumStyle()(wb)

    val finalRowN = totalRow + 3
    val finalRow = sheet.createRow(finalRowN)
    for (i <- 0 to 9) {
      finalRow.createCell(i).setCellStyle(style)
    }

    sheet.addMergedRegion(new CellRangeAddress(finalRowN, finalRowN,
      0, 4))
    val totalCell = finalRow.getCell(0)
    totalCell.setCellValue("Total")
    totalCell.setCellStyle(style)
    val sum = inventories.map(_.quantity).sum
    sheet.addMergedRegion(new CellRangeAddress(finalRowN, finalRowN,
      5, 9))
    val sumCell = finalRow.getCell(5)
    sumCell.setCellStyle(style)
    sumCell.setCellValue(toDozenStr(sum))

    finishExcel(reportFilePath, pkg, wb)
  }

  private def prepareTemplate(templateFile: String): (Path, OPCPackage, XSSFWorkbook) = {
    val templatePath = Paths.get(current.path.getAbsolutePath + docRoot + templateFile)
    val reportFilePath = Files.createTempFile("temp", ".xlsx");

    Files.copy(templatePath, reportFilePath, StandardCopyOption.REPLACE_EXISTING)

    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(reportFilePath.toAbsolutePath.toString))
    val wb = new XSSFWorkbook(pkg);

    (reportFilePath, pkg, wb)
  }

  private def finishExcel(reportFilePath: Path, pkg: OPCPackage, wb: XSSFWorkbook): File = {
    val out = new FileOutputStream(reportFilePath.toAbsolutePath.toString);
    wb.write(out);
    out.close();
    pkg.close();

    new File(reportFilePath.toAbsolutePath.toString)
  }

  def toDozenStr(v: Int): String = {
    val dozen = v / 12
    val fractal = v % 12
    val dozenStr = "%d".format(dozen)
    if (fractal == 0)
      dozenStr
    else {
      val fractalStr = "%02d".format(fractal)
      s"$dozenStr.$fractalStr"
    }
  }

  private def createRightAlignStyle()(implicit wb: XSSFWorkbook) = {
    val style = wb.createCellStyle();
    val format = wb.createDataFormat();
    // Create a new font and alter it.
    val font = wb.createFont();
    font.setFontHeightInPoints(16);
    font.setFontName("標楷體");

    style.setFont(font)
    style.setAlignment(HorizontalAlignment.RIGHT)
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.BLACK.getIndex);
    style
  }

  private def createSumStyle()(implicit wb: XSSFWorkbook) = {
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
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderLeft(BorderStyle.DOUBLE);
    style.setLeftBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderRight(BorderStyle.DOUBLE);
    style.setRightBorderColor(IndexedColors.BLACK.getIndex);
    style.setBorderTop(BorderStyle.DOUBLE);
    style.setTopBorderColor(IndexedColors.BLACK.getIndex);
    style
  }

  def exportOrder(order: Order): File = {
    val (reportFilePath, pkg, wb) = prepareTemplate("order.xlsx")
    val sheet = wb.getSheetAt(0)
    var row = sheet.createRow(0)
    row.createCell(0).setCellValue("訂單明細")
    row = sheet.createRow(1)
    row.createCell(0).setCellValue("訂單編號:")
    row.createCell(1).setCellValue(order._id)
    row = sheet.createRow(2)
    row.createCell(0).setCellValue("品牌:")
    row.createCell(1).setCellValue(order.brand)
    row = sheet.createRow(3)
    row.createCell(0).setCellValue("品名:")
    row.createCell(1).setCellValue(order.name)
    row = sheet.createRow(4)
    row.createCell(0).setCellValue("工廠代號:")
    row.createCell(1).setCellValue(order.factoryId)
    row = sheet.createRow(5)
    row.createCell(0).setCellValue("客戶編號:")
    row.createCell(1).setCellValue(order.customerId)
    row = sheet.createRow(6)
    row.createCell(0).setCellValue("預定出貨日:")
    val expectedDeliverDate = new DateTime(order.expectedDeliverDate)
    row.createCell(1).setCellValue(expectedDeliverDate.toString("YYYY/MM/dd"))
    row = sheet.createRow(7)
    row.createCell(0).setCellValue("訂單數量:")
    val quantity = order.details.map {
      _.quantity
    }.sum
    row.createCell(1).setCellValue(toDozenStr(Some(quantity)) + "打")
    row = sheet.createRow(8)
    row.createCell(0).setCellValue("修正出貨日:")
    for(finalDeliverDateLong <- order.finalDeliverDate){
      val finalDeliverDate = new DateTime(finalDeliverDateLong)
      row.createCell(1).setCellValue(finalDeliverDate.toString("YYYY/MM/dd"))
    }

    row = sheet.createRow(9)
    row.createCell(0).setCellValue("訂單細項:")
    row = sheet.createRow(10)
    row.createCell(0).setCellValue("顏色")
    row.createCell(1).setCellValue("尺寸")
    row.createCell(2).setCellValue("數量(打)")
    for (detail <- order.details) {
      row = sheet.createRow(sheet.getLastRowNum + 1)
      row.createCell(0).setCellValue(detail.color)
      row.createCell(1).setCellValue(detail.size)
      row.createCell(2).setCellValue(s"${toDozenStr(Some(detail.quantity))}打")
    }
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("注意事項:")
    row.createCell(1).setCellValue("部門")
    row.createCell(2).setCellValue("內容")
    for(notice <- order.notices){
      row = sheet.createRow(sheet.getLastRowNum + 1)
      row.createCell(1).setCellValue(notice.department)
      row.createCell(2).setCellValue(notice.msg)
    }
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("採購包裝材料:")
    row = sheet.createRow(sheet.getLastRowNum + 1)
    val packageInfos =
      for {
        packageIdx <- order.packageInfo.packageOption.zipWithIndex
        packageOpt = packageIdx._1 if packageOpt
        idx = packageIdx._2
      } yield {
        val packageType = idx match {
          case 0 => "(v)環帶"
          case 1 => "(v)紙卡"
          case 2 => "(v)紙盒"
          case 3 => "(v)掛卡"
          case 4 => "(v)掛盒"
        }
        packageType
      }
    row.createCell(0).setCellValue(packageInfos.mkString(","))
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("包裝備註:")
    row.createCell(1).setCellValue(order.packageInfo.packageNote)
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("貼標:")
    val labelInfos =
      for {
        labelIdx <- order.packageInfo.labelOption.zipWithIndex
        label = labelIdx._1 if label
        idx = labelIdx._2
      } yield {

        val labelType = idx match {
          case 0 => "(v)成份標+Made in Taiwan"
          case 1 => "(v)價標"
          case 2 => "(v)條碼標"
          case 3 => "(v)型號標"
          case 4 => "(v)Size標"
        }
        labelType
      }
    row.createCell(0).setCellValue(labelInfos.mkString(","))
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("塑膠袋:")
    val bagInfo = {
      for {
        bagIdx <- order.packageInfo.bagOption.zipWithIndex
        bag = bagIdx._1 if bag
        idx = bagIdx._2
      } yield {
        val bagType = idx match {
          case 0 => "(v)單入OPP"
          case 1 => "(v)單入PVC"
          case 2 => "(v)自黏"
          case 3 => "(v)高週波"
          case 4 => "(v)彩印"
          case 5 => "(v)掛孔"
        }

        if (idx == 1)
          s"$bagType-(${order.packageInfo.pvcNote})"
        else
          bagType
      }
    }
    row.createCell(1).setCellValue(bagInfo.mkString(","))
    for(numInBag <- order.packageInfo.numInBag){
      row = sheet.createRow(sheet.getLastRowNum + 1)
      row.createCell(1).setCellValue(s"${numInBag}雙入大袋")
    }
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(1).setCellValue(order.packageInfo.bagNote)
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("外銷箱:")
    for {
      boxIdx <-order.packageInfo.exportBoxOption.zipWithIndex
      box = boxIdx._1 if box
      idx = boxIdx._2
    } {
      val boxType = idx match {
        case 0 => "(v)內盒"
        case 1 => "(v)外箱"
      }
      row = sheet.createRow(sheet.getLastRowNum + 1)
      row.createCell(1).setCellValue(s"$boxType-(${order.packageInfo.exportBoxNote(idx)})")
    }
    row = sheet.createRow(sheet.getLastRowNum + 1)
    row.createCell(0).setCellValue("嘜頭:")
    row.createCell(1).setCellValue(order.packageInfo.ShippingMark)
    finishExcel(reportFilePath, pkg, wb)
  }
}
