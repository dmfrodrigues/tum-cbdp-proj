package urlshortener;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import urlshortener.db.DatabaseMongoLong;
import urlshortener.db.DatabaseOrdered;

public class DatabaseMongoTest {
    @Test
    public void testPutGetSmall() {
        DatabaseOrdered<Long> db = new DatabaseMongoLong("mongodb://localhost:27017");
        db.seed();
        db.init();

        assertTrue(db.putKeyValue(1L, "a"));
        assertEquals("a", db.getKeyValue(1L));

        assertTrue(db.putKeyValue(1L, "b"));
        assertEquals("b", db.getKeyValue(1L));
    }

    @Test
    public void testPutGet() {
        Random random = new Random(0);

        DatabaseOrdered<Long> db = new DatabaseMongoLong("mongodb://localhost:27017");
        db.seed();
        db.init();

        int N = 10000;
        List<String> valueList = new ArrayList<>();
        List<Long> keyList = new ArrayList<>();
        for(int i = 0; i < N; ++i){
            String value = TestUtils.getRandomURL(random);
            valueList.add(value);
            keyList.add(Long.valueOf(i));
        }

        System.out.println("N=" + N);

        long tBegin, tEnd, elapsed;

        tBegin = System.nanoTime();
        for(int i = 0; i < N; ++i){
            db.putKeyValue(keyList.get(i), valueList.get(i));
        }
        tEnd = System.nanoTime();
        elapsed = tEnd-tBegin;
        System.out.println("PUT | Total: " + elapsed/1000.0 + "micros, per query: " + elapsed/N/1000.0 + "micros");

        tBegin = System.nanoTime();
        for(int i = 0; i < N; ++i){
            db.getKeyValue(keyList.get(i));
        }
        tEnd = System.nanoTime();
        elapsed = tEnd-tBegin;
        System.out.println("GET | Total: " + elapsed/1000.0 + "micros, per query: " + elapsed/N/1000.0 + "micros");
    }
}
