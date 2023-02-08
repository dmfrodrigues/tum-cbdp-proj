package urlshortener.urlshortener;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import static com.mongodb.client.model.Filters.*;

public class DatabaseMongo implements Database {

    private static final String defaultUri = "mongodb://localhost:27017";
    
    private final MongoClient client;
    private MongoDatabase db;

    public DatabaseMongo() {
        this(DatabaseMongo.defaultUri);
    }

    public DatabaseMongo(String uri) {
        client = MongoClients.create(uri);
        db = client.getDatabase("node");
    }


    @Override
    public boolean seed() throws Exception {
        db.getCollection("short2url").drop();;
        db.createCollection("short2url");
        IndexOptions indexOptions = new IndexOptions().unique(true);
        db.getCollection("short2url").createIndex(
            Indexes.ascending("short"),
            indexOptions
        );

        db.getCollection("state").drop();
        db.createCollection("state");
        
        return true;
    }

    @Override
    public void put(String id, String url) throws Exception {
        Document d = new Document("short", "id").append("url", url);
        db.getCollection("short2url").insertOne(d);     
    }

    @Override
    public String get(String id) throws Exception {
        Bson filter = eq("short", id);
        Document d = db.getCollection("short2url").find(filter).first();

        return d.getString("url");
    }
    
}
