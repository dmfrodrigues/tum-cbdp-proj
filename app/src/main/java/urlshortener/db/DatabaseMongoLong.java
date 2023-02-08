package urlshortener.db;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bson.Document;
import org.bson.types.Binary;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import urlshortener.raft.LogEntry;
import urlshortener.raft.LogEntryContent;

public class DatabaseMongoLong extends DatabaseOrdered<Long> {
    
    private final MongoClient client;
    private MongoDatabase db;

    private Long highestKey;

    public DatabaseMongoLong(String uri) {
        client = MongoClients.create(uri);
        db = client.getDatabase("urlshortener");
    }

    public Boolean isSeeded() {
        return db.listCollectionNames()
            .into(new ArrayList<String>())
            .contains("map");
    }

    @Override
    public boolean seed(boolean force) {
        if(force) return seed();

        Boolean isSeededBoolean = isSeeded();
        if(isSeededBoolean == null) return false;

        if(isSeededBoolean){
            System.out.println("DB already seeded; did nothing");
            return true;
        } else {
            System.out.println("DB not yet seeded; seeding");
            return seed();
        }
    }

    @Override
    public boolean seed() {
        // map
        {
            db.getCollection("map").drop();
            db.createCollection("map");
            db.getCollection("map").createIndex(
                Indexes.hashed("key")
            );
        }

        // kv
        {
            db.getCollection("kv").drop();
            db.createCollection("kv");
            db.getCollection("kv").createIndex(
                Indexes.ascending("key")
            );
        }

        // log
        {
            db.getCollection("log").drop();
            db.createCollection("log");
            db.getCollection("log").createIndex(
                Indexes.ascending("id"),
                new IndexOptions().unique(true)
            );
        }

        return true;
    }

    @Override
    public boolean init() {
        // Getting highest key
        Document d = db.getCollection("kv")
            .find()
            .sort(new BasicDBObject("key", -1))
            .limit(1)
            .first();

        highestKey = (d == null ? -1 : d.getLong("key"));

        // Load log
        loadLog();

        return true;
    }

    @Override
    public boolean put(String key, InputStream value) {
        try {
            byte[] valueBytes = new byte[value.available()];
            value.read(valueBytes);
            Document d = new Document("key", key).append("value", valueBytes);
            db.getCollection("map").insertOne(d);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public InputStream get(String key) {
        Document d = db.getCollection("map")
            .find(eq("key", key))
            .first();
        if(d == null) return null;
        Binary bin = d.get("value", Binary.class);
        InputStream ret = new ByteArrayInputStream(bin.getData());
        return ret;
    }

    @Override
    public boolean putKeyValue(Long key, String value) {
        Document d = new Document("key", key).append("value", value);
        db.getCollection("kv").insertOne(d);
        // System.out.println("DB: applied " + key + " => " + value);
        highestKey = Math.max(key, highestKey);
        return true;
    }

    @Override
    public String getKeyValue(Long key) {
        Document d = db.getCollection("kv")
            .find(eq("key", key))
            .first();
        if(d == null) return null;
        String ret = d.getString("value");
        return ret;
    }

    @Override
    public Long getHighestKey() {
        return highestKey;
    }

    ArrayList<LogEntry> log = new ArrayList<>();

    private boolean loadLog() {
        try {
            FindIterable<Document> rs = db.getCollection("log")
                .find()
                .sort(ascending("id"));
            Iterator<Document> it = rs.iterator();
            while(it.hasNext()){
                Document d = it.next();

                int id = d.getInteger("id");
                int term = d.getInteger("term");

                Binary bin = d.get("content", Binary.class);
                byte[] data = bin.getData();
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = ois.readObject();
                LogEntryContent content = (LogEntryContent)obj;

                if(id != log.size()) return false;

                log.add(new LogEntry(term, content));
            }

            return true;
        } catch(IOException | ClassNotFoundException e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int size() {
        return log.size();
    }

    @Override
    public LogEntry get(int index) {
        return log.get(index);
    }

    @Override
    public boolean deleteAfter(int index) {
        db.getCollection("log")
            .deleteMany(gte("id", index));
        return true;
    }

    @Override
    public boolean add(LogEntry logEntry) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(logEntry.content);
            oos.flush();

            Document d = new Document("id", log.size())
                .append("term", logEntry.term)
                .append("content", baos.toByteArray());
            db.getCollection("log").insertOne(d);

            log.add(logEntry);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addAll(List<LogEntry> list) {
        if(list.size() == 0) return true;

        List<Document> documents = new LinkedList<>();

        int N = log.size();
        try {
            for(int i = 0; i < list.size(); ++i){
                int j = N+i;
                LogEntry logEntry = list.get(i);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(logEntry.content);
                oos.flush();

                Document document = new Document("id", j)
                    .append("term", logEntry.term)
                    .append("content", baos.toByteArray());
                
                documents.add(document);
            }

            db.getCollection("log").insertMany(documents);

            log.addAll(list);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<LogEntry> getAfter(int index) {
        return log.subList(index, log.size());
    }
}
