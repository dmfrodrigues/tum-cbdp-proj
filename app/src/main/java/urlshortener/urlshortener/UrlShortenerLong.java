package urlshortener.urlshortener;

import urlshortener.raft.Raft;

public class UrlShortenerLong extends UrlShortener {
    PersistentStateMachineOrdered<Long> db;

    public UrlShortenerLong(PersistentStateMachineOrdered<Long> db, Raft raft){
        super(raft);
        this.db = db;
    }

    public String shortenURL(String url){
        return Long.valueOf(db.getHighestKey()+1).toString();
    }

    @Override
    public String enlongate(String id) {
        Long key = Long.valueOf(id);
        return db.getKeyValue(key);
    }

    @Override
    public void commit(String id, String url) {
        Long key = Long.valueOf(id);
        db.putKeyValue(key, url);
    }
}
