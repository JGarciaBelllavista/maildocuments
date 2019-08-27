/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amxa
 */
public class Config {

    public static final String FILE_SPR = "FILE_SPR";
    public static final String DATE_FORMAT = "DATE_FORMAT";
    public static final String LOG_FILE_PATH = "LOG_FILE_PATH";

    public static final String SQL_HOST = "SQL_HOST";
    public static final String SQL_DBNAMES = "SQL_DBNAMES";
    public static final String SQL_USER = "SQL_USER";
    public static final String SQL_KEY = "SQL_KEY";

    public static final String STMP_FROM = "STMP_FROM";
    public static final String STMP_HOST = "STMP_HOST";
    public static final String STMP_START = "STMP_START";
    public static final String STMP_USER = "STMP_USER";
    public static final String STMP_AUTH = "STMP_AUTH";
    public static final String STMP_PORT = "STMP_PORT";
    public static final String STMP_CONTIMEOUT = "STMP_CONTIMEOUT";
    public static final String STMP_TIMEOUT = "STMP_TIMEOUT";
    public static final String STMP_KEY = "STMP_KEY";
    public static final String TO_EMAILCONFITM_LIST = "TO_EMAILCONFITM_LIST";

    private static String defaultFilePath;
    private static File configFile;
    private static FileInputStream fis;
    private static Properties props;
    private static Config instance = null;

    private Config() throws Exception {
        initConfig();
    }

    public static void init(String defaultFilePath) throws Exception {
        if (instance == null) {
            Config.defaultFilePath = defaultFilePath;
            instance = new Config();
        } else {
            System.out.println("****** Config() ya instanciada.");
        }
    }

    private void initConfig() {
        configFile = new File(defaultFilePath);
        props = new Properties();
        try {
            fis = new FileInputStream(configFile);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            props.load(isr);
            System.out.println("\n********* READING CONFIG PARAMS: " + props.size());
        } catch (Exception ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("********* ERROR! ARCHIVO DE CONFIGURACIÓN: " + defaultFilePath);
            System.exit(0);
        }
    }

    public static String param(String param) {
        String prop = props.getProperty(param);
        //System.out.println("param: " + param + ", prop: " + prop);
        return prop;
    }

    public static String param(String prefix, String param) {
        return param(prefix + param);
    }

}
