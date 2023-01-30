package urlshortener.urlshortener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

public abstract class Database {
    abstract public boolean seed();

    abstract public boolean put(String key, String value);

    abstract public String get(String key);

    public class Stored<T extends Serializable> {
        Database db;
        String varName;
        T t;
        private Stored(Database db, String varName, T defaultValue) throws IOException {
            this.db = db;
            this.varName = varName;
            
            String s = db.get(varName);
            if(s == null){
                put(defaultValue);
                return;
            }

            byte[] data = Base64.getDecoder().decode(s);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            try {
                Object obj = ois.readObject();
                t = (T)obj;
            } catch (ClassNotFoundException e) {
                throw new ClassCastException(e.getMessage());
            }
        }
        public void put(T t) throws IOException {
            this.t = t;
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this.t);
            String s = Base64.getEncoder().encodeToString(baos.toByteArray());
            db.put(varName, s);
        }
        public T get(){
            return t;
        }
    }

    public <T extends Serializable> Stored<T> loadStoredVariable(String varName, T defaultValue) throws IOException {
        return new Stored<T>(this, varName, defaultValue);
    }
}
