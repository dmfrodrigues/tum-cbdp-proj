package urlshortener.urlshortener;

import urlshortener.raft.Raft;

public class UrlShortenerLong extends UrlShortener<Long> {
    DatabaseOrdered<Long> db;

    Long highestKey;

    public UrlShortenerLong(DatabaseOrdered<Long> db, Raft raft){
        super(db, raft);
        this.db = db;

        highestKey = db.getHighestKey();
    }

    public Long shortenURL(String url){
        return highestKey++;
    }
}
