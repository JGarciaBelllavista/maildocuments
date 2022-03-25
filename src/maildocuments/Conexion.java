/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.LogSeyma;
import com.itextpdf.text.DocumentException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lorenzo
 */
public class Conexion {

    private String mailtype;
    private String defaultFilename = "";
    private Map<String, String> langMap = new HashMap<String, String>();
    private Connection conn;
    private int rows_num = -1;

    public int getRows_num() {
        return rows_num;
    }

    public Conexion(String i_mailtype) {
        mailtype = i_mailtype;
        
        defaultFilename = Config.param(mailtype, ConfigStr.DOC_DF);
        langMap.put("ES", Config.param(mailtype, ConfigStr.DOC_ES));
        langMap.put("ESDF", Config.param(mailtype, ConfigStr.DOC_ESDF));
        langMap.put("AD", Config.param(mailtype, ConfigStr.DOC_AD));
        langMap.put("IT", Config.param(mailtype, ConfigStr.DOC_IT));
        langMap.put("DE", Config.param(mailtype, ConfigStr.DOC_DE));
        langMap.put("FR", Config.param(mailtype, ConfigStr.DOC_FR));
        langMap.put("PT", Config.param(mailtype, ConfigStr.DOC_PT));
        langMap.put("NE", Config.param(mailtype, ConfigStr.DOC_NE));
    }
    
    public ArrayList<Email> readPandingDocuments(String doccompany) throws DocumentException, IOException {
        rows_num = -1;
        ArrayList<Email> maillist  = new ArrayList<>();
        String[] companies = Config.param(mailtype, ConfigStr.DOC_COMPANIES).split(Config.param(Config.FILE_SPR));
        int companynum = Arrays.asList(companies).indexOf(doccompany);
        String sql_dbname = Config.param(ConfigStr.SQL_DBNAMES).split("&")[companynum];
        String docfilename = Config.param(mailtype, ConfigStr.DOC_FILENAMES).split(Config.param(Config.FILE_SPR))[companynum];
        int usadocnum = 0;
        String usacomments = "";        
        String marca = Config.param(ConfigStr.MARCAS).split(Config.param(Config.FILE_SPR),-1)[companynum];
        //fake query to create one result
        String sql_query0 = Config.param(mailtype, ConfigStr.SQL_QUERY0);                   
        String sql_query = Config.param(mailtype, ConfigStr.SQL_QUERY).split("\\|")[companynum];
        String sql_query2 = Config.param(mailtype, ConfigStr.SQL_QUERY2).split("\\|")[companynum];
        String sql_query3 = Config.param(mailtype, ConfigStr.SQL_QUERY3).split("\\|")[companynum];
        String sql_query4 = Config.param(mailtype, ConfigStr.SQL_QUERY4).split("\\|")[companynum];        
        boolean isSeyma = true;
        if (companynum == 1) {
            isSeyma = false;
        }
        String company;
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            company = Config.param(ConfigStr.USA_COMAPNY);
        } else {
            company = Config.param(ConfigStr.COMPANIES).split(Config.param(Config.FILE_SPR))[companynum];
        }
        
        DocWriter docwriter;
        try {
            //Conectamos a la base de datos a través del servidor local
            conn = DriverManager.getConnection(Config.param(Config.SQL_HOST) + sql_dbname
                    + Config.param(Config.SQL_USER) + Config.param(Config.SQL_KEY));
            Statement stmt = null;
            Statement stmtvenciment;
            Statement stmtarticles;
            Statement stmtintrastat;
            ResultSet rs = null;
            ResultSet rsvenciment;
            ResultSet rsarticles;
            ResultSet rsintrastat;
            Statement stmtfactures;
            ResultSet rsusafactures;
            try {
                if (mailtype.equals(ConfigStr.FACTURASUSA))
                {
                    usadocnum = ultimoNumeroNumUSA();
                } 
                stmt = conn.createStatement();
                stmtvenciment = conn.createStatement();
                stmtarticles = conn.createStatement();
                stmtintrastat = conn.createStatement();
                stmtfactures = conn.createStatement();
                
                LogSeyma.printdebug("Executing sql query:\n" + sql_query0);
                rsusafactures = stmtfactures.executeQuery(sql_query0);
                String query;
                while (rsusafactures.next()) {
                    query = sql_query;
                    if (mailtype.equals(ConfigStr.FACTURASUSA) && rsusafactures.getString(ConfigStr.BASEREF) != null) {
                        query = sql_query.replace(Config.param(ConfigStr.STR_RPL), rsusafactures.getString(ConfigStr.BASEREF));
                        usacomments = DocWriter.getStringVal(rsusafactures.getString(ConfigStr.COMMENTS));
                        rows_num = rsusafactures.getInt(ConfigStr.ROW_NUM);
                    }
                    LogSeyma.printdebug("Executing sql query:\n" + query);
                    rs = stmt.executeQuery(query);

                    while (rs.next()) {
                        try {
                            if (rows_num == -1)
                                rows_num = rs.getInt(ConfigStr.ROW_NUM);

                            String idioma = DocWriter.getStringVal(rs.getString(ConfigStr.COUNTRY));
                            if (mailtype.equals(ConfigStr.VENCIMIENTOS) && !idioma.equals("ES") 
                                    && !DocWriter.getStringVal(rs.getString(ConfigStr.PEY_METHOD)).equals("TRANSFERENCIA")) 
                                    continue; //not sending email
                            String docentry = "";
                            String doctype;
                            String clienteCode = "";
                            String subject0 = "";
                            if (!mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                                docentry = DocWriter.getStringVal(rs.getString(ConfigStr.DOC_ENTRY));
                                clienteCode = DocWriter.getStringVal(rs.getString(ConfigStr.CARD_CODE));
                                subject0 = DocWriter.getStringVal(rs.getString(ConfigStr.COMPNY_NAME)).substring(0, 2);
                            }
                            if (mailtype.equals(ConfigStr.ABONOS))
                                doctype = DocWriter.getStringVal(rs.getString(ConfigStr.DOC_TYPE));
                            else
                                doctype = "I";
                            String docnum = DocWriter.getStringVal(rs.getString(ConfigStr.DOC_NUM));
                            String emaildocnum;
                            String usadocdate = "";
                            if (mailtype.equals(ConfigStr.FACTURASUSA)) {
                                emaildocnum = String.valueOf(usadocnum);
                                usadocdate = DocWriter.getStringVal(rsusafactures.getString(ConfigStr.DOC_DATE));
                            } else {
                                emaildocnum = docnum;
                            }
                            LogSeyma.println("docnum: " + docnum + ", emaildocnum: " + emaildocnum);
                            if (!mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                                docwriter = new DocWriter(rs, mailtype, isSeyma,usadocnum,usacomments,usadocdate);
                                query = sql_query2.replace(Config.param(ConfigStr.STR_RPL), docentry);
                                LogSeyma.printdebug("Executing sql query:\n" + query);
                                rsvenciment = stmtvenciment.executeQuery(query);
                                while (rsvenciment.next()) {
                                    docwriter.addDueDate(rsvenciment.getString(ConfigStr.DUE_DATE), rsvenciment.getFloat(ConfigStr.LINE_TOTAL));
                                }

                                query = sql_query3.replace(Config.param(ConfigStr.STR_RPL), docentry);
                                
                                // LÓGICA DEPRECADA, vamos a ordenar siempre por referencia:
                                
                                //Si tiene Pack de Navidad o Medi ordenamos por linea
                                //Para que el nodo Padre quede por encima del hijo
                                
                                /*if (!isSeyma || hayPackNavidad(docentry, stmt)) {
                                    query += Config.param(mailtype, ConfigStr.SQL_QUERY3_ORDER1);
                                } else {
                                    query += Config.param(mailtype, ConfigStr.SQL_QUERY3_ORDER2);
                                }*/

                                query += Config.param(mailtype, ConfigStr.SQL_QUERY3_ORDER2);

                                LogSeyma.printdebug("Executing sql query:\n" + query);
                                rsarticles = stmtarticles.executeQuery(query);
                                while (rsarticles.next()) {
                                    String code;
                                    if (doctype.equals("I")) {
                                        code = rsarticles.getString(ConfigStr.ITEM_CODE);
                                    } else {
                                        code = rsarticles.getString(ConfigStr.ACCT_CODE);
                                    }
                                    docwriter.addItem(code, rsarticles);
                                }
                                if (mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURAS)) {
                                    //
                                    query = sql_query4.replace(Config.param(ConfigStr.STR_RPL), docentry);
                                    LogSeyma.printdebug("Executing sql query:\n" + query);
                                    rsintrastat = stmtintrastat.executeQuery(query);
                                    while (rsintrastat.next()) {
                                        docwriter.addInterState(rsintrastat);
                                    }
                                }
                                docwriter.generateDocument(emaildocnum);
                            }

                            String temporada = "";
                            if (!mailtype.equals(ConfigStr.VENCIMIENTOS) && rs.getString(ConfigStr.TEMP_NAME) != null && rs.getString(ConfigStr.TEMP_NAME).equals("")) {
                                temporada = rs.getString(ConfigStr.TEMP_NAME);
                            }

                            String mailto = DocWriter.getStringVal(rs.getString(ConfigStr.E_MAIL));
                            String mailrepre = DocWriter.getStringVal(rs.getString(ConfigStr.U_SEIREPRE));
                            if (mailrepre == null || mailrepre.equals(""))
                                mailrepre = "-";
                            String mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER);
                            String email_lang = idioma;
                            if (mailtype.equals(ConfigStr.VENCIMIENTOS) && idioma.equals("ES") && !DocWriter.getStringVal(rs.getString(ConfigStr.PEY_METHOD)).equals("GIRO DOMICIL."))
                                email_lang = "ESDF";
                            String emailBody[];
                            if (mailtype.equals(ConfigStr.FACTURAS) && clienteCode.startsWith("CW")) {
                                String filename = langMap.get(idioma);
                                if (filename == null)
                                    filename = defaultFilename;
                                filename = filename.replace("_","_web_");
                                emailBody = getEmailBody(filename,true);
                                String pedidoNum = DocWriter.getStringVal(rs.getString(ConfigStr.AGRUPAR_NUM));
                                String emailCompny = isSeyma ? "Abbacino" : "HeyDudeShoes";
                                emailBody[0] = emailBody[0].replace(Config.param(ConfigStr.STR_RPL),pedidoNum).replace(Config.param(ConfigStr.STR_RPL2),emailCompny);
                            } else {
                                emailBody = getEmailBody(email_lang);
                            }
                            
                            if (mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                                if (idioma.equals("ES"))
                                    mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER2);
                            } else if (mailtype.equals(ConfigStr.FACTURAS) && clienteCode.startsWith("CW")) {
                                mailrepre = "-";
                                mailmanager = "-";
                            } else {
                                if (isSeyma) {
                                    if (mailtype.equals(ConfigStr.FACTURASUSA)) {
                                        mailto = Config.param(mailtype, ConfigStr.DOC_MAIL_TO);
                                        mailrepre = mailmanager;
                                        mailmanager = Config.param(mailtype, ConfigStr.DOC_MAIL_REPRE);                                
                                    }

                                    if (idioma.equals("IT") && clienteCode.contains("C04690")) { //COIN. Només s'envia a laurentia i Ana Maria(COIN).
                                        mailto = Config.param(mailtype, ConfigStr.MAILTO_COIN);
                                        mailrepre = Config.param(mailtype, ConfigStr.MAILREPRE_COIN);
                                        mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER_COIN);
                                    } else if (idioma.equals("AD") || idioma.equals("PT")
                                            || (idioma.equals("ES")) && !clienteCode.contains("C06020") && !clienteCode.contains("C06011")) {
                                        mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER2);
                                    }
                                    if (mailtype.equals(ConfigStr.PEDIDOS) && !clienteCode.startsWith("CW") && !idioma.equals("ES") 
                                            && !mailmanager.equals(Config.param(mailtype, ConfigStr.MAIL_MANAGER))) {
                                        if (mailmanager.equals("-"))
                                            mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER);
                                        else
                                            mailmanager += ";" + Config.param(mailtype, ConfigStr.MAIL_MANAGER);
                                    }
                                } else {
                                    mailmanager = "-";
                                    if (idioma.equals("ES") || idioma.equals("AD") || idioma.equals("PT")) {
                                        mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER2);
                                    } else if (mailtype.equals(ConfigStr.PEDIDOS) && (DocWriter.getStringVal(rs.getString(ConfigStr.SLP_CODE)).equals("23") || 
                                               DocWriter.getStringVal(rs.getString(ConfigStr.SLP_CODE)).equals("24") || DocWriter.getStringVal(rs.getString(ConfigStr.SLP_CODE)).equals("22"))) {
                                        mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER);
                                    } else if (mailtype.equals(ConfigStr.FACTURAS)) {
                                        mailmanager = Config.param(mailtype, ConfigStr.MAIL_MANAGER3);
                                    }
                                }
                            }
                            
                            String subject1 = emaildocnum;
                            String DEcardname = "";
                            String b2bText = "";
                            if (mailtype.equals(ConfigStr.PEDIDOS)) {
                                subject0 += " " + temporada;
                                subject1 += " (" + doctype + ")";
                                if (isSeyma && idioma.equals("DE"))
                                    DEcardname = "SE";
                                if (clienteCode.startsWith("C0") && rs.getString("U_SEI_DNPO") != null 
                                        && rs.getString("U_SEI_DNPO").startsWith("100")) {
                                    String configStrName = "B2B_TEXT_DF";
                                    if (langMap.containsKey(idioma))
                                        configStrName = ConfigStr.B2B_TEXT + "_" +  idioma;
                                    LogSeyma.println("B2B_TEXT, configStrName: " + configStrName);
                                    b2bText = Config.param(configStrName);
                                }
                                emailBody[1] = emailBody[1].replace(Config.param(ConfigStr.B2B_RPL), b2bText);                                                        
                            } else if (mailtype.equals(ConfigStr.FACTURAS)) {
                                String trackingText =  "<br>";
                                if (isSeyma && clienteCode.contains("CW") && idioma.equals("ES")) {
                                    trackingText = getEmailBody(Config.param(mailtype, ConfigStr.DOC_TRACKING), false)[1]
                                                    .replace(Config.param(ConfigStr.STR_RPL),DocWriter.getStringVal(rs.getString(ConfigStr.DOC_NUM)))
                                                    .replace(Config.param(ConfigStr.STR_RPL2),DocWriter.getStringVal(rs.getString(ConfigStr.ZIPCODE))); 
                                }
                                if (clienteCode.startsWith("C0") && rs.getString("U_SEI_DNPO") != null 
                                        && rs.getString("U_SEI_DNPO").startsWith("100")) {
                                    String configStrName = "B2B_TEXT_INV_DF";
                                    if (langMap.containsKey(idioma))
                                        configStrName = ConfigStr.B2B_TEXT_INV + "_" +  idioma;
                                    LogSeyma.println("B2B_TEXT_INV, configStrName: " + configStrName);
                                    b2bText = Config.param(configStrName);
                                }
                                emailBody[1] = emailBody[1].replace(Config.param(ConfigStr.STR_RPL), trackingText)
                                                           .replace(Config.param(ConfigStr.B2B_RPL), b2bText);                        
                            } else if (mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                                emailBody[1] = emailBody[1].replace(ConfigStr.FACTURA_RPL, docnum)
                                                           .replace(ConfigStr.PRICE_RPL, DocWriter.roundNum(rs.getFloat(ConfigStr.PRICE_O),mailtype) + " " + DocWriter.getStringVal(rs.getString(ConfigStr.DOC_CUR)))
                                                           .replace(ConfigStr.FACTURA_DATE_RPL, DocWriter.getStringVal(rs.getString(ConfigStr.DOC_DATE)))
                                                           .replace(ConfigStr.DUE_DATE_RPL, DocWriter.getStringVal(rs.getString(ConfigStr.DUE_DATE)));  
                            }
                            if (mailtype.equals(ConfigStr.FACTURAS) && clienteCode.startsWith("CW")) {
                                subject0 = "";
                                subject1 = "";
                            }
                            String filepath2send = "";
                            String file2send = "";
                            if (!mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                                filepath2send = Config.param(mailtype, ConfigStr.DOC_FOLDER) + docfilename + emaildocnum + Config.param(ConfigStr.DOC_FILEEXT);
                                file2send = docfilename + emaildocnum + Config.param(ConfigStr.DOC_FILEEXT);
                            }
                            Email email2add = new Email(mailto.replace(" ",""), mailrepre.replace(" ",""), mailmanager.replace(" ",""), DEcardname + DocWriter.getStringVal(rs.getString(ConfigStr.CARD_NAME)),
                                    subject0 + emailBody[0] + subject1, emailBody[1],filepath2send,file2send, docnum,
                                    company, marca, clienteCode, mailtype);
                            if (mailtype.equals(ConfigStr.FACTURASUSA))
                                email2add.setNumUSA(String.valueOf(usadocnum++));                                
                            maillist.add(email2add);
                        } catch (SQLException ex) {
                            LogSeyma.printexcp("DOC_NUM: " + DocWriter.getStringVal(rs.getString(ConfigStr.DOC_NUM)) +
                                               "SQLException line 313: " + Arrays.toString(ex.getStackTrace()) +
                                               "\nSQLState: " + ex.getSQLState() +
                                               "\nVendorError: " + ex.getErrorCode());
                        } catch (ParseException ex) {
                            LogSeyma.printexcp("ParseException: " + Arrays.toString(ex.getStackTrace()));
                            Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (Exception ex) {
                            LogSeyma.printexcp("DOC_NUM: " + DocWriter.getStringVal(rs.getString(ConfigStr.DOC_NUM)) +
                                               " Company: " + company +
                                               " General Exception line 320: " + Arrays.toString(ex.getStackTrace()));
                        }
                    }
                }
            } catch (SQLException ex) {
                LogSeyma.printexcp("SQLException line 327: " + Arrays.toString(ex.getStackTrace()) +
                                   "\nSQLState: " + ex.getSQLState() +
                                   "\nVendorError: " + ex.getErrorCode());
            } catch (Exception ex) {
                LogSeyma.printexcp("General Exception: " + Arrays.toString(ex.getStackTrace()));
                Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (SQLException sqlEx) {
                    } // ignore
                    rs = null;
                }

                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (SQLException sqlEx) {
                    } // ignore
                    stmt = null;
                }
            }

        } catch (SQLException ex) {
            LogSeyma.printexcp("SQLException line 336: " + Arrays.toString(ex.getStackTrace()) +
                               "\nSQLState: " + ex.getSQLState() +
                               "\nVendorError: " + ex.getErrorCode());
        } catch (Exception ex) {
            LogSeyma.printexcp("General Exception line 340: " + Arrays.toString(ex.getStackTrace()));
        }
        return maillist;
    }

    private String[] getEmailBody(String lang) throws UnsupportedEncodingException {
        String filename = langMap.get(lang);
        if (filename == null)
            filename = defaultFilename;
        return getEmailBody(filename,true);
    }
    
    private String[] getEmailBody(String filename, boolean subject) throws UnsupportedEncodingException {
        BufferedReader reader;
        String line;
        int lineNum = 1;
        String bodyText = "";
        String subjectText = "";
        try {
            //reader = new BufferedReader(filename);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
            try {
                while ((line = reader.readLine()) != null) {
                    if (subject && lineNum == 1) {
                        subjectText = line;
                    } else {
                        bodyText += line;
                    }
                    lineNum += 1;
                }
            } catch (IOException ex) {
                Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        String result[] = {subjectText, bodyText};
        return result;
    }

    private boolean hayPackNavidad(String docentry, Statement stmt) throws SQLException {
        ResultSet rs;
        stmt = conn.createStatement();
        String query = Config.param(mailtype, ConfigStr.SQL_QUERY5).replace(Config.param(ConfigStr.STR_RPL), docentry);
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            if (rs.getInt(1) > 0) {
                return true;
            }
        }
        return false;
    }

    public Email updateDocumentsAsSent(String doccompany, ArrayList<Email> maillist) {
        boolean debug = Config.param(ConfigStr.DEBUG).equals("true");
        String[] companies = Config.param(mailtype, ConfigStr.DOC_COMPANIES).split(Config.param(Config.FILE_SPR));
        int companynum = Arrays.asList(companies).indexOf(doccompany);
        String sql_dbname = Config.param(ConfigStr.SQL_DBNAMES).split("&")[companynum];
        Statement stmtupdate = null;
        //int rsupdate;
        Email mailstatus = null;
        ArrayList<String> docs = new ArrayList<>();
        int ip = 0;
        String query;
        try {
            if (!mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                conn = DriverManager.getConnection(Config.param(Config.SQL_HOST) + sql_dbname
                        + Config.param(Config.SQL_USER) + Config.param(Config.SQL_KEY));

                stmtupdate = conn.createStatement();
            }
            for (Email email2update : maillist) {
                if (email2update.isEnviado()) {
                    if (!mailtype.equals(ConfigStr.VENCIMIENTOS)) {
                        query = Config.param(mailtype, ConfigStr.SQL_UPDATE).replace(Config.param(ConfigStr.STR_RPL), email2update.getDocumento());
                        if (mailtype.equals(ConfigStr.FACTURASUSA))
                            query = query.replace(Config.param(ConfigStr.STR_RPL2), email2update.getNumUSA());
                        LogSeyma.printdebug("Update query: " + query);
                        if (!debug)
                            stmtupdate.executeUpdate(query);
                    }
                } else {
                    docs.add(email2update.getDocumento());
                    ip++;
                }
            }
            mailstatus = new Email(docs, maillist.size(), ip, mailtype, doccompany);
        } catch (SQLException ex) {
            // handle any errors
            LogSeyma.printexcp("SQLException: " + ex.getMessage() +
                               "\nSQLState: " + ex.getSQLState() +
                               "\nVendorError: " + ex.getErrorCode());
        } finally {
            if (stmtupdate != null) {
                try {
                    stmtupdate.close();
                } catch (SQLException sqlEx) {
                } // ignore
                stmtupdate = null;
            }
        }
        return mailstatus;
    }
    
    private int ultimoNumeroNumUSA() throws SQLException {
        ResultSet rs;

        int lastNumUSA = 0;

        Statement stmt = conn.createStatement();
        String query = Config.param(mailtype, ConfigStr.SQL_QUERY5);
        LogSeyma.printdebug("Max Num USA query: " + query);
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            lastNumUSA = rs.getInt(ConfigStr.MAX_NUM);
        }
        return lastNumUSA + 1;
    }
}
