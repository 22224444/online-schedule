package controllers;

import models.FSiRLesson;
import models.IMEILesson;
import models.Lesson;
import models.PhysLesson;
import play.*;
import play.mvc.*;
import views.html.*;
import play.data.*;

import java.io.*;
import java.util.*;

import play.db.ebean.*;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class ClientAPI extends Controller {

    public static Result findLessons(String groupNumber, String faculty) {
        ObjectNode result = Json.newObject();
        if(faculty.equals("IMEI")) {
            List<IMEILesson> lessonList;
            lessonList = IMEILesson.find.where()
                    .ilike("groupNumber", "%" + groupNumber + "%").findList();
            JsonNode lessonsListJson = Json.toJson(lessonList);
            result.put("objects", lessonsListJson);

        } else if (faculty.equals("FSiR")) {
            List<FSiRLesson> lessonList;
                lessonList = FSiRLesson.find.where()
                        .ilike("groupNumber", "%" + groupNumber + "%").findList() ;
            JsonNode lessonsListJson = Json.toJson(lessonList);
            result.put("objects", lessonsListJson);
        } else if (faculty.equals("Phys")) {
            List<PhysLesson> lessonList;
            lessonList = PhysLesson.find.where()
                    .ilike("groupNumber", "%" + groupNumber + "%").findList() ;
            JsonNode lessonsListJson = Json.toJson(lessonList);
            result.put("objects", lessonsListJson);
        }
        return ok(result);
    }
}