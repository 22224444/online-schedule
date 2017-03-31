package parser;

import models.PhysLesson;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.util.CellRangeAddress;
import play.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by penpen on 01.03.17.
 */
public class PhysParser extends Parser <PhysLesson> {

    @Deprecated
    public List<PhysLesson> parseFile(File file) throws Exception {
        return parseStream(new FileInputStream(file));
    }


    public List<PhysLesson> parseURL(URL url) throws Exception {
        return parseStream(url.openStream());
    }


    public List<PhysLesson> parseStream(InputStream in) throws Exception {
        List<PhysLesson> list = new ArrayList<PhysLesson>();
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook workbook = new HSSFWorkbook(fs);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            HSSFSheet sheet = workbook.getSheetAt(i);
            list.addAll(parseSheet(sheet));
            System.out.println("Sheet " + i + " processed");
        }
        return list;
    }

    public List<PhysLesson> parseSheet(HSSFSheet sheet) throws Exception {
        List<PhysLesson> list = new ArrayList<PhysLesson>();
        org.apache.poi.hssf.usermodel.HSSFRow row;
        org.apache.poi.hssf.usermodel.HSSFCell cell;

        int ROWS_COUNT = sheet.getPhysicalNumberOfRows();
        int COLUMNS_COUNT = 60; //todo Избавиться от магических чисел
        String[][] dataBase = new String[ROWS_COUNT][COLUMNS_COUNT];
        boolean[][] MergedRegionMask = new boolean[ROWS_COUNT][COLUMNS_COUNT];
        // разбираем файл Excel в массив
        for (int i = 0; i < ROWS_COUNT; i++) {
            row = sheet.getRow(i + 1);
            if (row != null)
            for (int j = 0; j < COLUMNS_COUNT; j++) {
                cell = row.getCell(j);
                if (cell != null) {
                    switch (cell.getCellType()) {
                        case HSSFCell.CELL_TYPE_STRING:
                            dataBase[i][j] = cell.getStringCellValue().trim();
                            break;
                    }
                }
            }
        }

        for (int i=0; i<ROWS_COUNT; i++)
            for (int j=0; j<COLUMNS_COUNT; j++)
                MergedRegionMask[i][j] = false;

        // startLine - строка, где начинается "чистое" расписание. x и y - для обхода файла  "ДНИ"
        int startLine = getStartRow(dataBase)+1;
        FooterInfo footer = getFooter(dataBase);
        for (int i = 0; i < sheet.getNumMergedRegions(); i++)
        {
            CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
            if ((mergedRegion.getFirstRow() > startLine+1) && (mergedRegion.getLastRow()< getEOT(startLine, dataBase)) && (mergedRegion.getFirstColumn()>1)) {
                for (int h=mergedRegion.getFirstRow(); h<mergedRegion.getLastRow()+1; h++)
                    for (int u=mergedRegion.getFirstColumn(); u<mergedRegion.getLastColumn()+1; u++)
                        MergedRegionMask[h-1][u] = true;
                //обьединена в рамках 1 группы и чет-нечет
                if ((mergedRegion.getLastColumn()== mergedRegion.getFirstColumn()) && ((mergedRegion.getLastRow() - mergedRegion.getFirstRow())==1)) {
                    String lecture = dataBase[mergedRegion.getFirstRow()-1][mergedRegion.getFirstColumn()];
                    String group = getGroup(startLine, mergedRegion.getFirstColumn() == 5?mergedRegion.getFirstColumn()-1:mergedRegion.getFirstColumn() , dataBase);
                    String groupName = getGroupName(startLine,mergedRegion.getFirstColumn() == 5?mergedRegion.getFirstColumn()-1:mergedRegion.getFirstColumn(),dataBase);
                    if (notEmpty(lecture)) {
                        String instructor = getInstructor(lecture);
                        String room = "";
                        try {
                            room = getRoom(lecture, instructor);
                        } catch (Exception ignored) {
                        }
                        try {
                            lecture = lecture.split(instructor)[0];
                        } catch (Exception e) {
                            String[] instructors = instructor.trim().split(" ");
                            lecture = dataBase[mergedRegion.getFirstRow() - 1][mergedRegion.getFirstColumn()];
                            String[] lec = lecture.split(instructors[0]);
                            lecture = lec[0];
                            room = lec[1];
                            for (int k = 1; k < instructors.length; k++) {
                                room.replace(instructors[k], "");
                            }
                        }
                        if (lecture.contains("\n") && room.contains("\n") && instructor.contains("")) {
                            //siam twins
                            String[] lecs = lecture.split("\n");
                            String[] rooms = room.split("\n");
                            String[] inst = instructor.split("\n");
                            for (int k = 0; k < lecs.length; k++) {
                                PhysLesson lesson = new PhysLesson();
                                lesson.setGroupNumber(group);
                                lesson.setGroupName(groupName);
                                lesson.setDay(getDay(mergedRegion.getFirstRow()-1, dataBase).toUpperCase());
                                lesson.setHours(getHours(mergedRegion.getFirstRow()-1, dataBase));
                                lesson.setLecture(lecs[i]);
                                lesson.setInstructor(inst[i]);
                                lesson.setRoom(rooms[i]);
                                list.add(lesson);
                            }
                        } else {
                            PhysLesson lesson = new PhysLesson();
                            lesson.setGroupNumber(group);
                            lesson.setGroupName(groupName);
                            lesson.setDay(getDay(mergedRegion.getFirstRow()-1, dataBase).toUpperCase());
                            lesson.setHours(getHours(mergedRegion.getFirstRow()-1, dataBase));
                            lesson.setLecture(lecture);
                            lesson.setInstructor(instructor);
                            lesson.setRoom(room);
                            list.add(lesson);
                        }
                    }
                }
                //объединена в рамках нескольких курсов
                else if ((mergedRegion.getLastColumn()-mergedRegion.getFirstColumn()) >=1) {
                    for (int b= mergedRegion.getFirstColumn(); b<mergedRegion.getLastColumn(); b++) {
                        String lecture = dataBase[mergedRegion.getFirstRow()-1][mergedRegion.getFirstColumn()];
                        String group = getGroup(startLine, b==5?b-1:b, dataBase);
                        String groupName = getGroupName(startLine,b==5?b-1:b,dataBase);
                        if (notEmpty(lecture)) {
                            String instructor = getInstructor(lecture);
                            String room = "";
                            try {
                                room = getRoom(lecture, instructor);
                            } catch (Exception ignored) {
                            }
                            try {
                                lecture = lecture.split(instructor)[0];
                            } catch (Exception e) {
                                String[] instructors = instructor.trim().split(" ");
                                lecture = dataBase[mergedRegion.getFirstRow() - 1][mergedRegion.getFirstColumn()];
                                String[] lec = lecture.split(instructors[0]);
                                lecture = lec[0];
                                room = lec[1];
                                for (int k = 1; k < instructors.length; k++) {
                                    room.replace(instructors[k], "");
                                }
                            }
                            if (lecture.contains("\n") && room.contains("\n") && instructor.contains("")) {
                                //siam twins
                                String[] lecs = lecture.split("\n");
                                String[] rooms = room.split("\n");
                                String[] inst = instructor.split("\n");
                                for (int k = 0; k < lecs.length; k++) {
                                    PhysLesson lesson = new PhysLesson();
                                    lesson.setGroupNumber(group);
                                    lesson.setGroupName(groupName);
                                    lesson.setDay(getDay(mergedRegion.getFirstRow()-1, dataBase).toUpperCase());
                                    lesson.setHours(getHours(mergedRegion.getFirstRow()-1, dataBase));
                                    lesson.setLecture(lecs[i]);
                                    lesson.setInstructor(inst[i]);
                                    lesson.setRoom(rooms[i]);
                                    list.add(lesson);
                                }
                            } else {
                                PhysLesson lesson = new PhysLesson();
                                lesson.setGroupNumber(group);
                                lesson.setGroupName(groupName);
                                lesson.setDay(getDay(mergedRegion.getFirstRow()-1, dataBase).toUpperCase());
                                lesson.setHours(getHours(mergedRegion.getFirstRow()-1, dataBase));
                                lesson.setLecture(lecture);
                                lesson.setInstructor(instructor);
                                lesson.setRoom(room);
                                list.add(lesson);
                            }
                        }
                    }
                }
            }
            // Just add it to the sheet on the new workbook.

        }
        //todo ВЕРХНЯЯ НЕДЕЛЯ
        //todo НИЖНЯЯ НЕДЕЛЯ
        int PrevCourse = 0;
        for (int y = 2; !endOfColumns(startLine, y, dataBase); y ++) {
            String group = getGroup(startLine, y==5?y-1:y, dataBase);
            String groupName = getGroupName(startLine,y==5?y-1:y,dataBase);
            if (groupName == null) continue; //конец расписания, дальше столбцы не содержат групп
            Logger.debug(groupName);
            getEOT(startLine, dataBase);
            for (int x = startLine + 1; x < getEOT(startLine, dataBase); x++) {
                //todo: continue on mask
                if (MergedRegionMask[x][y]) {
                    continue;
                }
                String lecture = dataBase[x][y];
                if (PrevCourse == group.charAt(2))
                    lecture = GetLLecture(x, y, dataBase);
                if (notEmpty(lecture)) {
                    String instructor = getInstructor(lecture);
                    String room = "";
                    try {
                        room = getRoom(lecture, instructor);
                    } catch (Exception ignored) {}
                    try {
                        lecture = lecture.split(instructor)[0];
                    } catch (Exception e) {
                        String[] instructors = instructor.trim().split(" ");
                        lecture = dataBase[x][y];
                        String[] lec = lecture.split(instructors[0]);
                        lecture = lec[0];
                        room = lec[1];
                        for(int i=1; i<instructors.length; i++) {
                            room.replace(instructors[i], "");
                        }
                    }
                    if (lecture.contains("\n") && room.contains("\n") && instructor.contains("")) {
                        //siam twins
                        String[] lecs = lecture.split("\n");
                        String[] rooms = room.split("\n");
                        String[] inst = instructor.split("\n");
                        for (int i = 0; i < lecs.length; i++) {
                            PhysLesson lesson = new PhysLesson();
                            lesson.setGroupNumber(group);
                            lesson.setGroupName(groupName);
                            lesson.setDay(getDay(x, dataBase).toUpperCase());
                            lesson.setHours(getHours(x, dataBase));
                            lesson.setLecture(lecs[i]);
                            lesson.setInstructor(inst[i]);
                            lesson.setRoom(rooms[i]);
                            list.add(lesson);
                        }
                    } else {
                        PhysLesson lesson = new PhysLesson();
                        lesson.setGroupNumber(group);
                        lesson.setGroupName(groupName);
                        lesson.setDay(getDay(x, dataBase).toUpperCase());
                        lesson.setHours(getHours(x, dataBase));
                        lesson.setLecture(lecture);
                        lesson.setInstructor(instructor);
                        lesson.setRoom(room);
                        list.add(lesson);
                    }
                }
            }
            PrevCourse = (int) group.charAt(2);
        }
        //различаем верхние и нижние недели
        /*for (int i = 0; i < list.size() - 1; i++) {
            Lesson lesson1 = list.get(i);
            Lesson lesson2 = list.get(i + 1);
            if (isUpperLowerWeekPair(lesson1, lesson2)) {
                lesson1.setWeek(1);
                lesson2.setWeek(2);
            } else {
                lesson1.setWeek(0);
            }
        }*/

        return list;
    }

    private static String GetLLecture(int x, int y, String[][] dataBase) throws Exception {
        String s = dataBase[x][y];
        if ((s == null) || (!notEmpty(s))) {
            return GetLLecture(x, y-1, dataBase);
        } else {
            //([0-9]+ час. [0-9]+ мин.)
            Pattern InstructorPattern = Pattern.compile("([0-9]+ час. [0-9]+ мин.)");
            Matcher m = InstructorPattern.matcher(s);
            if (m.matches())
                return "";
            else
                return s;
        }
    }

    private static int getEOT(int x, String[][] dataBase) throws Exception {
        int y = 0;
        for (int i=0; i<dataBase.length;i++) {
            for (int j=0; j<dataBase[i].length; j++) {
                if ((dataBase[i][j] != null) && (dataBase[i][j].equals("Начальник УМУ"))) {
                    return i-1;
                }
            }
        }
        throw new Exception("Ошибка парсинга, не найден конец таблицы");
    }

    private static boolean isUpperLowerWeekPair(Lesson lesson1, Lesson lesson2) {
        return Objects.equals(lesson1.getFromHours(), lesson2.getFromHours()) && Objects.equals(lesson1.getFromMinutes(), lesson2.getFromMinutes()) &&
                lesson1.getGroupNumber().equals(lesson2.getGroupNumber()) && Objects.equals(lesson1.getDayOfWeek(), lesson2.getDayOfWeek())
                && !lesson1.getLecture().contains("по выбору") && !lesson2.getLecture().contains("по выбору");
    }

    private static boolean endOfColumns(int x, int y, String[][] dataBase) {
        String s = dataBase[x][y];
        return  y ++ >= dataBase[x].length-1;
    }

    private static String getHours(int row, String[][] db) {
        String ret;
        if (row % 2 == 0)
            ret = db[row-1][1].replace(" час", "").replace(" мин.", "") + "-" + db[row][1].replace(" час", "").replace(" мин.", "") ;
        else
            ret = db[row][1].replace(" час", "").replace(" мин.", "")  + "-" + db[row+1][1].replace(" час", "").replace(" мин.", "") ;
        if (notEmpty(ret)) return ret;
        else return getHours(row - 1, db);
    }

    private static String getDay(int row, String[][] db) {
        String ret = db[row][0];
        if (notEmpty(ret)) return ret;
        else return getDay(row - 1, db);
    }

    private static String getInstructor(String lecture) throws Exception {
        Pattern InstructorPattern = Pattern.compile("([А-Я][а-я]+ \\W\\.\\W\\.,* )+");
        Matcher m = InstructorPattern.matcher(lecture);
        String res = "";
        while (m.find()) {
            res+= m.group(0);

        }

        if (notEmpty(res)) return res;
        else return "";
    }

    private static String getRoom(String lecture, String split) throws Exception {
        if (notEmpty(lecture)) return lecture.split(split)[1];
        else return "";
    }

    private static boolean isGroupTitle(String cell) {
        return "01".equals(cell.substring(0, 2));
    }

    private static boolean notEmpty(String cell) {
        return cell != null && !"".equals(cell);
    }

    private static String getGroup(int x, int y, String[][] dataBase) {
        if (dataBase[x][y] != null) {
            if (isGroupTitle(dataBase[x][y])) {
                return dataBase[x][y];
            } else {
                return dataBase[x + 1][y];
            }
        } else {
            return dataBase[x + 1][y];
        }
    }
    private static String getGroupName(int x, int y, String[][] dataBase) {
        if (dataBase[x][y] != null) {
            if (isGroupTitle(dataBase[x][y])) {
                return dataBase[x][y];
            } else {
                return dataBase[x][y];
            }
        } else {
            return null;
        }
    }

    private static int getStartRow(String[][] dataBase) {
        // ищем, где начинается расписание
        for (int x = 0; x < 100; x++) {
            if ("1 курс".equals(dataBase[x][2])) {
                return x;
            }
        }
        throw new IllegalArgumentException("Данный лист имеет неправильный формат,в первом столбце должны быть Дни");
    }

    private static FooterInfo getFooter(String[][] dataBase){
        FooterInfo ret = new FooterInfo();
        ret.minRow = dataBase.length;
        for (int row = dataBase.length-1; row>=0; row-- ){
            for (int j = 0; j < dataBase[row].length; j++) {
                String value = dataBase[row][j];
                if(value!=null && value.toLowerCase().contains("нижняя")) {
                    ret.minRow = row;
                    //find value of lower weak
                    ret.lower = findFirstRightValue(row, j+1, dataBase);
                    if (ret.lower == null) ret.lower = value;
                }
                if(value!=null && value.toLowerCase().contains("верхняя")) {
                    ret.minRow = row;
                    //find value of lower weak
                    ret.upper = findFirstRightValue(row, j+1, dataBase);
                    if (ret.upper == null) ret.upper = value;
                }
                if(ret.lower!=null && ret.upper != null) break;
            }
        }
        return ret;
    }

    private static String findFirstRightValue(int row, int i, String[][] dataBase) {
        if (i >= dataBase[row].length || row >= dataBase.length) return null;
        else if (notEmpty(dataBase[row][i])) return dataBase[row][i];
        else return findFirstRightValue(row, i+1, dataBase);
    }


}
