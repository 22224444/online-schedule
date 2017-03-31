package parser;


import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import play.Logger;


public abstract class Parser <T>{

    @Deprecated
    public abstract List<T> parseFile(File file) throws IOException, Exception;

    public abstract List<T> parseURL(URL url) throws IOException, Exception;


    public abstract List<T> parseStream(InputStream in) throws IOException, Exception;


    public abstract List<T> parseSheet(HSSFSheet sheet) throws Exception;

    private static boolean isUpperLowerWeekPair(Lesson lesson1, Lesson lesson2) {
        return Objects.equals(lesson1.getFromHours(), lesson2.getFromHours()) && Objects.equals(lesson1.getFromMinutes(), lesson2.getFromMinutes()) &&
                lesson1.getGroupNumber().equals(lesson2.getGroupNumber()) && Objects.equals(lesson1.getDayOfWeek(), lesson2.getDayOfWeek())
                && !lesson1.getLecture().contains("по выбору") && !lesson2.getLecture().contains("по выбору");
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
    private static String getGroupName(int x, int y, String[][] dataBase) {
        if (dataBase[x][y] != null) {
            if (isGroupTitle(dataBase[x][y])) {
                return dataBase[x-1][y];
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

    public static class FooterInfo {
        int minRow;
        String upper;
        String lower;
    }
}