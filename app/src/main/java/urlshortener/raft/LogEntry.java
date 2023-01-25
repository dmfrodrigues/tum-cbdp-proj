package urlshortener.raft;

public abstract class LogEntry {
    public int term;

    public LogEntry(int term){
        this.term = term;
    }

    public abstract void apply();
}
