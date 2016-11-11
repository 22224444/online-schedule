package parser;


import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public class Parser {

    public static void parseAndStore(File file) throws IOException {
        List<Lesson> ret = parseFile(file);
        for (Lesson lesson : ret) {
            models.Lesson.from(lesson).save();
        }
    }





    @Deprecated
    public static List<Lesson> parseFile(File file) throws IOException {
        return parseStream(new FileInputStream(file));
    }

    public static List<Lesson> parseURL(URL url) throws IOException {
        return parseStream(url.openStream());
    }


    public static List<Lesson> parseStream(InputStream in) throws IOException {
        List<Lesson> list = new ArrayList<Lesson>();
        POIFSFileSystem fs = new POIFSFileSystem(in);
        HSSFWorkbook workbook = new HSSFWorkbook(fs);

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            HSSFSheet sheet = workbook.getSheetAt(i);
            list.addAll(parseSheet(sheet));
            System.out.println("Sheet " + i + " processed");
        }
        return list;
    }



    private static List<Lesson> parseSheet(HSSFSheet sheet) {
        List<Lesson> list = new ArrayList<Lesson>();
        org.apache.poi.hssf.usermodel.HSSFRow row;
        org.apache.poi.hssf.usermodel.HSSFCell cell;

        int ROWS_COUNT = 240;   //todo Избавиться от магических чисел
        int COLUMNS_COUNT = 60; //todo Избавиться от магических чисел
        String[][] dataBase = new String[ROWS_COUNT][COLUMNS_COUNT];
        // разбираем файл Excel в массив
        for (int i = 0; i < ROWS_COUNT; i++) {
            row = sheet.getRow(i + 1);
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

        // startLine - строка, где начинается "чистое" расписание. x и y - для обхода файла  "ДНИ"
        int startLine = getStartRow(dataBase);
        FooterInfo footer = getFooter(dataBase);
        //todo ВЕРХНЯЯ НЕДЕЛЯ
        //todo НИЖНЯЯ НЕДЕЛЯ
        for (int y = 2; !endOfColumns(startLine, y, dataBase); y += 3) {
            String group = getGroup(startLine, y, dataBase);
            if (group == null) break; //конец расписания, дальше столбцы не содержат групп

            for (int x = startLine + 2; x < footer.minRow; x++) {
                String lecture = dataBase[x][y];
                if (notEmpty(lecture)) {
                    String room = getRoom(x, y, dataBase);
                    String instructor = getInstructor(x, y, dataBase);
                    if (lecture.contains("\n") && room.contains("\n") && instructor.contains("")) {
                        //siam twins
                        String[] lecs = lecture.split("\n");
                        String[] rooms = room.split("\n");
                        String[] inst = instructor.split("\n");
                        for (int i = 0; i < lecs.length; i++) {
                            Lesson lesson = new Lesson();
                            lesson.setGroupNumber(group);
                            lesson.setDay(getDay(x, dataBase));
                            lesson.setHours(getHours(x, dataBase));
                            lesson.setLecture(lecs[i]);
                            lesson.setInstructor(inst[i]);
                            lesson.setRoom(rooms[i]);
                            list.add(lesson);
                        }
                    } else {
                        Lesson lesson = new Lesson();
                        lesson.setGroupNumber(group);
                        lesson.setDay(getDay(x, dataBase));
                        lesson.setHours(getHours(x, dataBase));
                        lesson.setLecture(lecture);
                        lesson.setInstructor(instructor);
                        lesson.setRoom(room);
                        list.add(lesson);
                    }
                }
            }
        }
        //todo различать верхние и нижние недели
        for (int i = 0; i < list.size() - 1; i++) {
            Lesson lesson1 = list.get(i);
            Lesson lesson2 = list.get(i + 1);
            if (lesson1.getFromHours() == lesson2.getFromHours() && lesson1.getFromMinutes() == lesson2.getFromMinutes() &&
                    lesson1.getGroupNumber().equals(lesson2.getGroupNumber()) && lesson1.getDayOfWeek() == lesson2.getDayOfWeek()) {
                System.out.println(lesson1.toString() + " верхняя неделя");
                System.out.println(lesson2.toString() + " нижняя неделя");

            }
        }

        return list;
    }

    private static boolean endOfColumns(int x, int y, String[][] dataBase) {
        String s = dataBase[x][y];
        return "Часы".equals(s) || y + 2 >= dataBase[x].length;
    }

    private static String getHours(int row, String[][] db) {
        String ret = db[row][1];
        if (notEmpty(ret)) return ret;
        else return getHours(row - 1, db);
    }

    private static String getDay(int row, String[][] db) {
        String ret = db[row][0];
        if (notEmpty(ret)) return ret;
        else return getDay(row - 1, db);
    }

    private static String getInstructor(int x, int y, String[][] dataBase) {
        String instructor = dataBase[x][y + 1];
        if (notEmpty(instructor)) return instructor;
        else return getInstructor(x - 1, y, dataBase);
    }

    private static String getRoom(int x, int y, String[][] dataBase) {
        String ret = dataBase[x][y + 2];
        if (notEmpty(ret)) return ret;
        else return getRoom(x - 1, y, dataBase);
    }

    private static boolean isGroupTitle(String cell) {
        return "02".equals(cell.substring(0, 2));
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

    private static int getStartRow(String[][] dataBase) {
        // ищем, где начинается расписание
        for (int x = 0; x < 100; x++) {
            if ("Дни".equals(dataBase[x][0])) {
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
                }
                if(value!=null && value.toLowerCase().contains("верхняя")) {
                    ret.minRow = row;
                    //find value of lower weak
                    ret.upper = findFirstRightValue(row, j+1, dataBase);
                }
                if(ret.lower!=null && ret.upper != null) break;
            }
        }
        return ret;
    }

    private static String findFirstRightValue(int row, int i, String[][] dataBase) {
        if (notEmpty(dataBase[row][i])) return dataBase[row][i]; else return findFirstRightValue(row, i+1, dataBase);
    }

    private static class FooterInfo {
        int minRow;
        String upper;
        String lower;
    }
}