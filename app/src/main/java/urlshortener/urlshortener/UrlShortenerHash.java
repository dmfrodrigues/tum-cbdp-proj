package urlshortener.urlshortener;

import java.util.Base64;

import urlshortener.raft.Raft;

public class UrlShortenerHash extends UrlShortener<String> {
    public UrlShortenerHash(Database<String> db, Raft raft){
        super(db, raft);
    }

    static public String staticShortenURL(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        return new String(Base64.getEncoder().encode(hashString.getBytes()));
    }

    public String shortenURL(String url){
        return staticShortenURL(url);
    }
}
