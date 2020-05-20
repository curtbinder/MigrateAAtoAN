package info.curtbinder.java.MigrateAAtoAN;

import com.opencsv.bean.CsvToBeanBuilder;

import java.io.*;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Main m = new Main();
        m.migrate();
    }

    private String anDBName = "userdata.db";
    private Connection m_Conn = null;
    private List<String> m_ImagesToMove = new ArrayList<>();

    Main() {
        m_Conn = getDBConnection();
    }

    public void migrate() {
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
}
