package models
import play.api._
import play.api.mvc._
import play.api.Play.current
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.PageSize
import com.itextpdf.tool.xml.XMLWorker
import com.itextpdf.tool.xml.XMLWorkerFontProvider
import com.itextpdf.tool.xml.XMLWorkerHelper
import com.itextpdf.tool.xml.net._
import com.itextpdf.tool.xml.html.CssAppliersImpl
import com.itextpdf.tool.xml.html.Tags
import com.itextpdf.tool.xml.parser.XMLParser
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.github.nscala_time.time.Imports._

/**
 * @author user
 */
object PdfUtility {
  val CSS_ROOT = "/public/"

  def createPdf(htmlInput: String, landscape: Boolean = true) = {

    //debug
    import java.io.FileOutputStream
    import java.nio.charset.Charset
    //val outs = new FileOutputStream("D:/temp/output.html")
    //outs.write(htmlInput.getBytes(Charset.forName("UTF-8")))
    //outs.close()

    // step 1
    val document =
      if (landscape)
        new Document(PageSize.A4.rotate());
      else
        new Document(PageSize.A4);

    // step 2
    import java.io._
    import java.nio.charset.Charset

    val tempFile = File.createTempFile("report", ".pdf")
    val writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));

    // step 3
    document.open();

    // step 4

    // CSS
    val cssResolver =
      XMLWorkerHelper.getInstance().getDefaultCssResolver(false);
    val bootstrapCss = XMLWorkerHelper.getCSS(new FileInputStream(current.path + CSS_ROOT + "css/bootstrap.min.css"))
    cssResolver.addCss(bootstrapCss)

    //val styleCss = XMLWorkerHelper.getCSS(new FileInputStream(current.path + CSS_ROOT +"css/style.css"))
    //cssResolver.addCss(styleCss)

    //val aqmCss = XMLWorkerHelper.getCSS(new FileInputStream(current.path + CSS_ROOT +"css/aqm.css"))
    //cssResolver.addCss(aqmCss)

    // HTML
    val fontProvider = new XMLWorkerFontProvider();
    val cssAppliers = new CssAppliersImpl(fontProvider);
    val htmlContext = new HtmlPipelineContext(cssAppliers);
    htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());

    // Pipelines
    val pdf = new PdfWriterPipeline(document, writer);
    val html = new HtmlPipeline(htmlContext, pdf);
    val css = new CssResolverPipeline(cssResolver, html);

    // XML Worker
    val worker = new XMLWorker(css, true);
    val p = new XMLParser(worker);
    val charSet = Charset.forName("UTF-8")
    p.parse(new ByteArrayInputStream(htmlInput.getBytes(charSet)), charSet)

    // step 5
    document.close()

    tempFile
  }

  import com.itextpdf.text.pdf._
  import com.itextpdf.text._

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

  def dyeCardProc(dyeCard: DyeCard, workSeq: Seq[WorkCard], orderMap: Map[String, Order])(doc: Document, writer: PdfWriter) {
    val bf = BaseFont.createFont("C:/Windows/Fonts/mingliu.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    val defaultFont = new Font(bf, 12)
    val bigFont = new Font(bf, 18, Font.BOLD)
    
    def prepareCell(str: String, add: Boolean = true, colspan:Int = 1)(implicit tab: PdfPTable, font:Font = defaultFont) = {
      val cell = new PdfPCell(new Paragraph(str, font))
      cell.setHorizontalAlignment(Element.ALIGN_LEFT)
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE)
      cell.setPaddingBottom(8)
      if(colspan != 1)
        cell.setColspan(colspan)
        
      if (add)
        tab.addCell(cell)

      cell
    }
    
    
    val sizeList = workSeq.map {
      work =>
        val order = orderMap(work.orderId)
        order.details(work.detailIndex).size
    }

    {
      implicit val topTable = new PdfPTable(4); // 3 columns.
      topTable.setWidthPercentage(100)
      val orderStr = orderMap.keys.mkString("\n")
      prepareCell("訂單編號:\n" + orderStr)(topTable, bigFont)
      val deliverDate = new DateTime(orderMap.values.map { _.expectedDeliverDate }.min)
      prepareCell("出貨日:\n" + deliverDate.toString("YYYY-MM-dd"))(topTable, bigFont)
            prepareCell("顏色:\n" + dyeCard.color)(topTable, bigFont)
      topTable.addCell(getBarCodeImg(dyeCard._id)(writer))
      
      val brandSet = {orderMap.values map {order => order.brand}}.toSet
      val brandString = brandSet.mkString("\n")  
      val quantityList = workSeq.map { _.quantity }
      prepareCell("包襪人員:")
      prepareCell("編織批號:")
      prepareCell("總數量(打):" + toDozenStr(quantityList.sum))
      prepareCell("")
      prepareCell("包襪日期:")
      prepareCell("品牌:" + brandString)
      prepareCell("備註:")
      prepareCell(dyeCard.remark.getOrElse(""))

      

      doc.add(topTable)
    }
    {
      implicit val tab2 = new PdfPTable(10)
      tab2.setSpacingBefore(12f)
      tab2.setWidthPercentage(100)

      prepareCell("品名", true, 2)
      prepareCell("客戶編號")
      prepareCell("工廠代號", true, 2)
      prepareCell("流動卡號")
      prepareCell("尺寸")
      prepareCell("打數")
      prepareCell("包數")
      prepareCell("襪袋")
      
      for (workCard <- workSeq) {
        val order = orderMap(workCard.orderId)
        prepareCell(order.name, true, 2)
        prepareCell(order.customerId)
        prepareCell(order.factoryId, true, 2)
        prepareCell(workCard._id)
        prepareCell(order.details(workCard.detailIndex).size)
        prepareCell(toDozenStr(workCard.quantity))
        prepareCell(" ")        
        prepareCell(" ")
      }
      tab2.setSpacingBefore(12f)
      doc.add(tab2)
    }

    {
      implicit val tab3 = new PdfPTable(4)
      tab3.setSpacingBefore(12f)
      tab3.setWidthPercentage(100)
      prepareCell("染色人員:")
      prepareCell("染色日期:")
      prepareCell("染鍋:")
      prepareCell("襪重:")
      val r1 = prepareCell("精煉程序(kg):", false)
      r1.setRowspan(2)
      tab3.addCell(r1)
      val r2 = prepareCell("精煉劑:____kg\n(環保,特用,LYS,其他)", false)
      r2.setRowspan(2)
      tab3.addCell(r2)
      val r3 = prepareCell("乳化劑:____kg", false)
      r3.setRowspan(2)
      tab3.addCell(r3)
      prepareCell("溫度:__C")
      prepareCell("時間:__分鐘")

      val p1 = prepareCell("染色藥劑(g):", false)
      p1.setRowspan(3)
      tab3.addCell(p1)
      prepareCell("Y:")
      prepareCell(" ")
      prepareCell("螢光劑:")
      prepareCell("R:")
      prepareCell(" ")
      prepareCell("增白劑:")
      prepareCell("B:")
      prepareCell(" ")
      prepareCell(" ")

      val dp = prepareCell("染色程序(kg):", false)
      dp.setRowspan(3)
      tab3.addCell(dp)
      prepareCell("均染劑:____kg\n(一般,PAM,其他)")
      prepareCell("冰醋酸:____kg")
      prepareCell("溫度:____C")
      prepareCell("醋銨:____kg")
      prepareCell("氨水:____kg")
      prepareCell("時間:____分鐘")
      prepareCell("起染pH:")
      prepareCell("染終pH:")
      prepareCell("溫度:____C")

      val pp = prepareCell("後處理程序(kg):", false)
      pp.setRowspan(2)
      tab3.addCell(pp)
      prepareCell("固色劑:____kg")
      prepareCell("陽離子柔軟劑:____kg")
      prepareCell("柔軟時間:____分鐘")
      prepareCell("冰醋酸:____kg")
      prepareCell("矽利康:____kg")
      prepareCell("溫度:____C")

      prepareCell("烘乾")
      prepareCell("溫度:")
      prepareCell("時間:")
      prepareCell("機台:")

      doc.add(tab3)
    }

    {
      implicit val tab = new PdfPTable(6)
      tab.setSpacingBefore(12f)
      tab.setWidthPercentage(100)
      prepareCell("尺寸")
      prepareCell("染前拉量(cm)")
      prepareCell("染後拉量(cm)")
      prepareCell("尺寸")
      prepareCell("染前拉量(cm)")
      prepareCell("染後拉量(cm)")

      for (size <- sizeList) {
        prepareCell(size)
        prepareCell("")
        prepareCell("")
      }
      if(sizeList.length % 2 == 1){
        prepareCell("")
        prepareCell("")
        prepareCell("")
      }
      doc.add(tab)
    }
  }

  def createItextPdf(proc: (Document, PdfWriter) => Unit) = {
    import java.io.FileOutputStream
    import java.nio.charset.Charset
    import java.io._
    import java.nio.charset.Charset

    val document = new Document(PageSize.A4);
    val tempFile = File.createTempFile("dyeCard", ".pdf")
    val writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));

    document.open();
    proc(document, writer)
    document.close()

    tempFile
  }

  def orderProc(order: Order)(doc: Document, writer: PdfWriter) {
    val bf = BaseFont.createFont("C:/Windows/Fonts/mingliu.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    val font = new Font(bf, 12)
    def prepareCell(str: String, colspan: Int = 1, add: Boolean = true)(implicit tab: PdfPTable) = {
      val cell = new PdfPCell(new Paragraph(str, font))
      cell.setHorizontalAlignment(Element.ALIGN_LEFT)
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE)
      cell.setPaddingBottom(8)
      if (colspan != 1)
        cell.setColspan(colspan)

      if (add)
        tab.addCell(cell)

      cell
    }
    def showOptDate(millisOpt: Option[Long]) = {
      val strOpt = millisOpt map {
        millis => new DateTime(millis).toString("YYYY/MM/dd")
      }

      strOpt.getOrElse("")
    }

    val user = User.getUserByEmail(order.salesId)
    val p1 = new Paragraph(
      s"外銷內部工作單", new Font(bf, 18))
    p1.setAlignment(Element.ALIGN_CENTER)
    doc.add(p1)
    val p2 = new Paragraph(s"訂單編號:${order._id} ${user.get.name}", new Font(bf, 12))
    p2.setAlignment(Element.ALIGN_RIGHT)
    doc.add(p2)

    {
      implicit val topTable = new PdfPTable(6); // 6 columns.
      topTable.setWidthPercentage(100)
      topTable.setSpacingBefore(12f)
      prepareCell("品名:")
      prepareCell(order.name)
      prepareCell("白襪庫存:")
      prepareCell("")
      prepareCell("預定出貨日:")
      val expectedDeliverDate = new DateTime(order.expectedDeliverDate)
      prepareCell(expectedDeliverDate.toString("YYYY/MM/dd"))
      prepareCell("工廠代號:")
      prepareCell(order.factoryId)
      prepareCell("訂單數量:")
      val quantity = order.details.map { _.quantity }.sum
      prepareCell(toDozenStr(Some(quantity)) + "打")
      prepareCell("修正出貨日:")
      prepareCell(showOptDate(order.finalDeliverDate))
      prepareCell("客戶編號:")
      prepareCell(order.customerId)
      prepareCell("品牌:")
      prepareCell(order.brand)
      prepareCell("通知日期:")
      prepareCell(showOptDate(order.date))

      doc.add(topTable)
    }

    { //Order details
      implicit val tab = new PdfPTable(3); // 6 columns.
      tab.setWidthPercentage(100)
      tab.setSpacingBefore(12f)
      prepareCell("顏色")
      prepareCell("尺寸")
      prepareCell("數量(打)")
      for (detail <- order.details) {
        prepareCell(detail.color)
        prepareCell(detail.size)
        prepareCell(toDozenStr(detail.quantity) + "打")
      }
      doc.add(tab)
    }

    {
      implicit val tab = new PdfPTable(8); // 6 columns.
      tab.setWidthPercentage(100)
      tab.setSpacingBefore(12f)
      prepareCell("部門")
      prepareCell("工作天")
      prepareCell("預定交期")
      prepareCell("部門主管")
      prepareCell("注意事項", 4)

      for (notice <- order.notices) {
        prepareCell(Department.map(Department.withName(notice.department)))
        prepareCell("")
        prepareCell("")
        prepareCell("")
        prepareCell(notice.msg, 4)
      }
      doc.add(tab)
    }
    {
      val packageInfo = order.packageInfo
      implicit val tab = new PdfPTable(4); // 6 columns.
      tab.setWidthPercentage(100)
      prepareCell("採購部包裝材料", 3)
      prepareCell("預定進廠")
      val packageInfos =
        for {
          packageIdx <- packageInfo.packageOption.zipWithIndex
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
      prepareCell(packageInfos.mkString("\n"), 3)
      prepareCell("")

      prepareCell(packageInfo.packageNote, 3)
      prepareCell("")

      prepareCell("貼標:", 3)
      prepareCell("")
      val labelInfos =
        for {
          labelIdx <- packageInfo.labelOption.zipWithIndex
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
      prepareCell(labelInfos.mkString(" "), 3)
      prepareCell("")

      val cardInfos =
        for {
          cardIdx <- packageInfo.cardOption.zipWithIndex
          card = cardIdx._1 if card
          idx = cardIdx._2
        } yield {
          val cardType = idx match {
            case 0 => "(v)撐卡"
            case 1 => "(v)襯卡"
            case 2 => "(v)掛勾"
            case 3 => "(v)洗標"
          }
          cardType + packageInfo.cardNote(idx)
        }
      prepareCell(cardInfos.mkString("\n"), 3)
      prepareCell("")

      prepareCell("塑膠袋:", 3)
      prepareCell("")

      val bagInfo =
        for {
          bagIdx <- packageInfo.bagOption.zipWithIndex
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
            s"$bagType-(${packageInfo.pvcNote})"
          else
            bagType
        }
      prepareCell(bagInfo.mkString("\n"), 3)
      prepareCell("")
      if (packageInfo.numInBag.isDefined) {
        prepareCell(s"${packageInfo.numInBag.get}雙入大袋", 3)
        prepareCell("")
      }
      prepareCell(packageInfo.bagNote, 3)
      prepareCell("")

      prepareCell("外銷箱:", 3)
      prepareCell("")
      for {
        boxIdx <- packageInfo.exportBoxOption.zipWithIndex
        box = boxIdx._1 if box
        idx = boxIdx._2
      } {
        val boxType = idx match {
          case 0 => "(v)內盒"
          case 1 => "(v)外箱"
        }
        prepareCell(boxType + "-" + packageInfo.exportBoxNote(idx), 3)
        prepareCell("")
      }

      val mainTab = new PdfPTable(2)
      mainTab.setWidthPercentage(100)
      mainTab.setSpacingBefore(12f)

      {
        implicit val tab2 = new PdfPTable(1)
        prepareCell("嘜頭:")(tab2)
        prepareCell(packageInfo.ShippingMark)(tab2)
        if (packageInfo.extraNote.isDefined) {
          prepareCell("備註欄:")(tab2)
          prepareCell(packageInfo.extraNote.get)(tab2)
        }
        val cell1 = new PdfPCell(tab)
        cell1.setBorder(Rectangle.NO_BORDER)
        val cell2 = new PdfPCell(tab2)
        cell2.setBorder(Rectangle.NO_BORDER)
        mainTab.addCell(cell1)
        mainTab.addCell(cell2)
      }

      doc.add(mainTab)
    }
  }

  def getBarCodeImg(code: String)(implicit writer: PdfWriter) = {
    val code128 = new Barcode128()
    code128.setCodeType(Barcode.CODE128_UCC)
    code128.setCode(code)
    val bar2Img = code128.createImageWithBarcode(writer.getDirectContent, BaseColor.BLACK, BaseColor.GRAY)
    bar2Img.setAlignment(Element.ALIGN_MIDDLE)
    bar2Img
  }

  def workSheetProc(workSeq: Seq[WorkCard], orderMap: Map[String, Order])(doc: Document, writer: PdfWriter) {
    val bf = BaseFont.createFont("C:/Windows/Fonts/mingliu.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    val font = new Font(bf, 13)
    def prepareCell(str: String, colspan: Int = 1, add: Boolean = true)(implicit tab: PdfPTable) = {
      val cell = new PdfPCell(new Paragraph(str, font))
      cell.setHorizontalAlignment(Element.ALIGN_LEFT)
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE)
      cell.setPaddingBottom(8)
      cell.setColspan(colspan)
      if (add)
        tab.addCell(cell)

      cell
    }

    def prepareSheet(workCard: WorkCard) = {
      val sheetTab = new PdfPTable(8)
      sheetTab.setWidthPercentage(100)
      def whiteSock() {
        implicit val tab = new PdfPTable(1)
        tab.setSpacingBefore(6f)
        tab.setWidthPercentage(100)

        tab.addCell(new PdfPCell(getBarCodeImg(workCard._id)(writer)))

        prepareCell("單位:白襪課")
        prepareCell("訂單:\n" + workCard.orderId)
        val order = orderMap(workCard.orderId)
        prepareCell("工廠代號:\n" + order.factoryId)
        prepareCell("客戶編號:\n" + order.customerId)
        prepareCell("尺寸:" + order.details(workCard.detailIndex).size)
        prepareCell("顏色:" + order.details(workCard.detailIndex).color)
        prepareCell("數量:" + toDozenStr(workCard.quantity))
        prepareCell("日期:")

        val tab2 = new PdfPTable(8)
        tab2.setWidthPercentage(100)
        prepareCell("編卡號碼", 4)(tab2)
        prepareCell("數量",2)(tab2)
        prepareCell("工號",2)(tab2)
        for (i <- 1 to 6) {
          prepareCell(" ", 4)(tab2)
          prepareCell(" ", 2)(tab2)
          prepareCell(" ", 2)(tab2)
        }
        val cell1 = new PdfPCell(tab2)
        cell1.setBorder(Rectangle.NO_BORDER)
        cell1.setRowspan(7)
        tab.addCell(cell1)
        
        prepareCell("備註:")
        prepareCell("主管審核:")

        val cell = new PdfPCell(tab)
        cell.setBorder(Rectangle.NO_BORDER)
        cell.setPadding(5f)
        sheetTab.addCell(cell)
      }

      def stylingTidy() {
        val cardTab = new PdfPTable(1)
        cardTab.setSpacingBefore(6f)
        cardTab.setWidthPercentage(100)

        //header
        {
          implicit val tab = new PdfPTable(1)
          tab.setWidthPercentage(100)
          tab.addCell(new PdfPCell(getBarCodeImg(workCard._id)(writer)))

          prepareCell("定型組/整理課")
          prepareCell("訂單:" + workCard.orderId)
          val order = orderMap(workCard.orderId)
          prepareCell("工廠代號:" + order.factoryId)
          prepareCell("客戶編號:" + order.customerId)
          prepareCell("尺寸:" + order.details(workCard.detailIndex).size)
          prepareCell("顏色:" + order.details(workCard.detailIndex).color)
          prepareCell("數量:" + toDozenStr(workCard.quantity))

          val cell = new PdfPCell(tab)
          cell.setBorder(Rectangle.NO_BORDER)

          cardTab.addCell(cell)
        }
        // body
        {
          val bodyTab = new PdfPTable(9)
          bodyTab.setWidthPercentage(100)

          {
            // 定型
            implicit val tab = new PdfPTable(2)
            prepareCell(""); prepareCell("定型")
            prepareCell("機台"); prepareCell("")
            val cell = new PdfPCell(tab)
            cell.setBorder(Rectangle.NO_BORDER)
            cell.setColspan(2)
            bodyTab.addCell(cell)
          }
          {
            implicit val tab = bodyTab
            def emptyCells() {
              for (i <- 1 to 8)
                prepareCell("")
            }

            prepareCell("檢襪\n分襪")
            prepareCell("巡襪")
            prepareCell("車洗標"); prepareCell("剪線頭");
            prepareCell("整理\n包裝"); prepareCell("成品\n倉庫"); prepareCell("備註")

            prepareCell("日期"); emptyCells
            prepareCell("優"); emptyCells
            prepareCell("副"); emptyCells
            prepareCell("副未包"); emptyCells
            prepareCell("汙"); emptyCells
            prepareCell("長短"); emptyCells
            prepareCell("破"); emptyCells
            prepareCell("不均"); emptyCells
            prepareCell("油"); emptyCells
            prepareCell("襪頭"); emptyCells
            prepareCell("工號"); emptyCells
            prepareCell("備註"); emptyCells
            prepareCell("主管審核"); emptyCells
          }

          val cell = new PdfPCell(bodyTab)
          cell.setBorder(Rectangle.NO_BORDER)
          cardTab.addCell(cell)
        }

        val cell = new PdfPCell(cardTab)

        cell.setBorder(Rectangle.NO_BORDER)
        cell.setPadding(5f)
        cell.setColspan(4)
        cell.setRowspan(1)
        sheetTab.addCell(cell)
      }

      def stylingLabel() {
        implicit val tab = new PdfPTable(1)
        tab.setSpacingBefore(6f)
        tab.setWidthPercentage(100)

        tab.addCell(new PdfPCell(getBarCodeImg(workCard._id)(writer)))

        prepareCell("單位:定型卡")
        prepareCell(workCard.orderId)
        val order = orderMap(workCard.orderId)
        prepareCell("工廠代號:\n" + order.factoryId)
        prepareCell("客戶編號:\n" + order.customerId)
        prepareCell("尺寸:" + order.details(workCard.detailIndex).size)
        prepareCell("顏色:" + order.details(workCard.detailIndex).color)
        prepareCell("數量:" + toDozenStr(workCard.quantity))
        prepareCell("機台:")
        prepareCell("日期:")
        prepareCell("優:")
        prepareCell("副:")
        prepareCell("副未包:")
        prepareCell("汙:")
        prepareCell("長短:")
        prepareCell("破:")
        prepareCell("不均:")
        prepareCell("油:")
        prepareCell("襪頭");
        prepareCell("工號:")
        prepareCell("備註:")
        prepareCell("主管審核:")

        val cell = new PdfPCell(tab)
        cell.setBorder(Rectangle.NO_BORDER)
        cell.setPadding(5f)
        sheetTab.addCell(cell)
      }

      def tidyLabel(name: String) {
        implicit val tab = new PdfPTable(1)
        tab.setSpacingBefore(6f)
        tab.setWidthPercentage(100)

        tab.addCell(new PdfPCell(getBarCodeImg(workCard._id)(writer)))

        prepareCell("單位:" + name)
        prepareCell(workCard.orderId)
        val order = orderMap(workCard.orderId)
        prepareCell("工廠代號:\n" + order.factoryId)
        prepareCell("客戶編號:\n" + order.customerId)
        prepareCell("尺寸:" + order.details(workCard.detailIndex).size)
        prepareCell("顏色:" + order.details(workCard.detailIndex).color)
        prepareCell("數量:" + toDozenStr(workCard.quantity))
        prepareCell("日期:")
        prepareCell("優:")
        prepareCell("副:")
        prepareCell("副未包:")
        prepareCell("汙:")
        prepareCell("長短:")
        prepareCell("破:")
        prepareCell("不均:")
        prepareCell("油:")
        prepareCell("襪頭")
        prepareCell("工號:")
        prepareCell("備註:")
        prepareCell("主管審核:")

        val cell = new PdfPCell(tab)
        cell.setBorder(Rectangle.NO_BORDER)
        cell.setPadding(5f)
        sheetTab.addCell(cell)
      }

      //Start
      whiteSock
      stylingTidy
      stylingLabel
      tidyLabel("車洗標卡")
      tidyLabel("檢襪卡")

      doc.add(sheetTab)
      doc.newPage()
    }

    for (workCard <- workSeq) {
      prepareSheet(workCard)
    }
  }

  def workCardLabelProc(workSeq: Seq[WorkCard], orderMap: Map[String, Order])(doc: Document, writer: PdfWriter) {
    val bf = BaseFont.createFont("C:/Windows/Fonts/mingliu.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    val font = new Font(bf, 16)
    def prepareCell(str: String, add: Boolean = true)(implicit tab: PdfPTable) = {
      val cell = new PdfPCell(new Paragraph(str, font))
      cell.setHorizontalAlignment(Element.ALIGN_LEFT)
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE)
      cell.setPaddingBottom(8)
      if (add)
        tab.addCell(cell)

      cell
    }

    for (workCard <- workSeq) {
      doc.add(getBarCodeImg(workCard._id)(writer))

      implicit val tab = new PdfPTable(1)
      tab.setSpacingBefore(6f)
      tab.setWidthPercentage(100)

      prepareCell(workCard.orderId)
      val order = orderMap(workCard.orderId)
      prepareCell(order.factoryId)
      prepareCell(order.customerId)
      prepareCell(order.details(workCard.detailIndex).size)
      prepareCell(order.details(workCard.detailIndex).color)
      prepareCell("數量:" + toDozenStr(workCard.quantity))

      doc.add(tab)
      doc.newPage()
    }
  }
  def createWorkCardLabel(proc: (Document, PdfWriter) => Unit) = {
    import java.io.FileOutputStream
    import java.nio.charset.Charset
    import java.io._
    import java.nio.charset.Charset

    val labelSize = new Rectangle(82, 254)
    val document = new Document(labelSize);

    val tempFile = File.createTempFile("workCardLabel", ".pdf")
    val writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));

    document.setMargins(2, 2, 2, 2)
    document.open()

    proc(document, writer)
    document.close()

    tempFile
  }

  def createWorkSheet(proc: (Document, PdfWriter) => Unit) = {
    import java.io.FileOutputStream
    import java.nio.charset.Charset
    import java.io._
    import java.nio.charset.Charset

    val document =
      new Document(PageSize.A4.rotate());

    val tempFile = File.createTempFile("workCardLabel", ".pdf")
    val writer = PdfWriter.getInstance(document, new FileOutputStream(tempFile));

    document.setMargins(2, 2, 2, 2)
    document.open()

    proc(document, writer)
    document.close()

    tempFile
  }

  import java.io.File
  def createBarcode(outputFile: File, msg: String) = {
    import java.awt.image.BufferedImage

    import java.io.FileOutputStream
    import java.io.OutputStream

    import org.krysalis.barcode4j.impl.code39.Code39Bean
    import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider
    import org.krysalis.barcode4j.tools.UnitConv
    val bean = new Code39Bean()

    val dpi = 100;

    //Configure the barcode generator
    bean.setModuleWidth(UnitConv.in2mm(1.0f / dpi)); //makes the narrow bar 
    //width exactly one pixel
    bean.setWideFactor(3);
    bean.doQuietZone(false);

    //Open output file
    val out = new FileOutputStream(outputFile);
    try {
      //Set up the canvas provider for monochrome JPEG output 
      val canvas = new BitmapCanvasProvider(
        out, "image/jpeg", dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);

      //Generate the barcode
      bean.generateBarcode(canvas, msg);

      //Signal end of generation
      canvas.finish();
    } finally {
      out.close();
    }
    outputFile
  }
}