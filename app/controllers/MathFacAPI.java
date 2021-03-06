package controllers;

import models.Lesson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

public class MathFacAPI extends Controller {

    private class Foo {
        List<String> teachers;
    }

    public static Result smartSearch(String parameters) {

        List<Lesson> teachersList = new ArrayList<>();

        JSONParser parser = new JSONParser();

        try {
            JSONObject jsonObject = (JSONObject) parser.parse(parameters);
            JSONArray teachersArray = (JSONArray) jsonObject.get("teachers");
            for(int i=0; i<teachersArray.size();i++){
                teachersList.addAll(Lesson.find.where().ilike("teacher", "%" + teachersArray.get(i) + "%")
                        .orderBy("teacher asc, day asc, hours asc").findList() );
            }
        } catch (Exception e) {
            System.out.println("ERROR");
        }

        String table =
                "<div>\n" +
                "<table>\n" ;
        for (int i=0; i < teachersList.size(); i++) {
            Lesson lesson = teachersList.get(i);
            table += "<tr><td>" + lesson.getInstructor() + "</td><td>" + lesson.getDay() + " " + lesson.getHours() + "</td><td>" + lesson.getRoom() + "</td></tr>\n";
        }
        table += "</table>\n" +
                "</div>\n";


        response().setContentType("text/html; charset=utf-8");

        return ok(table);
    }
}
