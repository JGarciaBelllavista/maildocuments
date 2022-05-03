/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.LogSeyma;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author isivan
 */
public class MailDocuments {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            String envPath = args.length > 0 ? args[0] : System.getProperty("user.dir") + "/mail.config";
            Config.init(envPath);
            String[] mailtypes = Config.param(ConfigStr.MAIL_TYPES).split(Config.param(Config.FILE_SPR));
            if (mailtypes.length < 0) {
                System.out.println("Usage error, must supply at least one mail type in config file");
                System.exit(1);
            }
            boolean debug = Config.param(ConfigStr.DEBUG).equals("true");
            for (String mailtype : mailtypes) {
                String logtype = mailtype;
                if (debug)
                    logtype += "_DEBUG";
                LogSeyma.init(logtype, debug);
                LogSeyma.println("Ejecutado mailDocuments (mail-documents-1.1.0-rc2)");
                LogSeyma.println("Starting email sending for document type: " + mailtype);
                Conexion conn = new Conexion(mailtype);
                
                ArrayList<Email> maillist;
                Email confirmemail;
                String[] companies = Config.param(mailtype, ConfigStr.DOC_COMPANIES).split(Config.param(Config.FILE_SPR));
                for (String company : companies) {
                    maillist = conn.readPandingDocuments(company);
                    LogSeyma.println("Total number of " + mailtype + " to be genereated: " + conn.getRows_num());
                    LogSeyma.println(maillist.size() + " " + mailtype + " were generated for " + company);
                    if (maillist.size() != conn.getRows_num()) {
                        int num_diff = conn.getRows_num() - maillist.size();
                        LogSeyma.println("Number of files NOT generated:  " + num_diff);
                    }
                    LogSeyma.println("Number of email in list for " + mailtype + " to send: " + maillist.size());
                    
                    EmailSenderDoc es = new EmailSenderDoc();
                    for (Email email2send : maillist) {
                        es.send(email2send);
                    }
                    
                    LogSeyma.println("Updating SAP with state = \'Email Sent\' for " + mailtype);
                    confirmemail = conn.updateDocumentsAsSent(company, maillist);
                    
                    es.sendConfirmation(confirmemail);
                    
                }
            }
            LogSeyma.println("End of program");
        } catch (Exception ex) {
            Logger.getLogger(MailDocuments.class.getName()).log(Level.SEVERE, null, ex);
            LogSeyma.printexcp(ex.getMessage());
        }
    }
}
