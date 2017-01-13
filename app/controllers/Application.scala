package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import scala.concurrent.Future
import play.api.libs.json._
import com.github.nscala_time.time.Imports._
import Highchart._
import models._

object Application extends Controller {

  val title = "佩登斯生產履歷系統"

  def index = Security.Authenticated.async {
    implicit request =>
      val userOptF = User.getUserByEmailFuture(request.user.id)
      for {
        userOpt <- userOptF if userOpt.isDefined
        groupF = Group.findGroup(userOpt.get.groupId)
        groupSeq <- groupF
      } yield {
        val group = groupSeq(0)

        Ok(views.html.outline(title, request.user, views.html.dashboard(group.privilege)))
      }
  }

  def dashboard = Security.Authenticated.async {
    implicit request =>
      val userOptF = User.getUserByEmailFuture(request.user.id)
      for {
        userOpt <- userOptF if userOpt.isDefined
        groupF = Group.findGroup(userOpt.get.groupId)
        groupSeq <- groupF
      } yield {
        val group = groupSeq(0)

        Ok(views.html.dashboard(group.privilege))
      }
  }

  def userManagement() = Security.Authenticated {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      if (userInfoOpt.isEmpty)
        Forbidden("No such user!")
      else {
        val userInfo = userInfoOpt.get
        val user = User.getUserByEmail(userInfo.id).get
        val userList =
          if (!user.isAdmin)
            List.empty[User]
          else
            User.getAllUsers.toList

        Ok(views.html.userManagement(userInfo, user, userList))
      }
  }

  import models.User._

  def newUser = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      adminOnly({
        val newUserParam = request.body.validate[User]

        newUserParam.fold(
          error => {
            Logger.error(JsError.toJson(error).toString())
            Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
          },
          param => {
            val f = User.newUser(param)
            val requestF =
              for (result <- f) yield {
                Ok(Json.obj("ok" -> true))
              }

            requestF.recover({
              case _: Throwable =>
                Logger.info("recover from newUser error...")
                Ok(Json.obj("ok" -> false))
            })
          })
      })
  }

  def deleteUser(email: String) = Security.Authenticated.async {
    implicit request =>
      adminOnly({
        Logger.info(email.toString)
        val f = User.deleteUser(email)
        val requestF =
          for (result <- f) yield {
            val deleteResult = result.head
            Ok(Json.obj("ok" -> (deleteResult.getDeletedCount == 0)))
          }

        requestF.recover({
          case _: Throwable =>
            Logger.info("recover from deleteUser error...")
            Ok(Json.obj("ok" -> false))
        })
      })
  }

  def updateUser(id: String) = Security.Authenticated(BodyParsers.parse.json) {
    implicit request =>
      val userParam = request.body.validate[User]

      userParam.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString()))
        },
        param => {
          User.updateUser(param)
          Ok(Json.obj("ok" -> true))
        })
  }

  def getAllUsers = Security.Authenticated {
    val users = User.getAllUsers()
    
    Ok(Json.toJson(users))
  }

  def groupManagement() = Security.Authenticated {
    implicit request =>
      val userInfoOpt = Security.getUserinfo(request)
      if (userInfoOpt.isEmpty)
        Forbidden("No such user!")
      else {
        val userInfo = userInfoOpt.get
        val user = User.getUserByEmail(userInfo.id).get
        if (!user.isAdmin)
          Forbidden("無權限!")
        else
          Ok(views.html.groupManagement(userInfo))
      }
  }

  def adminOnly[A, B <: controllers.Security.UserInfo](permited: Future[Result])(implicit request: play.api.mvc.Security.AuthenticatedRequest[A, B]) = {
    val userInfoOpt = Security.getUserinfo(request)
    if (userInfoOpt.isEmpty)
      Future {
        Forbidden("No such user!")
      }
    else {
      val userInfo = userInfoOpt.get
      val user = User.getUserByEmail(userInfo.id).get
      if (!user.isAdmin)
        Future {
          Forbidden("無權限!")
        }
      else {
        permited
      }
    }
  }

  def newGroup(id: String) = Security.Authenticated.async {
    implicit request =>
      adminOnly({
        val newGroup = Group(id, Privilege.defaultPrivilege)
        val f = Group.newGroup(newGroup)

        val requestF =
          for (result <- f) yield {
            Ok(Json.obj("ok" -> true))
          }

        requestF.recover({
          case _: Throwable =>
            Logger.info("recover...")
            Ok(Json.obj("ok" -> false))
        })
      })
  }

  import scala.concurrent.ExecutionContext.Implicits.global
  def getAllGroups = Security.Authenticated.async {
    val f = Group.getGroupList
    for (groupList <- f) yield {
      Ok(Json.toJson(groupList))
    }
  }

  def deleteGroup(id: String) = Security.Authenticated.async {
    implicit request =>
      adminOnly({
        val f = Group.delGroup(id)
        val requestF = for (ret <- f) yield {
          Ok(Json.obj("ok" -> true))
        }
        requestF.recover({
          case _: Throwable =>
            Logger.info("recover...")
            Ok(Json.obj("ok" -> false))
        })
      })
  }

  def updateGroup(id: String) = Security.Authenticated.async(BodyParsers.parse.json) {
    implicit request =>
      Logger.debug("updateGroup")
      val userInfoOpt = Security.getUserinfo(request)
      if (userInfoOpt.isEmpty)
        Future {
          Forbidden("No such user!")
        }
      else {
        val userInfo = userInfoOpt.get
        val user = User.getUserByEmail(userInfo.id).get
        if (!user.isAdmin)
          Future {
            Forbidden("無權限!")
          }
        else {
          val groupResult = request.body.validate[Group]

          groupResult.fold(error => {
            Logger.error(JsError.toJson(error).toString())
            Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
          },
            group => {
              val f = Group.updateGroup(group)
              val requestF = for (ret <- f) yield {
                Ok(Json.obj("ok" -> true))
              }
              requestF
            })
        }
      }
  }

  def menuRightList = Security.Authenticated.async {
    implicit request =>
      val userOptF = User.getUserByEmailFuture(request.user.id)
      for {
        userOpt <- userOptF if userOpt.isDefined
        groupF = Group.findGroup(userOpt.get.groupId)
        groupSeq <- groupF
      } yield {
        if (groupSeq.length == 0)
          Ok(Json.toJson(List.empty[MenuRight.Value]))
        else {
          val group = groupSeq(0)
          val menuRightList =
            if (userOpt.get.isAdmin) {
              MenuRight.values.toList.map { v => MenuRight(v, MenuRight.map(v)) }
            } else
              group.privilege.allowedMenuRights.map { v => MenuRight(v, MenuRight.map(v)) }

          Ok(Json.toJson(menuRightList))
        }
      }
  }
}
