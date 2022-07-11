package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.LogSeyma;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MailDocuments
 * @author isivan
 * @author Andriy Byelikov
 */
public class MailDocuments {

    public static void main(String[] args) {
        try {
            String envPath = args.length > 0 ? args[0] : System.getProperty("user.dir") + "/mail.config"; // mail_test_desarrollo.config
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
                LogSeyma.println("Ejecutado mailDocuments (mail-documents-1.3.0)");
                LogSeyma.println("Starting email sending for document type: " + mailtype);
                Conexion conn = new Conexion(mailtype);
                
                ArrayList<Email> maillist;
                Email confirmemail;
                String[] companies = Config.param(mailtype, ConfigStr.DOC_COMPANIES).split(Config.param(Config.FILE_SPR));
                // int i = 0; -- descomentar para probar Medi
                for (String company : companies) {
                    /*i++;
                    if (i == 1)
                        continue; -- descomentar para probar Medi*/
                    maillist = conn.readPendingDocuments(company);
                    LogSeyma.println("Total number of " + mailtype + " to be generated: " + conn.getRows_num());
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
