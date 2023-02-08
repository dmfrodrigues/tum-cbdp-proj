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
        return new String(Base64.getEncoder().encode(hashString.getBytes()));
    }

    public String shortenURL(String url){
        return staticShortenURL(url);
    }

    @Override
    public String enlongate(String id) {
        String key = new String(Base64.getDecoder().decode(id.getBytes()));
        return db.getKeyValue(key);
    }

    @Override
    public void commit(String id, String url) {
        String key = new String(Base64.getDecoder().decode(id.getBytes()));
        db.putKeyValue(key, url);
    }
}
