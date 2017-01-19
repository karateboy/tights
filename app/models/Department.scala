package models

case class DeparmentInfo(id: String, name: String)
object Department extends Enumeration {
  val PM = Value
  val Tuexture = Value
  val Weaving = Value
  val WhiteSock = Value
  val Dyeing = Value
  val Styling = Value
  val Tidy = Value
  val Packaging = Value

  val map = Map(
    PM -> "生管部",
    Tuexture -> "假撚課",
    Weaving -> "編織部",
    WhiteSock -> "白襪部",
    Dyeing -> "漂染部",
    Styling -> "定型部",
    Tidy -> "整理部",
    Packaging -> "裝箱出貨部")
    
  def getInfoList = map.map {m => DeparmentInfo(m._1.toString, m._2)}.toList
  
}