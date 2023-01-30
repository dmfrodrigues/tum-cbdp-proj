package urlshortener;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.Test;

import urlshortener.urlshortener.Database;
import urlshortener.urlshortener.DatabasePostgres;
import urlshortener.urlshortener.Database.Stored;

public class DatabaseTest {
    @Test
    public void testStoredVariables() throws SQLException, IOException {
        String POSTGRES_PASSWORD = System.getenv("POSTGRES_PASSWORD");
        Database db = new DatabasePostgres("jdbc:postgresql://localhost:5432/postgres", "postgres", POSTGRES_PASSWORD);
        db.seed();

        // Single variable
        Stored<Integer> i = db.loadStoredVariable("i", 2);
        assertEquals(i.get(), Integer.valueOf(2));

        // Modify variable
        i.put(3);
        assertEquals(i.get(), Integer.valueOf(3));

        // Two variables at same time
        Stored<Integer> i2 = db.loadStoredVariable("i", 4);
        assertEquals(i .get(), Integer.valueOf(3));
        assertEquals(i2.get(), Integer.valueOf(3));
    }
}
