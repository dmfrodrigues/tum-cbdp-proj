package urlshortener.urlshortener;

import urlshortener.raft.PersistentLog;
import urlshortener.raft.PersistentMap;

public abstract class Database extends PersistentMap implements PersistentLog, PersistentStateMachine<String> {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);
}
