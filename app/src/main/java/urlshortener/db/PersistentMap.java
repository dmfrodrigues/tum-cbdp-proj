package urlshortener.db;

import java.io.IOException;
import java.io.Serializable;

public abstract class PersistentMap<Key, Value> {

    abstract public boolean put(Key key, Value value);
    abstract public Value get(Key key);

    public static interface Stored<T>{
        public void set(T t) throws IOException;
        public T get();
    }

    public abstract <T extends Serializable> Stored<T> loadStoredVariable(Key varName, T defaultValue) throws IOException;
}
