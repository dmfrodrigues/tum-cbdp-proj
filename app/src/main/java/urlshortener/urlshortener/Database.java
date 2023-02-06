package urlshortener.urlshortener;

import urlshortener.raft.PersistentLog;
import urlshortener.raft.PersistentMap;

public abstract class Database<T> extends PersistentMap implements PersistentLog, PersistentStateMachine<T> {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);
}
