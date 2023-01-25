package urlshortener.urlshortener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    Connection conn;

    private Connection connect(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public Database(String url, String user, String password) throws SQLException {
        conn = connect(url, user, password);
    }

    public boolean seed() throws SQLException {
        String seedString = """
            DROP SCHEMA IF EXISTS urlshortener;

            CREATE SCHEMA urlshortener;
            SET search_path TO urlshortener;

            CREATE TABLE short2url(
                id VARCHAR PRIMARY KEY,
                url VARCHAR
            );
        """;
        Statement st = conn.createStatement();
        st.execute(seedString);
        st.close();

        System.out.println("Database seeded");

        return true;
    }

    public void put(String id, String url) throws SQLException{
        Statement st = conn.createStatement();
        st.execute("INSERT INTO short2url(id, url) VALUES ('" + id + "', '" + url + "')");
        st.close();
    }

    public String get(String id) throws SQLException {
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT url FROM short2url WHERE id = '" + id + "'");
        if(!rs.next()) return null;
        String url = rs.getString("url");
        st.close();
        return url;
    }
}
