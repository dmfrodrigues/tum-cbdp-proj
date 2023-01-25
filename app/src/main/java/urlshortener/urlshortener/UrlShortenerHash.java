package urlshortener.urlshortener;

import java.sql.SQLException;
import java.util.Base64;

public class UrlShortenerHash implements UrlShortener {
    Database db;

    public UrlShortenerHash(Database db){
        this.db = db;
    }

    public String shorten(String url){
        Integer hash = url.hashCode();
        String hashString = hash.toString();
        String id = new String(Base64.getEncoder().encode(hashString.getBytes()));

        try {
            db.put(id, url);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    @Override
    public String enlongate(String id) {
        try {
            return db.get(id);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
