package models

import models._
import models.ModelHelper._
import org.mongodb.scala.bson.{BsonArray, BsonDocument, Document}
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logger
import play.api.libs.json.{JsError, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._
import org.mongodb.scala.model.ReplaceOptions

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by user on 2017/1/1.
 */

case class ProductionNotice(department: String, msg: String) {
  def toDocument = {
    Document("department" -> department, "msg" -> msg)
  }
}

case class OrderDetail(color: String, size: String, quantity: Int, complete: Boolean) {
  def toDocument = {
    Document("color" -> color, "size" -> size,
      "quantity" -> quantity,
      "complete" -> complete)
  }

  def trim = OrderDetail(color.trim(), size.trim(), quantity, complete)
}
case class QueryOrderParam(_id: Option[String], brand: Option[String], name: Option[String],
                           factoryId: Option[String], customerId: Option[String],
                           start: Option[Long], end: Option[Long])

object OrderDetail {
  implicit def toOrderDetail(implicit doc: BsonDocument) = {
    //OrderDetail(color: String, size: String, quantity: Int)
    import org.mongodb.scala.bson._

    val color = doc.getString("color").getValue
    val size = doc.getString("size").getValue
    val quantity = doc.getInt32("quantity").getValue
    val complete = doc.getBoolean("complete").getValue

    OrderDetail(color, size, quantity, complete)
  }
}

case class PackageInfo(packageOption: Seq[Boolean], packageNote: String,
                       labelOption: Seq[Boolean], labelNote: String,
                       cardOption: Seq[Boolean], cardNote: Seq[String],
                       bagOption: Seq[Boolean], pvcNote: String,
                       numInBag: Option[Int], bagNote: String,
                       exportBoxOption: Seq[Boolean], exportBoxNote: Seq[String],
                       ShippingMark: String, extraNote: Option[String]) {
  def toDocument = {
    Document(
      "packageOption" -> packageOption,
      "packageNote" -> packageNote,
      "labelOption" -> labelOption,
      "labelNote" -> labelNote,
      "cardOption" -> cardOption,
      "cardNote" -> cardNote,
      "bagOption" -> bagOption,
      "pvcNote" -> pvcNote,
      "numInBag" -> numInBag,
      "bagNote" -> bagNote,
      "exportBoxOption" -> exportBoxOption,
      "exportBoxNote" -> exportBoxNote,
      "ShippingMark" -> ShippingMark,
      "extraNote" -> extraNote)
  }
}
object PackageInfo {
  implicit def toPackageInfo(implicit doc: Document) = {
    import org.mongodb.scala.bson._

    val packageOption = getArray("packageOption", (v) => v.asBoolean().getValue)
    val packageNote = doc.getString("packageNote")
    val labelOption = getArray("labelOption", (v) => v.asBoolean().getValue)
    val labelNote = doc.getString("labelNote")
    val cardOption = getArray("cardOption", (v) => v.asBoolean().getValue)
    val cardNote = getArray("cardNote", (v) => v.asString().getValue)
    val bagOption = getArray("bagOption", (v) => v.asBoolean().getValue)
    val pvcNote = doc.getString("pvcNote")
    val numInBag = getOptionInt("numInBag")
    val bagNote = doc.getString("bagNote")
    val exportBoxOption = getArray("exportBoxOption", (v) => v.asBoolean().getValue)
    val exportBoxNote = getArray("exportBoxNote", (v) => v.asString().getValue)
    val ShippingMark = doc.getString("ShippingMark")
    val extraNote = getOptionStr("extraNote")

    PackageInfo(
      packageOption = packageOption,
      packageNote = packageNote,
      labelOption = labelOption,
      labelNote = labelNote,
      cardOption = cardOption,
      cardNote = cardNote,
      bagOption = bagOption,
      pvcNote = pvcNote,
      numInBag = numInBag,
      bagNote = bagNote,
      exportBoxOption = exportBoxOption,
      exportBoxNote = exportBoxNote,
      ShippingMark = ShippingMark,
      extraNote = extraNote)
  }
}
case class Order(_id: String, salesId: String, name: String, expectedDeliverDate: Long, finalDeliverDate: Option[Long],
                 factoryId: String,
                 customerId: String, brand: String, var date: Option[Long],
                 details: Seq[OrderDetail], notices: Seq[ProductionNotice], packageInfo: PackageInfo, active: Boolean) {
  def toDocument = {
    import org.mongodb.scala.bson._
    implicit object TransformOrderDetail extends BsonTransformer[OrderDetail] {
      def apply(od: OrderDetail): BsonDocument = od.toDocument.toBsonDocument
    }

    implicit object TransformNotice extends BsonTransformer[ProductionNotice] {
      def apply(pn: ProductionNotice): BsonDocument = pn.toDocument.toBsonDocument
    }

    implicit object TransformPackage extends BsonTransformer[PackageInfo] {
      def apply(pInfo: PackageInfo): BsonDocument = pInfo.toDocument.toBsonDocument
    }

    Document("_id" -> _id,
      "salesId" -> salesId,
      "name" -> name,
      "expectedDeliverDate" -> expectedDeliverDate,
      "finalDeliverDate" -> finalDeliverDate,
      "factoryId" -> factoryId,
      "customerId" -> customerId,
      "brand" -> brand,
      "date" -> date,
      "details" -> details,
      "notices" -> notices,
      "packageInfo" -> packageInfo,
      "active" -> active)
  }

  def trim = {
    Order(_id, salesId, name.trim(), expectedDeliverDate, finalDeliverDate,
      factoryId.trim(),
      customerId.trim(), brand.trim, date,
      details.map { _.trim }, notices, packageInfo, active)
  }

}

object Order {
  val colName = "orders"
  val collection = MongoDB.database.getCollection(colName)

  implicit val odWrite = Json.writes[OrderDetail]
  implicit val odRead = Json.reads[OrderDetail]
  implicit val pnWrite = Json.writes[ProductionNotice]
  implicit val pnRead = Json.reads[ProductionNotice]
  implicit val pkRead = Json.reads[PackageInfo]
  implicit val pkWrite = Json.writes[PackageInfo]
  implicit val orderWrite = Json.writes[Order]
  implicit val orderRead = Json.reads[Order]

  def init(colNames: Seq[String]) = {
    if (!colNames.contains(colName)) {
      val f = MongoDB.database.createCollection(colName).toFuture()
      f.onFailure(errorHandler)
      f onComplete {
        case Success(_)=>
          val cf1 = collection.createIndex(ascending("date", "customerId", "active")).toFuture()
          val cf2 = collection.createIndex(ascending("salesId", "date", "active")).toFuture()
          val cf3 = collection.createIndex(ascending("active")).toFuture()
        case Failure(exception)=>
          Logger.error("failed", exception)

      }
      Some(f.mapTo[Unit])
    } else
      None
  }

  def toOrder(doc: Document) = {
    import org.bson.json._

    val _id = doc.getString("_id")
    val salesId = doc.getString("salesId")
    val name = doc.getString("name")
    val expectedDeliverDate = doc("expectedDeliverDate").asInt64().getValue
    val finalDeliverDate = getOptionTime("finalDeliverDate")(doc)
    val factoryId = doc.getString("factoryId")
    val customerId = doc.getString("customerId")
    val brand = doc.getString("brand")
    val date = getOptionTime("date")(doc)
    val details = doc("details").asArray()
    val notices = doc("notices").asArray()
    val packageInfo = doc("packageInfo").asDocument()
    val active = doc.getBoolean("active")

    def toOrderDetialSeq(ar: BsonArray) = {
      import OrderDetail._
      val array = ar.toArray()
      array.map {
        doc => toOrderDetail(doc.asInstanceOf[BsonDocument])
      }
    }

    def toProductionNoticeSeq(ar: BsonArray) = {
      def toProductionNotice(doc: BsonDocument) = {
        //ProductionNotice(department: String, msg: String)
        val department = doc.getString("department").getValue
        val msg = doc.getString("msg").getValue
        ProductionNotice(department, msg)
      }

      val array = ar.toArray()
      array.map {
        doc => toProductionNotice(doc.asInstanceOf[BsonDocument])
      }
    }

    import PackageInfo._

    Order(_id = _id,
      salesId = salesId,
      name = name,
      expectedDeliverDate = expectedDeliverDate,
      date = date,
      finalDeliverDate = finalDeliverDate,
      factoryId = factoryId,
      customerId = customerId,
      brand = brand,
      details = toOrderDetialSeq(details),
      notices = toProductionNoticeSeq(notices),
      packageInfo = toPackageInfo(packageInfo),
      active = active)
  }

  def listActiveOrder() = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(equal("active", true)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  import org.mongodb.scala.model.Filters._

  def upsertOrder(order: Order) = {
    import org.mongodb.scala.model.UpdateOptions
    import org.mongodb.scala.bson.BsonString

    if (order.date == Some(0) || order.date == None)
      order.date = Some(DateTime.now().getMillis)

    val col = MongoDB.database.getCollection(colName)
    val doc = order.toDocument
    val colorSeq = getOrderColor(order)
    SysConfig.addColorSeq(colorSeq)
    val f = col.replaceOne(equal("_id", doc("_id")), doc, ReplaceOptions().upsert(true)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def getOrderColor(order: Order) = {
    var colorSet = Set.empty[String]
    for (detail <- order.details) {
      colorSet += detail.color
    }
    colorSet.toSeq
  }
  def getOrder(orderId: String) = {
    val f = collection.find(equal("_id", orderId)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
      if (orders.isEmpty)
        None
      else
        Some(toOrder(orders(0)))
    }
  }

  def getOrders(orderIds: Seq[String]) = {
    val f = collection.find(in("_id", orderIds: _*)).toFuture()
    f.onFailure {
      errorHandler
    }
    for (orders <- f) yield {
      orders map toOrder
    }
  }
  def findOrders(orderIdList: Seq[String]) = {
    import org.mongodb.scala.model._
    val f = collection.find(in("_id", orderIdList: _*)).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def myActiveOrder(salesId: String)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(equal("active", true), equal("salesId", salesId)))
      .sort(Sorts.ascending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def myActiveOrderCount(salesId: String): Future[Long] = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.countDocuments(and(equal("active", true), equal("salesId", salesId))).toFuture()
    f.onFailure {
      errorHandler
    }
    for (count <- f)
      yield count
  }

  def getHistoryOrder(begin: Long, end: Long) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._
    val f = collection.find(and(gte("date", begin), lt("date", end))).sort(Sorts.ascending("_id")).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def addOrderDetailWorkID(orderId: String, index: Int, workCardID: String) = {
    import org.mongodb.scala.bson._
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model.Updates._

    val fieldName = "details." + index + ".workCardIDs"
    val col = MongoDB.database.getCollection(colName)
    val f = col.updateOne(and(equal("_id", orderId)), addToSet(fieldName, workCardID)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def setOrderDetailComplete(orderId: String, index: Int, complete: Boolean) = {
    import org.mongodb.scala.bson._
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model.Updates._

    val fieldName = "details." + index + ".complete"
    val col = MongoDB.database.getCollection(colName)
    val f = col.updateOne(and(equal("_id", orderId)), set(fieldName, complete)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def queryOrder(param: QueryOrderParam)(skip: Int, limit: Int) = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val brandFilter = param.brand map { brand => regex("brand", "(?i)" + brand) }
    val nameFilter = param.name map { name => regex("name", "(?i)" + name) }
    val factoryFilter = param.factoryId map { factoryId => regex("factoryId", "(?i)" + factoryId) }
    val customerFilter = param.customerId map { customerId => regex("customerId", "(?i)" + customerId) }
    val startFilter = param.start map { start => gte("expectedDeliverDate", start) }
    val endFilter = param.end map { end => lt("expectedDeliverDate", end) }

    val filterList = List(idFilter, brandFilter, nameFilter, factoryFilter,
      customerFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.find(filter).sort(Sorts.ascending("_id")).skip(skip).limit(limit).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }
  def queryOrderCount(param: QueryOrderParam): Future[Long] = {
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model._

    val idFilter = param._id map { _id => regex("_id", _id) }
    val brandFilter = param.brand map { brand => regex("brand", "(?i)" + brand) }
    val nameFilter = param.name map { name => regex("name", "(?i)" + name) }
    val factoryFilter = param.factoryId map { factoryId => regex("factoryId", "(?i)" + factoryId) }
    val customerFilter = param.customerId map { customerId => regex("customerId", "(?i)" + customerId) }
    val startFilter = param.start map { start => gte("expectedDeliverDate", start) }
    val endFilter = param.end map { end => lt("expectedDeliverDate", end) }

    val filterList = List(idFilter, brandFilter, nameFilter, factoryFilter,
      customerFilter, startFilter, endFilter).flatMap { f => f }
    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")

    val f = collection.countDocuments(filter).toFuture()
    f.onFailure {
      errorHandler
    }
    for (countSeq <- f)
      yield countSeq
  }
  def closeOrder(_id: String) = {
    import org.mongodb.scala.model.Updates._
    val f = collection.findOneAndUpdate(equal("_id", _id), set("active", false)).toFuture()
    f
  }

  def reopenOrder(_id: String) = {
    import org.mongodb.scala.model.Updates._
    val f = collection.findOneAndUpdate(equal("_id", _id), set("active", true)).toFuture()
    f
  }

  def deleteOrder(_id: String) = {
    collection.deleteOne(equal("_id", _id)).toFuture()
  }

  import org.mongodb.scala.model._
  def trimOrder() = {
    val f = collection.find().toFuture()
    val orderListF =
      for (docList <- f) yield {
        docList map { toOrder }
      }

    val modelsF =
      for (orderList <- orderListF) yield {
        import org.mongodb.scala.model.ReplaceOneModel
        orderList map {
          order =>
            ReplaceOneModel(Filters.eq("_id", order._id), order.trim.toDocument)
        }

      }

    val upgradeFF =
      for (models <- modelsF) yield {
        val f = collection.bulkWrite(models).toFuture()
        f.onFailure(errorHandler)
        f
      }
    val upgradeF = upgradeFF.flatMap { x => x }
    for(upgradeSeq <- upgradeF) {
      val upgradeCount = upgradeSeq.getMatchedCount
      Logger.info(s"$upgradeCount orders are trimmed.")
    }
  }
}
