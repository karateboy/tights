package models
import play.api._
import com.github.nscala_time.time.Imports._
import models.ModelHelper._
import models._
import org.mongodb.scala.bson.Document
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class User(_id: String, password: String, name: String, phone: String, isAdmin: Boolean,
                groupId: String = Group.Admin.toString())

object User {
  import scala.concurrent._
  import scala.concurrent.duration._

  val ColName = "users"
  val collection = MongoDB.database.getCollection(ColName)
  implicit val userRead = Json.reads[User]
  implicit val userWrite = Json.writes[User]
  
  def toDocument(user: User) = Document(Json.toJson(user).toString())

  def init(colNames: Seq[String]) {
    if (!colNames.contains(ColName)) {
      val f = MongoDB.database.createCollection(ColName).toFuture()
      f.onFailure(errorHandler)
    }
    val f = collection.countDocuments().toFuture()
    f.onSuccess({
      case count: Long =>
        if (count == 0) {
          val defaultUser = User("sales@wecc.com.tw", "abc123", "Aragorn", "02-2219-2886", true)
          Logger.info("Create default user:" + defaultUser.toString())
          newUser(defaultUser)
        }
    })
    f.onFailure(errorHandler)
  }

  def toUser(doc: Document) = {
    val ret = Json.parse(doc.toJson()).validate[User]

    ret.fold(error => {
      Logger.error(JsError.toJson(error).toString())
      throw new Exception(JsError.toJson(error).toString)
    },
      usr => usr)

  }

  def createDefaultUser = {
    val f = collection.countDocuments().toFuture()
    val ret = waitReadyResult(f)
    if (ret == 0) {
      val defaultUser = User("sales@wecc.com.tw", "abc123", "Aragorn", "02-2219-2886", true)
      Logger.info("Create default user:" + defaultUser.toString())
      newUser(defaultUser)
    }
  }
  def newUser(user: User) = {
    collection.insertOne(toDocument(user)).toFuture()
  }

  import org.mongodb.scala.model.Filters._
  def deleteUser(email: String) = {
    collection.deleteOne(equal("_id", email)).toFuture()
  }

  def updateUser(user: User) = {
    val f = collection.replaceOne(equal("_id", user._id), toDocument(user)).toFuture()
    f
  }

  def getUserByEmail(email: String) = {
    val f = collection.find(equal("_id", email)).toFuture()
    f.onFailure { errorHandler }
    val ret = waitReadyResult(f)
    if (ret.isEmpty)
      None
    else
      Some(toUser(ret(0)))
  }

  def getUserByEmailFuture(email: String): Future[Some[User]] = {
    val f = collection.find(equal("_id", email)).first().toFuture()
    f.onFailure { errorHandler }
    for (ret <- f)
      yield
      Some(toUser(ret))
  }

  def getAllUsers() = {
    val f = collection.find().toFuture()
    f.onFailure { errorHandler }
    val ret = waitReadyResult(f)
    ret.map { toUser }
  }

  def getAllUsersFuture() = {
    val f = collection.find().toFuture()
    f.onFailure { errorHandler }
    for (ret <- f) yield ret.map { toUser }
  }

  def getAdminUsers() = {
    val f = collection.find(equal("isAdmin", true)).toFuture()
    f.onFailure { errorHandler }
    val ret = waitReadyResult(f)
    ret.map { toUser }
  }

}
