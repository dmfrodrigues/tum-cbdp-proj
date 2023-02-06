package urlshortener;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import urlshortener.urlshortener.DatabaseOrdered;
import urlshortener.urlshortener.DatabasePostgresLong;

public class DatabasePostgresTest {
    @Test
    public void testPutGet() throws SQLException {
        Random random = new Random(0);

        String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");
        DatabaseOrdered<Long> db = new DatabasePostgresLong("jdbc:postgresql://localhost:5432/postgres", "postgres", POSTGRES_PASSWORD);
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
