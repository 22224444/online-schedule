package controllers;

import models.FSiRLesson;
import org.jsoup.select.Elements;
import play.*;
import play.libs.Akka;
import play.mvc.*;
import scala.concurrent.duration.Duration;
import views.html.*;
import play.data.*;

import java.io.*;
import java.util.*;

import java.net.*;
import java.io.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import play.db.ebean.*;
import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


public class ScheduleClass extends Controller {

    public static Result reloadFSiR() throws IOException {
        Akka.system().scheduler().scheduleOnce(
                Duration.create(0, TimeUnit.SECONDS),
                new Runnable() {
                    public void run() {
                        long before = System.currentTimeMillis();
                        FSiRLesson.clearBase();
                        Document doc = null;
                        try {
                            doc = Jsoup.connect("https://forlabs.ru/rasp").get();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Elements groups = doc.select(".list-group-item");
                        ArrayList<String> groupList = new ArrayList<>();
                        for (Element group : groups) {
                            Document groupDoc = null;
                            try {
                                groupDoc = Jsoup.connect(group.child(0).attr("href")).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Elements table = groupDoc.select(".table-bordered");
                            String groupNum = groupDoc.select(".dropdown-toggle").text().trim();
                            String FSiRLessonTime = "";
                            for (Element row : table.get(0).child(1).children()) {
                                if ((row.tagName() != null) && (row.tagName().equals("tr"))) {
                                    int colNum = 0;
                                    for (Element col : row.children()) {
                                        FSiRLesson FSiRLesson = new FSiRLesson();
                                        FSiRLesson.setGroupNumber(groupNum);
                                        FSiRLesson.setGroupName(groupNum);
                                        if (col.tagName().equals("th")) {
                                            Elements cs = col.children();
                                            FSiRLessonTime = col.childNode(0).toString() + "-" + col.childNode(2).toString();
                                        } else if (col.tagName().equals("td")) {
                                            if (col.children().size() > 0) {
                                                String day = "";
                                                switch (colNum) {
                                                    case 0:
                                                        day = "ПОНЕДЕЛЬНИК";
                                                        break;
                                                    case 1:
                                                        day = "ВТОРНИК";
                                                        break;
                                                    case 2:
                                                        day = "СРЕДА";
                                                        break;
                                                    case 3:
                                                        day = "ЧЕТВЕРГ";
                                                        break;
                                                    case 4:
                                                        day = "ПЯТНИЦА";
                                                        break;
                                                    case 5:
                                                        day = "СУББОТА";
                                                        break;
                                                    case 6:
                                                        day = "ВОСКРЕСЕНЬЕ";
                                                        break;
                                                    default:
                                                        day = "";
                                                        break;
                                                }
                                                FSiRLesson.setDay(day);
                                                for (Element week : col.children()) {

                                                    FSiRLesson.setLecture(week.child(0).text());
                                                    FSiRLesson.setInstructor(week.childNodes().get(3).childNode(0).toString().trim());
                                                    FSiRLesson.setRoom(week.childNodes().get(3).childNode(1).childNode(0).toString().trim());

                                                    if (week.className().equals("panel panel-info uw-item")) {
                                                        FSiRLesson.setWeek(0);
                                                    } else {
                                                        FSiRLesson.setWeek(1);
                                                    }
                                                    FSiRLesson.setHours(FSiRLessonTime);
                                                    if ((FSiRLesson.getLecture() != null) && (FSiRLesson.getLecture().trim().length() > 0))
                                                        models.FSiRLesson.from(FSiRLesson).save();
                                                }
                                            }
                                        }
                                        colNum++;
                                    }
                                }
                            }
                        }
                    }
                },
                Akka.system().dispatcher()
            );

    	return ok("reloaded");
    }

    public static Result FSiR() {
        List<models.FSiRLesson> list = models.FSiRLesson.all();
        return ok(list.toString());
    }
} 
