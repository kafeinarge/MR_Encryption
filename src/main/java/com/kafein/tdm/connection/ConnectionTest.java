package com.kafein.tdm.connection;

import com.kafein.tdm.enumaration.DatabaseType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectionTest {

    public boolean isConnectedToDatabase (DatabaseType type, String url, String userName, String password) {
        try {
            switch (type) {
/*                case POSTGRES:
                    Class.forName("org.postgresql.Driver");
                    break;
                case ORACLE:
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                    break;
                case DB2:
                    Class.forName("COM.ibm.db2.jdbc.net.DB2Driver");
                    break;
                    */
                case MSSQL:
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    break;
                    /*
                case MYSQL:
                    Class.forName("com.mysql.jdbc.Driver");
                    break;
                case SYBASE:
                    Class.forName("com.sybase.jdbc.SybDriver");
                    break;
*/
            }

        } catch (ClassNotFoundException ex) {
            System.out.println("Problem during loading db-driver.");
            return false;
        }
        //Test if Database is reachable
        try {
            Connection conn = DriverManager.getConnection(url, userName, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            rs.close();
            conn.close();
        } catch (Throwable ex) {
            System.out.println("Could not connect to database: \n\n" + ex.getLocalizedMessage());
            return false;
        }

        return true;

    }
}
