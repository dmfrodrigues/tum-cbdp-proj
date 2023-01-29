package urlshortener.raft;

import java.io.Serializable;

public class LogEntry implements Serializable {
    public int term;
    public LogEntryContent content;

    public LogEntry(int term, LogEntryContent content){
        this.term = term;
        this.content = content;
    }

    public void apply(){
        if(content == null){
            System.out.println("FODASSE");
        }
        content.apply();
    };
}
