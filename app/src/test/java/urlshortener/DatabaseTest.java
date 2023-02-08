package urlshortener;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Test;

import urlshortener.db.DatabaseMongoLong;
import urlshortener.db.DatabaseOrdered;
import urlshortener.db.PersistentMap.Stored;

public class DatabaseTest {
    @Test(timeout = 10000)
    public void testStoredVariables() throws SQLException, IOException {
        DatabaseOrdered<Long> db = new DatabaseMongoLong("mongodb://localhost:27017");
        db.seed();

        // Single variable
        Stored<Integer> i = db.loadStoredVariable("i", 2);
        assertEquals(i.get(), Integer.valueOf(2));

        // Modify variable
        i.set(3);
        assertEquals(i.get(), Integer.valueOf(3));

        // Two variables at same time
        Stored<Integer> i2 = db.loadStoredVariable("i", 4);
        assertEquals(i .get(), Integer.valueOf(3));
        assertEquals(i2.get(), Integer.valueOf(3));

        // Modify to null
        i.set(null);
        assertEquals(i.get(), null);
    }
}
