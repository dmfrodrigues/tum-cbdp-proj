package urlshortener.urlshortener;

import java.util.Base64;

import urlshortener.db.PersistentStateMachine;
import urlshortener.raft.Raft;

public class UrlShortenerHash extends UrlShortener {
    PersistentStateMachine<String> db;
    
    public UrlShortenerHash(PersistentStateMachine<String> db, Raft raft){
        super(raft);
        this.db = db;
    }

    static public String staticShortenURL(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        String id = new String(Base64.getEncoder().encode(hashString.getBytes()));

        try {
            db.put(id, url);
        } catch (Exception e) {
            e.printStackTrace();
        }

    public String shortenURL(String url){
        return staticShortenURL(url);
    }

    @Override
    public String enlongate(String id) {
        try {
            return db.get(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
