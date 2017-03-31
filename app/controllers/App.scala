package controllers

import java.util

import models.{AbstractLesson, FSiRLesson, IMEILesson, Lesson}
import play.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc.{Action, Controller}
import scala.collection.JavaConversions._

/**
  * @author Vladimir Ulyanov
  */
object App extends Controller {

  case class FilterData(groupNumber: Option[String], instructor: List[String])


  val filterForm = Form(
    mapping(
      "groupNumber" -> optional(text),
      "instructor" -> list(text)
    )(FilterData.apply)(FilterData.unapply)
  )

  def allGroups = new IMEILesson().all().toList.map(lesson => lesson.getGroupNumber).distinct.sorted

  def allInstructors = new IMEILesson().all().toList.map(lesson => lesson.getInstructor).distinct.sorted

  def groupSchedule() = Action {
    Ok("todo")
  }

  def index() = Action { implicit request =>

    filterForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        val lessonList = new IMEILesson().all()
        BadRequest(views.html.index(formWithErrors, lessonList, allGroups))
      },
      userData => {
        userData match {
            case FilterData(None, Nil) =>
                var lessonList = IMEILesson.find.orderBy("dayOfWeek asc, fromHours asc").findList()
                Ok(views.html.index(filterForm.fill(userData), lessonList))
            case FilterData(Some(group), Nil) =>
              val lessonList = IMEILesson.find.where().ilike("groupNumber", group).orderBy("dayOfWeek asc, fromHours asc").findList()
              Ok(views.html.index(filterForm.fill(userData), lessonList))
            case FilterData(None, instructors) =>
              val lessonList = IMEILesson.find.orderBy("dayOfWeek asc, fromHours asc").findList().
                filter(lesson => instructors.contains(lesson.getInstructor))
              Ok(views.html.index(filterForm.fill(userData), lessonList))
            case FilterData(Some(group), instructors) =>
              val lessonList = IMEILesson.find.where().ilike("groupNumber", group).orderBy("dayOfWeek asc, fromHours asc")
                .findList().filter(lesson => instructors.contains(lesson.getInstructor))
              Ok(views.html.index(filterForm.fill(userData), lessonList))
        }
      }
    )
  }

  def lessonSorter(l1: IMEILesson, l2: IMEILesson): Boolean = {
    (l1.getDayOfWeek < l2.getDayOfWeek())
  }
}
