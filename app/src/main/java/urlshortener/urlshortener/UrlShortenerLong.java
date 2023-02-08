package urlshortener.urlshortener;

import java.nio.ByteBuffer;
import java.util.Base64;

import urlshortener.db.PersistentStateMachineOrdered;
import urlshortener.raft.Raft;

public class UrlShortenerLong extends UrlShortener {
    PersistentStateMachineOrdered<Long> db;

    public UrlShortenerLong(PersistentStateMachineOrdered<Long> db, Raft raft){
        super(raft);
        this.db = db;
    }

    // Encode and decode from:
    // https://stackoverflow.com/questions/4485128/how-do-i-convert-long-to-byte-and-back-in-java

    private String encode(Long n){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(0, n);
        return new String(Base64.getEncoder().encode(buffer.array()));
    }

    private Long decode(String s){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        byte[] bytes = Base64.getDecoder().decode(s.getBytes());
        if(bytes.length != 8) return null;
        buffer.put(bytes, 0, bytes.length);
        buffer.flip();
        return buffer.getLong();
    }

    public String shortenURL(String url){
        return encode(db.getHighestKey()+1);
    }

    @Override
    public String enlongate(String id) {
        Long key = decode(id);
        if(key == null) return null;
        return db.getKeyValue(key);
    }

    @Override
    public void commit(String id, String url) {
        Long key = decode(id);
        if(key == null) return;
        db.putKeyValue(key, url);
    }
}
