package models

import java.util.Date

import models.ModelHelper._
import org.mongodb.scala.bson.{ BsonArray, BsonDocument, Document }
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logger
import play.api.libs.json.{ JsError, Json }

import scala.concurrent.ExecutionContext.Implicits.global
import com.github.nscala_time.time.Imports._

/**
 * Created by user on 2017/1/1.
 */

case class ProductionNotice(department: String, msg: String){
  def toDocument={
    Document("department"->department, "msg"->msg)
  }
}

case class OrderDetail(color: String, size: String, quantity: Int,
                       workCardIDs: Seq[String], finishedWorkCards: Seq[WorkCard], complete:Boolean) {
  def toDocument = {
    Document("color" -> color, "size" -> size,
      "quantity" -> quantity,
      "workCardIDs" -> workCardIDs,
      "finishedWorkCards" -> finishedWorkCards.map { _.toDocument },
      "complete"->complete
      )
  }
}
object OrderDetail {
  implicit def toOrderDetail(implicit doc: BsonDocument) = {
    //OrderDetail(color: String, size: String, quantity: Int)
    import org.mongodb.scala.bson._

    val color = doc.getString("color").getValue
    val size = doc.getString("size").getValue
    val quantity = doc.getInt32("quantity").getValue
    val workCardIDs = getArray("workCardIDs", (v: BsonValue) => { v.asString().getValue })(doc)
    val finishedWorkCards = getArray("finishedWorkCards", (v: BsonValue) => { WorkCard.toWorkCard(v.asDocument()) })(doc)
    val complete = doc.getBoolean("complete").getValue
    
    OrderDetail(color, size, quantity, workCardIDs, finishedWorkCards, complete)
  }
}

case class Order(_id: String, salesId: String, name: String, expectedDeliverDate: Long, finalDeliverDate: Option[Long],
                 factoryId: String,
                 customerId: String, brand: String, date: Long,
                 details: Seq[OrderDetail], notices: Seq[ProductionNotice], active: Boolean) {
  def toDocument = {
    import org.mongodb.scala.bson._
    implicit object TransformOrderDetail extends BsonTransformer[OrderDetail] {
      def apply(od: OrderDetail): BsonDocument = od.toDocument.toBsonDocument
    }

    implicit object TransformNotice extends BsonTransformer[ProductionNotice] {
      def apply(pn: ProductionNotice): BsonDocument = pn.toDocument.toBsonDocument
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
      "active" -> active)
  }
}

object Order {
  val colName = "orders"
  val collection = MongoDB.database.getCollection(colName)

  implicit val odWrite = Json.writes[OrderDetail]
  implicit val odRead = Json.reads[OrderDetail]
  implicit val pnWrite = Json.writes[ProductionNotice]
  implicit val pnRead = Json.reads[ProductionNotice]
  implicit val orderWrite = Json.writes[Order]
  implicit val orderRead = Json.reads[Order]

  def init(colNames: Seq[String]) = {
    if (!colNames.contains(colName)) {
      val f = MongoDB.database.createCollection(colName).toFuture()
      f.onFailure(errorHandler)
      f.onSuccess({
        case _: Seq[t] =>

          val cf1 = collection.createIndex(ascending("date", "customerId", "active")).toFuture()
          val cf2 = collection.createIndex(ascending("salesId", "date", "active")).toFuture()
          val cf3 = collection.createIndex(ascending("active")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)
          cf3.onFailure(errorHandler)
      })
      Some(f.mapTo[Unit])
    } else
      None
  }

  def toOrder(doc: Document) = {
    import org.bson.json._
    def getOptionTime(key: String) = {
      if (doc(key).isNull())
        None
      else
        Some(doc(key).asInt64().getValue)
    }

    val _id = doc.getString("_id")
    val salesId = doc.getString("salesId")
    val name = doc.getString("name")
    val expectedDeliverDate = doc("expectedDeliverDate").asInt64().getValue
    val finalDeliverDate = getOptionTime("finalDeliverDate")
    val factoryId = doc.getString("factoryId")
    val customerId = doc.getString("customerId")
    val brand = doc.getString("brand")
    val date = doc.getLong("date")
    val details = doc("details").asArray()
    val notices = doc("notices").asArray()
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
      active = active)
  }

  def listActiveOrder() = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(equal("active", true)).toFuture()
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

    val col = MongoDB.database.getCollection(colName)
    val doc = order.toDocument

    val f = col.replaceOne(equal("_id", doc("_id")), doc, UpdateOptions().upsert(true)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }

  def findOrder(orderId: String) = {
    val f = collection.find(equal("_id", orderId)).first().toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def myActiveOrder(salesId: String) = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(and(equal("active", true), equal("salesId", salesId))).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }

  def getHistoryOrder(begin: Date, end: Date) = {
    import org.mongodb.scala.model.Filters._
    val f = collection.find(and(gte("date", begin), lt("date", end))).toFuture()
    f.onFailure {
      errorHandler
    }
    for (records <- f)
      yield records map {
      toOrder
    }
  }
  
  def addOrderDetailWorkID(orderId:String, index:Int, workCardID:String) = {
    import org.mongodb.scala.bson._
    import org.mongodb.scala.model.Filters._
    import org.mongodb.scala.model.Updates._

    val fieldName = "details." +index + ".workCardIDs"
    val col = MongoDB.database.getCollection(colName)
    val f = col.updateOne(and(equal("_id", orderId)), addToSet(fieldName, workCardID)).toFuture()
    f.onFailure({
      case ex: Exception => Logger.error(ex.getMessage, ex)
    })
    f
  }
}
