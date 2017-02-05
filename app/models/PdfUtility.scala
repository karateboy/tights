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

  def creatPdfWithReportHeader(title: String, content: play.twirl.api.HtmlFormat.Appendable) = {
    val html = views.html.reportTemplate(title, content)
    createPdf(html.toString, false)
  }

  def creatPdfWithReportHeaderP(title: String, content: play.twirl.api.HtmlFormat.Appendable) = {
    val html = views.html.reportTemplate(title, content)
    createPdf(html.toString, false)
  }

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

  def dyeCardProc(dyeCard: DyeCard, workSeq: Seq[WorkCard], orderMap: Map[String, Order])(doc: Document, writer: PdfWriter) {
    { // barcode      
      val code39 = new Barcode39()
      code39.setCode(dyeCard._id)
      val bar2Img = code39.createImageWithBarcode(writer.getDirectContent, BaseColor.BLACK, BaseColor.GRAY)
      doc.add(bar2Img)
      
    }
    val bf = BaseFont.createFont("C:/Windows/Fonts/mingliu.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    val font = new Font(bf, 12)
    def prepareCell(str: String, add: Boolean = true)(implicit tab: PdfPTable) = {
      val cell = new PdfPCell(new Paragraph(str, font))
      cell.setHorizontalAlignment(Element.ALIGN_LEFT)
      cell.setVerticalAlignment(Element.ALIGN_MIDDLE)
      cell.setPaddingBottom(8)
      if (add)
        tab.addCell(cell)

      cell
    }
    val sizeList = workSeq.map {
      work =>
        val order = orderMap(work.orderId)
        order.details(work.detailIndex).size
    }
    val sizeSet = Set(sizeList.toSeq: _*)

    {
      implicit val topTable = new PdfPTable(3); // 3 columns.
      topTable.setWidthPercentage(100)
      val orderStr = orderMap.keys.mkString(",")
      prepareCell("訂單編號:" + orderStr)
      val customerSeq = orderMap.values.map { _.customerId }
      val customerSet = Set(customerSeq.toSeq: _*)
      prepareCell("客戶:" + customerSet.mkString(","))
      val deliverDate = new DateTime(orderMap.values.map { _.expectedDeliverDate }.min)
      prepareCell("出貨日:" + deliverDate.toString("YYYY-MM-dd"))

      val factoryIds = orderMap.values.map { _.factoryId }
      val factoryIdSet = Set(factoryIds.toSeq: _*)
      val factoryCell = prepareCell("工廠代號:" + factoryIdSet.mkString(","), false)
      factoryCell.setColspan(2)
      topTable.addCell(factoryCell)
      prepareCell("尺寸:" + sizeSet.mkString("/"))
      prepareCell("顏色:" + dyeCard.color)
      val quantityList = workSeq.map { _.quantity }
      val totalDozen = quantityList.sum / 12
      prepareCell("總數量(打):" + totalDozen)
      prepareCell("編織編號:")

      doc.add(topTable)
    }
    {
      implicit val tab2 = new PdfPTable(5)
      tab2.setSpacingBefore(12f)
      tab2.setWidthPercentage(100)

      prepareCell("品名")
      prepareCell("尺寸")
      prepareCell("包數")
      prepareCell("打數")
      prepareCell("襪袋備註")
      for(workCard <- workSeq){
        val order = orderMap(workCard.orderId)
        prepareCell(order.name)
        prepareCell(order.details(workCard.detailIndex).size)
        prepareCell(" ")
        prepareCell((workCard.quantity/12).toString())
        prepareCell(" ")
      }
      
      prepareCell("其他備註:")
      val cell = prepareCell(" ", false)
      cell.setColspan(2)
      tab2.addCell(cell)
      prepareCell("包襪日:")
      prepareCell("工號:")
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
      prepareCell("均染劑(一般,PAM):____kg")
      prepareCell("冰醋酸:____kg")
      prepareCell("溫度:____C")
      prepareCell("醋銨:____kg")
      prepareCell("氨水:____kg")
      prepareCell("溫度:____時間")
      prepareCell("起染pH:")
      prepareCell("染終pH:")
      prepareCell(" ")

      val pp = prepareCell("後處理程序(kg):", false)
      pp.setRowspan(2)
      tab3.addCell(pp)
      prepareCell("固色劑:____kg")
      prepareCell("陽離子柔軟劑:____kg")
      prepareCell("柔軟時間:____kg")
      prepareCell("冰醋酸:____kg")
      prepareCell("矽利康:____kg")
      prepareCell(" ")

      prepareCell("烘乾")
      prepareCell("溫度:")
      prepareCell("時間:")
      prepareCell(" ")

      doc.add(tab3)
    }

    {
      implicit val tab = new PdfPTable(3)
      tab.setSpacingBefore(12f)
      tab.setWidthPercentage(100)
      prepareCell("尺寸")
      prepareCell("染前長度(cm)")
      prepareCell("染後長度(cm)")

      for(size <- sizeSet){
        prepareCell(size)
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
    
    for(workCard<-workSeq){
      val code128 = new Barcode128()
      code128.setCode(workCard._id)
      val bar2Img = code128.createImageWithBarcode(writer.getDirectContent, BaseColor.BLACK, BaseColor.GRAY)
      bar2Img.setAlignment(Element.ALIGN_MIDDLE)
      doc.add(bar2Img)
      
      implicit val tab = new PdfPTable(1)
      tab.setSpacingBefore(6f)
      tab.setWidthPercentage(100)
      
      prepareCell(workCard.orderId)
      val order = orderMap(workCard.orderId)
      prepareCell(order.factoryId)
      prepareCell(order.customerId)
      prepareCell(order.details(workCard.detailIndex).size)
      prepareCell(order.details(workCard.detailIndex).color)
      prepareCell("數量:" + workCard.quantity)
      prepareCell("優:")
      
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