package urlshortener.urlshortener;

import urlshortener.raft.PersistentMap;

public abstract class Database extends PersistentMap {
    abstract public boolean seed();

    abstract public boolean put(String key, String value);

    abstract public String get(String key);
}
