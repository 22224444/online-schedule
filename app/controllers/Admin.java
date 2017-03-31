package controllers;

import models.*;
import parser.IMEIParser;
import parser.PhysParser;
import play.Logger;
import play.libs.Akka;
import play.mvc.Controller;
import play.mvc.*;
import scala.concurrent.duration.Duration;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Vladimir Ulyanov
 */
public class Admin extends Controller {
    public static Result reload() {
        startReload();
        return ok("reloaded");//redirect(controllers.routes.Application.adminPage());
    }

    public static void startReload(){
        Akka.system().scheduler().scheduleOnce(
                Duration.create(0, TimeUnit.SECONDS),
                new Runnable() {
                    public void run() {
                        long before = System.currentTimeMillis();
                        List<ScheduleURL> urls = ScheduleURL.all();
                        Lesson.clearBase();
                        IMEILesson.clearBase();
                        PhysLesson.clearBase();
                        FSiRLesson.clearBase();
                        for (ScheduleURL schedule : urls) {
                            try {
                                if (schedule.url.contains("physdep")) {
                                    List<PhysLesson> list;
                                    PhysParser parser = new PhysParser();
                                    list = parser.parseURL(new URL(schedule.url));
                                    for (models.PhysLesson lesson : list)
                                        models.PhysLesson.from(lesson).save();
                                } else if (schedule.url.contains("math")) {
                                    List<IMEILesson> list;
                                    IMEIParser parser = new IMEIParser();
                                    list = parser.parseURL(new URL(schedule.url));
                                    for (models.IMEILesson lesson : list)
                                        models.IMEILesson.from(lesson).save();
                                }
                            } catch (Exception e) {
                                Logger.error("Can not parse the URL:" + schedule.url);
                            }
                        }
                        System.out.println("downloaded and processed for "+(System.currentTimeMillis()-before) +" ms.");
                    }
                },
                Akka.system().dispatcher()
        );
        try {
            ScheduleClass.reloadFSiR();
        } catch (Exception ignored) {}

    }
}
