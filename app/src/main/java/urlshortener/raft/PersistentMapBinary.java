package urlshortener.raft;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

public abstract class PersistentMapBinary<Key> extends PersistentMap<Key, InputStream>{
    public static class Stored<Key, T extends Serializable> implements PersistentMap.Stored<T> {
        PersistentMapBinary<Key> map;
        Key varName;
        T t;
        private Stored(PersistentMapBinary<Key> map, Key varName, T defaultValue) throws IOException {
            this.map = map;
            this.varName = varName;
            
            InputStream is = map.get(varName);
            if(is == null){
                set(defaultValue);
                return;
            }

            ObjectInputStream ois = new ObjectInputStream(is);
            try {
                Object obj = ois.readObject();
                t = (T)obj;
            } catch (ClassNotFoundException e) {
                throw new ClassCastException(e.getMessage());
            }
        }
        public void set(T t) throws IOException {
            this.t = t;
            
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            oos.writeObject(this.t);
            oos.close();
            if(!map.put(varName, pis)){
                throw new IOException("Could not put variable " + varName);
            }
        }
        public T get(){
            return t;
        }
    }

    @Override
    public <T extends Serializable> PersistentMap.Stored<T> loadStoredVariable(Key varName, T defaultValue) throws IOException {
        return new Stored<Key, T>(this, varName, defaultValue);
    }
}
