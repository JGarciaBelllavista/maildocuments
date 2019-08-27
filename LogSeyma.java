/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amxa
 */
public class LogSeyma {

    private static final String absoluteLogFile = Config.param(Config.LOG_FILE_PATH);
    private static final SimpleDateFormat dFormater = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.S");
    private static final SimpleDateFormat dayFormater = new SimpleDateFormat("_YYYY-MM-dd.");
    private static final EmailSenderDoc sender = new EmailSenderDoc();

    public static void print(String line) {
        write(line, false);
    }

    public static void println(String newLine) {
        write(newLine, true);
        System.out.println(newLine);
    }

     public static void printerr(String newLine) {
        println("ERROR: " + newLine);
        sender.sendError("ERROR: MailDoctuments", newLine);
    }

     public static void printexcp(String newLine) {
        println("EXCEOPTION: " + newLine);
        sender.sendException("EXCEOPTION: MailDoctuments", newLine);
    }
     
     /**
     * Write log in LOG_FILE_PATH
     *
     * @param logLine
     */
    private static void write(String logLine, boolean addNewLine) {
        try {
            File file = new File(absoluteLogFile.replace(".",dayFormater.format(new Date())));
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            if (addNewLine) {
                bw.append(dFormater.format(new Date()) + "\t" + logLine);
                bw.newLine();
            } else {
                bw.write(logLine);
            }
            bw.flush();
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(LogSeyma.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
