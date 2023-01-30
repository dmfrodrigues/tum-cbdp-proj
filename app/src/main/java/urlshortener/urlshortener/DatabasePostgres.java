package urlshortener.urlshortener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import urlshortener.raft.LogEntry;
import urlshortener.raft.LogEntryContent;

public class DatabasePostgres extends Database {
    private Connection conn;

    private PreparedStatement putStmt;
    private PreparedStatement getStmt;
    
    private PreparedStatement logLoadStmt;
    private PreparedStatement logDeleteAfterStmt;
    private PreparedStatement logAddStmt;

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

        logLoadStmt = conn.prepareStatement("""
            SELECT (id, term, content) FROM log
        """);
        logDeleteAfterStmt = conn.prepareStatement("""
            DELETE FROM log
            WHERE id>=?
        """);
        logAddStmt = conn.prepareStatement("""
            INSERT INTO log(id, term, content)
            VALUES (?, ?, ?)
            ON CONFLICT (id) DO
                UPDATE SET  term=EXCLUDED.term,
                            content=EXCLUDED.content
        """);
    }

    public Boolean isSeeded() {
        try {
            String checkShouldSeedString = """
                SELECT schema_name FROM information_schema.schemata
                WHERE schema_name = 'urlshortener'
            """;
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(checkShouldSeedString);
            if(rs.next()) return true;
            else return false;
        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean seed(boolean force) {
        if(force) return seed();

        Boolean isSeededBoolean = isSeeded();
        if(isSeededBoolean == null) return false;

        if(isSeededBoolean){
            System.out.println("DB already seeded; did nothing");
            return true;
        } else {
            System.out.println("DB not yet seeded; seeding");
            return seed();
        }
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

                CREATE TABLE log(
                    id INT PRIMARY KEY,
                    term INT,
                    content VARCHAR
                )
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

    ArrayList<LogEntry> log = new ArrayList<>();

    public boolean loadLog(){
        try {
            log.clear();

            ResultSet rs = logLoadStmt.executeQuery();
            while(rs.next()){
                int id = rs.getInt("id");
                int term = rs.getInt("term");

                String s = rs.getString("content");
                byte[] data = Base64.getDecoder().decode(s);
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = ois.readObject();
                LogEntryContent content = (LogEntryContent)obj;

                if(id != log.size()) return false;

                log.add(new LogEntry(term, content));
            }

            return true;
        } catch(SQLException | ClassNotFoundException | IOException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int size() {
        return log.size();
    }

    @Override
    public LogEntry get(int index) {
        return log.get(index);
    }

    @Override
    public boolean deleteAfter(int index) {
        try {
            logDeleteAfterStmt.setInt(1, index);
            logDeleteAfterStmt.execute();
        
            log.subList(index, log.size()).clear();
        } catch(SQLException e){
            e.printStackTrace();
            System.out.println("DB: failed to delete log after " + index);
            return false;
        }
        return true;
    }

    @Override
    public boolean add(LogEntry logEntry) {
        try {
            logAddStmt.setInt(1, log.size());
            logAddStmt.setInt(2, logEntry.term);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(logEntry.content);
            String s = Base64.getEncoder().encodeToString(baos.toByteArray());
            logAddStmt.setString(3, s);

            logAddStmt.executeUpdate();

            log.add(logEntry);

            return true;
        } catch(SQLException | IOException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addAll(List<LogEntry> list) {
        int N = log.size();
        try {
            for(int i = 0; i < list.size(); ++i){
                int j = N+i;
                LogEntry logEntry = list.get(i);

                logAddStmt.setInt(1, j);
                logAddStmt.setInt(2, logEntry.term);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(logEntry.content);
                String s = Base64.getEncoder().encodeToString(baos.toByteArray());
                logAddStmt.setString(3, s);

                logAddStmt.addBatch();
            }
            logAddStmt.executeBatch();

            log.addAll(list);

            return true;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            System.out.println("DB: failed to add list of entries to log");
            return false;
        }
    }

    @Override
    public List<LogEntry> getAfter(int index) {
        return log.subList(index, log.size());
    }
}
