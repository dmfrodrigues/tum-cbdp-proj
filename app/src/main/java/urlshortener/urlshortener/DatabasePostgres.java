package urlshortener.urlshortener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePostgres implements Database {
    Connection conn;

    private Connection connect(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public DatabasePostgres(String url, String user, String password) throws SQLException {
        conn = connect(url, user, password);
    }

    public boolean seed() {
        try {
            String seedString = """
                DROP SCHEMA IF EXISTS urlshortener;

                CREATE SCHEMA urlshortener;
                SET search_path TO urlshortener;

                CREATE TABLE map(
                    key VARCHAR PRIMARY KEY,
                    value VARCHAR
                );
            """;
            Statement st = conn.createStatement();
            st.execute(seedString);
            st.close();

            System.out.println("Database seeded");

            return true;
        } catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean put(String key, String value) {
        try {
            Statement st = conn.createStatement();
            st.execute("INSERT INTO map(key, value) VALUES ('" + key + "', '" + value + "')");
            st.close();
            System.out.println("DB: applied " + key + " => " + value);
            return true;
        } catch(SQLException e){
            e.printStackTrace();
            System.out.println("DB: failed to apply " + key + " => " + value);
            return false;
        }
    }

    public String get(String key) {
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT value FROM map WHERE key='" + key + "'");
            if(!rs.next()) return null;
            String url = rs.getString("value");
            st.close();
            return url;
        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }
}
