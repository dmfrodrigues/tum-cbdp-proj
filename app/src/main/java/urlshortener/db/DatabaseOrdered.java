package urlshortener.db;

public abstract class DatabaseOrdered<T> extends PersistentMapBinary<String> implements PersistentLog, PersistentStateMachineOrdered<T> {
    abstract public boolean seed();
    abstract public boolean seed(boolean force);
    abstract public boolean init();
}
