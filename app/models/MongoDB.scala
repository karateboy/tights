package models
import play.api._
import scala.concurrent.ExecutionContext.Implicits.global

object MongoDB {
  import org.mongodb.scala._

  val url = Play.current.configuration.getString("my.mongodb.url")
  val dbName = Play.current.configuration.getString("my.mongodb.db")

  val mongoClient: MongoClient = MongoClient(url.get)
  val database: MongoDatabase = mongoClient.getDatabase(dbName.get);
  def init() {
    val f = database.listCollectionNames().toFuture()
    val colFuture = f.map { colNames =>
      User.init(colNames)
      Order.init(colNames)
      DyeCard.init(colNames)
      WorkCard.init(colNames)
      TidyCard.init(colNames)
      Identity.init(colNames)
      Inventory.init(colNames)
      SysConfig.init(colNames)

      for (v <- SysConfig.getTrimOrderConfig) {
        if (!v.asBoolean().getValue) {
          Order.trimOrder()
          SysConfig.setTrimOrderConfig(true)
        }
      }

      for (v <- SysConfig.getTrimColorSeq) {
        if (!v.asBoolean().getValue) {
          SysConfig.trimColorSeq
          SysConfig.setTrimColorSeq(true)
        }
      }

      for (v <- SysConfig.getFixNullInventory) {
        Logger.info(s"FixNullInventory $v")
        if (!v.asBoolean().getValue) {
          val f = Inventory.fixNullInventory()
          f.onComplete(ret => {
            if (ret.isFailure) {
              Logger.error("failed to fix null inventory")
            } else {
              val updateResultSeq = ret.get
              if(!updateResultSeq.isEmpty){
                val result = updateResultSeq.head
                Logger.info(s"${result.getMatchedCount} match ${result.getModifiedCount} modified")
              }
              SysConfig.setFixNullInventory(true)
            }
          })

        }
      }
    }
    //Program need to wait before init complete
    import scala.concurrent.Await
    import scala.concurrent.duration._
    import scala.language.postfixOps

    Await.result(colFuture, 30 seconds)
  }

  def cleanup = {
    mongoClient.close()
  }
}