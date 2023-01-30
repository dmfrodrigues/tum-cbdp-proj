package urlshortener.urlshortener;

import java.rmi.NotBoundException;
import java.util.Base64;

import urlshortener.LogEntryContentPut;
import urlshortener.raft.Raft;

public class UrlShortenerHash implements UrlShortener {
    Database db;
    Raft raft;

    public UrlShortenerHash(Database db, Raft raft){
        this.db = db;
        this.raft = raft;
    }

    public String shorten(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        String id = new String(Base64.getEncoder().encode(hashString.getBytes()));

        try {
            if(!raft.appendEntry(new LogEntryContentPut(id, url)))
                return null;
        } catch (InterruptedException | NotBoundException e) {
            e.printStackTrace();
            return null;
        }

        // try {
        //     db.put(id, url);
        // } catch (SQLException e) {
        //     e.printStackTrace();
        // }

        return id;
    }

    @Override
    public String enlongate(String id) {
        return db.get(id);
    }
}
