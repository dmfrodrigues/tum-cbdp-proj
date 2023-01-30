package urlshortener.urlshortener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePostgres extends Database {
    private Connection conn;
    private PreparedStatement putStmt;
    private PreparedStatement getStmt;

    private Connection connect(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public DatabasePostgres(String url, String user, String password) throws SQLException {
        conn = connect(url, user, password);
        putStmt = conn.prepareStatement("""
            INSERT INTO map(key, value)
            VALUES (?, ?)
            ON CONFLICT (key) DO
                UPDATE SET value=EXCLUDED.value
        """);
        getStmt = conn.prepareStatement("""
            SELECT value FROM map
            WHERE key=?
        """);
    }

    public boolean seed() {
        try {
            String seedString = """
                DROP SCHEMA IF EXISTS urlshortener CASCADE;

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
            putStmt.setString(1, key);
            putStmt.setString(2, value);
            int n = putStmt.executeUpdate();
            // System.out.println("DB: applied " + key + " => " + value);
            return (n == 1);
        } catch(SQLException e){
            e.printStackTrace();
            System.out.println("DB: failed to apply " + key + " => " + value);
            return false;
        }
    }

    public String get(String key) {
        try {
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            if(!rs.next()) return null;
            String url = rs.getString("value");
            return url;
        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }
}
