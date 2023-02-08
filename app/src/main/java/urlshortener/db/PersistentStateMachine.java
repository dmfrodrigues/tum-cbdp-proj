package urlshortener.db;

public interface PersistentStateMachine<T> {
    boolean putKeyValue(T key, String value);
    String getKeyValue(T key);
}
