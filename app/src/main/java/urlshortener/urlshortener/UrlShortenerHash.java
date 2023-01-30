package urlshortener.urlshortener;

import java.util.Base64;

import urlshortener.raft.Raft;

public class UrlShortenerHash extends UrlShortener {
    public UrlShortenerHash(Database db, Raft raft){
        super(db, raft);
    }

    public String shortenURL(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        return new String(Base64.getEncoder().encode(hashString.getBytes()));
    }
}
