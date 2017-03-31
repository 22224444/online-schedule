package models;

import java.util.*;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlUpdate;
import play.db.ebean.*;

import javax.persistence.*;

@Entity
public class FSiRLesson extends AbstractLesson <FSiRLesson> {

    @Id
    private Integer id;
    private String groupNumber;
    private String groupName;
    private Integer dayOfWeek;
    private String lecture;
    private String instructor;
    private String room;

    private Integer fromHours;
    private Integer fromMinutes;
    private Integer toHours;
    private Integer toMinutes;
    private Integer week;

    public static Model.Finder<Integer, FSiRLesson> find = new Model.Finder(
            Integer.class, FSiRLesson.class
    );

    public List<FSiRLesson> all() {
        return find.all();
    }

    public static void clearBase() {
        SqlUpdate rebuildTable = Ebean.createSqlUpdate("TRUNCATE TABLE fsi_rlesson");
        try {
            rebuildTable.execute();
        } catch (Exception e) {
            System.out.println("LOGGGGG" + e.getMessage());
        }

    }

    private String zs(Integer n) {
        return String.format("%02d", n);
    }

    private Calendar nextDay() {
        Calendar date = Calendar.getInstance();

        while (date.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            date.add(Calendar.DATE, 1);
        }

        return date;
    }

    public static FSiRLesson from(FSiRLesson from) {
        FSiRLesson to = new FSiRLesson();
        to.setDayOfWeek(from.getDayOfWeek());
        to.setWeek(from.getWeek());
        to.setFromHours(from.getFromHours());
        to.setFromMinutes(from.getFromMinutes());
        to.setGroupNumber(from.getGroupNumber());
        to.setGroupName(from.getGroupName());
        to.setInstructor(from.getInstructor());
        to.setLecture(from.getLecture());
        to.setRoom(from.getRoom());
        to.setToHours(from.getToHours());
        to.setToMinutes(from.getToMinutes());
        return to;
    }
}