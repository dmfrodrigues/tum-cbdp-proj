package urlshortener.urlshortener;

import urlshortener.raft.PersistentLog;
import urlshortener.raft.PersistentMap;

public abstract class DatabaseOrdered<T> extends PersistentMap implements PersistentLog, PersistentStateMachineOrdered<T> {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);
    abstract public boolean init();
}
