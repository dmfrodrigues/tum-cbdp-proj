package urlshortener.urlshortener;

import urlshortener.App;
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
        App.node.urlShortener.commit(key, value);
    }
}
