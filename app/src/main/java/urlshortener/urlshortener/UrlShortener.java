package urlshortener.urlshortener;

import java.io.IOException;
import java.rmi.NotBoundException;

import urlshortener.db.LogEntryContentPut;
import urlshortener.raft.Raft;

public abstract class UrlShortener {
    abstract public String shortenURL(String url);

    Raft raft;

    public UrlShortener(Raft raft){
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

    abstract public String enlongate(String id);

    abstract public void commit(String id, String url);
}
