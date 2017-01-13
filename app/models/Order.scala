package models

import java.util.Date

import models.ModelHelper.errorHandler
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by user on 2017/1/1.
  */
case class ProductionNotice(department:String, msg:String)
case class OrderDetail(color:String, size:String, quantity:Int)
case class Order(_id:String, salesId:String, name:String, expectedDeliverDate:Date, finalDeliverDate:Date,
                factoryId:String,
                customerId:String, brand:String, date:Date,
                 details:Seq[OrderDetail], notices:Seq[ProductionNotice], active:Boolean)
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
          val cf2 = collection.createIndex(ascending("date", "salesId")).toFuture()

          cf1.onFailure(errorHandler)
          cf2.onFailure(errorHandler)


      })
      Some(f.mapTo[Unit])
    } else
      None
  }

  def toDocument(order: Order) = {
    val json = Json.toJson(order)
    Document(json.toString())
  }

  def toOrder(d: Document) = {
    val ret = Json.parse(d.toJson()).validate[Order]

    ret.fold(error => {
        Logger.error(JsError.toJson(error).toString())
        throw new Exception(JsError.toJson(error).toString)
      },
      m => m)
  }

}
