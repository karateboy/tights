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

  import java.io.File
  def createBarcode(outputFile:File, msg:String) = {
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