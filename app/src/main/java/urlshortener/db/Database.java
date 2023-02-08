package urlshortener.db;

public abstract class Database<T> extends PersistentMapBinary<String> implements PersistentLog, PersistentStateMachine<T> {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);
}
