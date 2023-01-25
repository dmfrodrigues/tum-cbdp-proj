package urlshortener;

import urlshortener.raft.LogEntry;

public class PutLogEntry extends LogEntry {
    private String key;
    private String value;

    public PutLogEntry(int term, String key, String value){
        super(term);
        this.key = key;
        this.value = value;
    }

    @Override
    public void apply() {
        System.out.println("Apply " + key + " => " + value);
        // TODO: implement
    }
    
}
