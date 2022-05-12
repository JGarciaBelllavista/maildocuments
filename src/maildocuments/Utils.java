package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.LogSeyma;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utils
 *
 * @author Andriy Byelikov
 */
public class Utils {

    public static String getCardFNameFromDelivery(int docNum, boolean isSeyma) {
        String result = "";

        String sql_dbname = Config.param(ConfigStr.SQL_DBNAMES).split("&")[isSeyma ? 0 : 1];
        try {
            Connection conn = DriverManager.getConnection(Config.param(Config.SQL_HOST)
                    + sql_dbname
                    + Config.param(Config.SQL_USER)
                    + Config.param(Config.SQL_KEY));
            String query = Config.param("FACTURAS_SQL_CARDFNAME_ALBARAN")
                    .replace("&DOCNUM&", Integer.toString(docNum));
            LogSeyma.println(query);
            try ( Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    result = rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

}
