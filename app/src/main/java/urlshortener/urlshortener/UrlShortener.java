package urlshortener.urlshortener;

import java.io.IOException;
import java.rmi.NotBoundException;

import urlshortener.LogEntryContentPut;
import urlshortener.raft.Raft;

public abstract class UrlShortener {
    abstract public String shortenURL(String url);

    Database db;
    Raft raft;

    public UrlShortener(Database db, Raft raft){
        this.db = db;
        this.raft = raft;
    }

    public String shorten(String url){
        String id = shortenURL(url);

        try {
            if(!raft.appendEntryRPC(new LogEntryContentPut(id, url)))
                return null;
        } catch (InterruptedException | NotBoundException | IOException e) {
            e.printStackTrace();
            return null;
        }

        return id;
    }

    public String enlongate(String id) {
        return db.get(id);
    }
}
