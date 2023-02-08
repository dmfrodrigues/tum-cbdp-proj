package urlshortener.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import urlshortener.raft.LogEntry;
import urlshortener.raft.LogEntryContent;

public class DatabasePostgresLong extends DatabaseOrdered<Long> {
    private Connection conn;

    private PreparedStatement putStmt;
    private PreparedStatement getStmt;

    private PreparedStatement putKeyValueStmt;
    private PreparedStatement getKeyValueStmt;
    private Long highestKey;
    
    private PreparedStatement logLoadStmt;
    private PreparedStatement logDeleteAfterStmt;
    private PreparedStatement logAddStmt;

    private Connection connect(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public DatabasePostgresLong(String url, String user, String password) throws SQLException {
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

        putKeyValueStmt = conn.prepareStatement("""
            INSERT INTO kv(key, value)
            VALUES (?, ?)
            ON CONFLICT (key) DO
                UPDATE SET value=EXCLUDED.value
        """);
        getKeyValueStmt = conn.prepareStatement("""
            SELECT value FROM kv
            WHERE key=?
        """);


        logLoadStmt = conn.prepareStatement("""
            SELECT id, term, content FROM log
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
                    value BYTEA
                );

                CREATE TABLE kv(
                    key BIGINT PRIMARY KEY,
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

    public boolean init(){
        try {
            // Selecting schema urlshortener
            Statement st = conn.createStatement();
            st.execute("SET search_path TO urlshortener");

            // Getting highest key
            ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(key),-1) AS maxKey FROM kv");
            if(!rs.next()) return false;
            highestKey = rs.getLong("maxKey");
            st.close();

            // Load log
            loadLog();

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean put(String key, InputStream value) {
        try {
            putStmt.setString(1, key);
            putStmt.setBinaryStream(2, value);
            int n = putStmt.executeUpdate();
            // System.out.println("DB: applied " + key + " => " + value);
            return (n == 1);
        } catch(SQLException e){
            e.printStackTrace();
            System.out.println("DB: failed to apply " + key + " => " + value);
            return false;
        }
    }

    public InputStream get(String key) {
        try {
            getStmt.setString(1, key);
            ResultSet rs = getStmt.executeQuery();
            if(!rs.next()) return null;
            InputStream value = rs.getBinaryStream("value");
            return value;
        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean putKeyValue(Long key, String value) {
        try {
            putKeyValueStmt.setLong(1, key.intValue());
            putKeyValueStmt.setString(2, value);
            int n = putKeyValueStmt.executeUpdate();
            if(n == 1){
                // System.out.println("DB: applied " + key + " => " + value);
                highestKey = Math.max(key, highestKey);
                return true;
            }
            return false;
        } catch(SQLException e){
            e.printStackTrace();
            System.out.println("DB: failed to apply " + key + " => " + value);
            return false;
        }
    }

    @Override
    public String getKeyValue(Long key) {
        try {
            getKeyValueStmt.setLong(1, key);
            ResultSet rs = getKeyValueStmt.executeQuery();
            if(!rs.next()) return null;
            String url = rs.getString("value");
            return url;
        } catch(SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Long getHighestKey() {
        return highestKey;
    }

    ArrayList<LogEntry> log = new ArrayList<>();

    private boolean loadLog(){
        try {
            log.clear();

            ResultSet rs = logLoadStmt.executeQuery();
            while(rs.next()){
                int id = rs.getInt("id");
                int term = rs.getInt("term");

                InputStream is = rs.getBinaryStream("content");
                assert(is != null);
                ObjectInputStream ois = new ObjectInputStream(is);
                Object obj = ois.readObject();
                LogEntryContent content = (LogEntryContent)obj;
                ois.close();

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

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            oos.writeObject(logEntry.content);
            oos.close();
            logAddStmt.setBinaryStream(3, pis);

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

                PipedOutputStream pos = new PipedOutputStream();
                PipedInputStream pis = new PipedInputStream(pos);
                ObjectOutputStream oos = new ObjectOutputStream(pos);
                oos.writeObject(logEntry.content);
                oos.close();
                logAddStmt.setBinaryStream(3, pis);

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
