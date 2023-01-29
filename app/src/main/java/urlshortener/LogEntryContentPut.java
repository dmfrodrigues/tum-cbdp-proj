package urlshortener;

import java.sql.SQLException;

import urlshortener.raft.LogEntryContent;

public class LogEntryContentPut implements LogEntryContent {
    private String key;
    private String value;

    public LogEntryContentPut(String key, String value){
        this.key = key;
        this.value = value;
    }

    @Override
    public void apply() {
        try {
            App.db.put(key, value);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
