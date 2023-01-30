package urlshortener.urlshortener;

import urlshortener.raft.PersistentLog;
import urlshortener.raft.PersistentMap;

public abstract class Database extends PersistentMap implements PersistentLog {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);

    abstract public boolean put(String key, String value);

    abstract public String get(String key);
}
