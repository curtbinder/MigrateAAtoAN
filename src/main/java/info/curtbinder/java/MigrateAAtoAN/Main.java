/*
 * MIT License
 * Copyright (c) 2020 Curt Binder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package info.curtbinder.java.MigrateAAtoAN;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Main m = new Main();
        //m.migrateActivities();
        m.migrateParameters();
    }

    private String anDBName = "userdata.db";
    private Connection m_Conn = null;
    private List<String> m_ImagesToMove = new ArrayList<>();
    private List<ParamTests> m_dbParameters = new ArrayList<>();

    Main() {
        m_Conn = getDBConnection();
    }

    public void migrateActivities() {
        List<Activities> activities = readAALogEntries("Activities.csv");
        LocalDateTime dt;
        // loop through activities
        int count = 0;
        int size = activities.size();
        for (Activities a : activities) {
            ++count;
            long millis = a.getActivitydatetimemilis();
            if (millis == 0) {
                dt = LocalDateTime.of((int)a.getExecuteyear(), (int)a.getExecutemonth()+1,
                        (int)a.getExecuteday(), (int)a.getExecutehour(), (int)a.getExecuteminute());
            } else {
                dt = millisToLocalDateTime(millis);
            }

            // insert it into the Aquarium Note database
            System.out.print(count + "/" + size + " Inserting '" + a.getName() + "'...");
            if (!insertActivityToDb(a.getName(), a.getContentdata(), a.getProfileimagelocallink(), dt)) {
                // Failed, so exit
                System.out.println("FAILED.");
                break;
            }
            System.out.println("OK.");
        }
        printImagesToMove();
    }

    public List<Activities> readAALogEntries(String filename) {
        System.out.println("Reading AquariumAssistant activities...");
        List<Activities> aa = null;
        try {
             aa = new CsvToBeanBuilder<Activities>(new FileReader(filename))
                    .withType(Activities.class).build().parse();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Finished reading activities. " + aa.size() + " found.");
        return aa;
    }

    public static DateTimeFormatter getANFilenameFormat() {
        return DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    }

    public static LocalDateTime millisToLocalDateTime(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public boolean insertActivityToDb(String title, String content, String image, LocalDateTime dt) {
        boolean fRet = false;
        String sqlInsert = "insert into log(year, month, date, aquarium, log, text1, int1, int2) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = m_Conn.prepareStatement(sqlInsert);
            pst.setInt(1, dt.getYear());
            pst.setInt(2, dt.getMonthValue()-1); // subtract month by 1
            pst.setInt(3, dt.getDayOfMonth());
            pst.setInt(4, 1);
            pst.setString(5, convertAALogToANLog(title, content));
            pst.setString(6, convertAAImageToANImage(image, dt));
            pst.setInt(7, dt.getHour());
            pst.setInt(8, dt.getMinute());
            pst.executeUpdate();
            fRet = true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return fRet;
    }

    public String convertAALogToANLog(String title, String content) {
        return content + SEPARATOR + title;
    }

    public String convertAAImageToANImage(String filename, LocalDateTime dt) {
        String newImage = null;
        if (!filename.isEmpty() && !filename.equals("<null>")) {
            File f = new File(filename);
            String oldName = f.getName();
            newImage = dt.format(getANFilenameFormat()) + "00s.jpg";
            String s = "cp " + oldName + " an/" + newImage;
            m_ImagesToMove.add(s);
        }
        return newImage;
    }

    public void printImagesToMove() {
        try {
            FileWriter fw = new FileWriter(new File("copyimages.sh"));
            fw.write("#!/bin/sh\n\n");
            fw.write("## Run this file in the AquariumAssistant Backup folder\n");
            fw.write("## It will copy the appropriate files to their new name\n");
            fw.write("## in the folder an/ inside the current directory.\n\n");
            fw.write("mkdir an\n");
            for (String img : m_ImagesToMove) {
                fw.write(img);
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Split char in LOG field is a double cross, unicode char U2021
    public static int DOUBLE_CROSS = 0x2021;
    public static String SEPARATOR = Character.toString(DOUBLE_CROSS);
    public void outputANActivities() {
        String sql = "select _id, year, month, date, int1, int2, log from log";

        try {
            Statement stmt = m_Conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            String outputFormat = "%4d) Title: %s\n       Note: %s";
            while (rs.next()) {
                ++count;
                // print out the data
                String log = rs.getString("log");
                int id = rs.getInt("_id");
                if (log.indexOf(DOUBLE_CROSS) > -1) {
                    // split the string
                    String[] parts = log.split(SEPARATOR);
                    if (parts.length > 1) {
                        System.out.println(String.format(outputFormat, id, parts[1], parts[0]));
                    }
                } // Log always contains double cross
            }
            System.out.println("Records: " + count);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private Connection getDBConnection() {
        String url = "jdbc:sqlite:" + anDBName;
        Connection conn = null;
        try {
            // create connection to the database
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void migrateParameters() {
        System.out.println("Migrate Parameters");
        clearSkippedParametersFile();
        readANParameters();
        List<ParamTests> paramTests = getWaterTests("WaterTest.csv");
        int count = 0;
        int size = paramTests.size();
        for (ParamTests pt : paramTests) {
            ++count;
            System.out.print(count + "/" + size + " Inserting " + pt.titleString() + "...");
            int index = isParameterAlreadyLogged(pt);
            if (index > -1) {
                System.out.println("SKIPPED.");
                logSkippedParameters(index, pt);
            } else {
                if (!insertParameterToDb(pt)) {
                    System.out.println("FAILED.");
                    break;
                }
                System.out.println("OK.");
            }
        }
    }

    private int isParameterAlreadyLogged(ParamTests pt) {
        return m_dbParameters.indexOf(pt);
    }

    private boolean insertParameterToDb(ParamTests pt) {
        boolean fRet = false;
        // MUST INSERT a string into almost all fields or android app will crash
        // Need to add ammonia, nitrite, phosphate, user2 - user12
        String sqlInsert = "insert into parameter("
                + "year, month, date, aquarium, ph, temperature, nitrate, "
                + "salinity, alkalinity, calcium, magnesium, user1, "
                + "int1, int2, notes, ammonia, nitrite, phosphate, "
                + "user2, user3, user4, user5, user6, user7, user8, user9, user10, user11, user12"
                + ") values (" +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "?, ?, ?, ?"
                + ")";
        try {
            PreparedStatement pst = m_Conn.prepareStatement(sqlInsert);
            pst.setInt(1, pt.getDateTime().getYear());  // year
            pst.setInt(2, pt.getDateTime().getMonthValue() - 1);  // month (minus 1)
            pst.setInt(3, pt.getDateTime().getDayOfMonth());  // day
            pst.setInt(4, 1);  // aquarium
            pst.setString(5, pt.getPhString());  // ph
            pst.setString(6, pt.getTemperatureString());  // temperature
            pst.setString(7, pt.getNitrateString());  // nitrate
            pst.setString(8, pt.getSalinityString());  // salinity
            pst.setString(9, pt.getAlkalinityString());  // alkalinity
            pst.setString(10, pt.getCalciumString());  // calcium
            pst.setString(11, pt.getMagnesiumString());  // magnsium
            pst.setString(12, pt.getPhosphorusString());  // phosphorus
            pst.setInt(13, pt.getDateTime().getHour());  // hour
            pst.setInt(14, pt.getDateTime().getMinute());  // minute
            pst.setString(15, pt.getNotes());
            pst.setString(16, "");
            pst.setString(17, "");
            pst.setString(18, "");
            pst.setString(19, "");
            pst.setString(20, "");
            pst.setString(21, "");
            pst.setString(22, "");
            pst.setString(23, "");
            pst.setString(24, "");
            pst.setString(25, "");
            pst.setString(26, "");
            pst.setString(27, "");
            pst.setString(28, "");
            pst.setString(29, "");
            pst.executeUpdate();
            fRet = true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return fRet;
    }

    public void clearSkippedParametersFile() {
        try {
            FileWriter fw = new FileWriter(new File("skipped_params.txt"));
            fw.write("# Parameters that were skipped during import.\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logSkippedParameters(int index, ParamTests p) {
        // write out both parameter sets to a log file for comparison
        try {
            FileWriter fw = new FileWriter(new File("skipped_params.txt"), true);
            fw.write("* NEW: ");
            fw.write(p.toString());
            fw.write("\n");
            fw.write("   AN: ");
            fw.write(m_dbParameters.get(index).toString());
            fw.write("\n----\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readANParameters() {
        /*
        Read the Aquarium Note Parameters
         */
        System.out.println("Read Aquarium Note Parameters");
        String sql = "select "
                + "year, month, date, aquarium, ph, temperature, nitrate, "
                + "salinity, alkalinity, calcium, magnesium, user1, "
                + "int1, int2"
                + " from parameter";

        try {
            Statement stmt = m_Conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            int count = 0;
            while (rs.next()) {
                ++count;
                ParamTests pt = new ParamTests();
                // store the data
                LocalDateTime dt = LocalDateTime.of(
                        rs.getInt("year"),
                        rs.getInt("month")+1,
                        rs.getInt("date"),
                        rs.getInt("int1"),
                        rs.getInt("int2")
                );
                pt.setDateTime(dt);
                String s = rs.getString("ph");
                if (!s.isEmpty()) {
                    pt.setPh(new BigDecimal(s));
                }
                s = rs.getString("temperature");
                if (!s.isEmpty()) {
                    pt.setTemperature(new BigDecimal(s));
                }
                s = rs.getString("nitrate");
                if (!s.isEmpty()) {
                    pt.setNitrate(new BigDecimal(s));
                }
                s = rs.getString("salinity");
                if (!s.isEmpty()) {
                    pt.setSalinity(new BigDecimal(s));
                }
                s = rs.getString("alkalinity");
                if (!s.isEmpty()) {
                    pt.setAlkalinity(new BigDecimal(s));
                }
                s = rs.getString("calcium");
                if (!s.isEmpty()) {
                    pt.setCalcium(Integer.parseInt(s));
                }
                s = rs.getString("magnesium");
                if (!s.isEmpty()) {
                    pt.setMagnesium(Integer.parseInt(s));
                }
                s = rs.getString("user1");
                if (!s.isEmpty()) {
                    pt.setPhosphorus(Integer.parseInt(s));
                }
                m_dbParameters.add(pt);
            }
            System.out.println("Records: " + count);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private List<WaterTest> getAAParameters(String filename) {
        System.out.println("Reading AquariumAssistant water tests...");
        List<WaterTest> aa = null;
        try {
            aa = new CsvToBeanBuilder<WaterTest>(new FileReader(filename))
                    .withType(WaterTest.class).build().parse();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Finished reading water tests. " + aa.size() + " found.");
        return aa;
    }

    public List<ParamTests> getWaterTests(String filename) {
        List<WaterTest> waterTests = getAAParameters(filename);
        // Take the WaterTest list and put it into our own simplified list
        ArrayList<ParamTests> paramTests = new ArrayList<>();
        String sConvert;
        double d;
        float f;
        int i;
        for (WaterTest test : waterTests) {
            long millis = test.getTestdatetimemilis();
            ParamTests pt = new ParamTests();
            pt.setDateTime(millisToLocalDateTime(millis));
            if (test.isIs_Value_Ph()) {
                // up to 2 decimal points
                d = test.getValue_Ph();
                sConvert = String.format("%.2f", d);
                pt.setPh(new BigDecimal(sConvert));
            }
            if (test.isIs_Value_Temperature()) {
                // up to 1 decimal point
                d = test.getValue_Temperature();
                sConvert = String.format("%.1f", d);
                pt.setTemperature(new BigDecimal(sConvert));
            }
            if (test.isIs_Value_Salinity()) {
                // up to 1 decimal point
                d = test.getValue_Salinity();
                sConvert = String.format("%.1f", d);
                pt.setSalinity(new BigDecimal(sConvert));
            }
            if (test.isIs_Value_Alkalinity()) {
                // up to 2 decimal points
                d = test.getValue_Alkalinity();
                sConvert = String.format("%.2f", d);
                pt.setAlkalinity(new BigDecimal(sConvert));
            }
            if (test.isIs_Value_Ca()) {
                f = test.getValue_Ca();
                i = (int) f;
                pt.setCalcium(i);
            }
            if (test.isIs_Value_Mg()) {
                f = test.getValue_Mg();
                i = (int) f;
                pt.setMagnesium(i);
            }
            if (test.isIs_Value_No3()) {
                // up to 2 decimal points
                d = test.getValue_No3();
                sConvert = String.format("%.2f", d);
                pt.setNitrate(new BigDecimal(sConvert));
            }
            if (test.isIs_Value_User_1()) {
                // Phosphorus
                f = test.getValue_User_1();
                i = (int) f;
                pt.setPhosphorus(i);
            }
            paramTests.add(pt);
        }
        return paramTests;
    }
}
