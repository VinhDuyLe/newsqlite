import edu.scu.DatabaseManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DatabaseManagerTest {
    DatabaseManager dbManager;
    private final String dbPath = "test1.db";

    @Before
    public void setUp() {
        dbManager = new DatabaseManager(dbPath);
    }

    @Test
    public void testExecuteQuery() {
        String tableName = "records1";
        List<List<String>> queryResults = dbManager.executeQuery("SELECT * FROM " + tableName);
        for (List<String> row : queryResults) {
            System.out.println(row);
        }
    }
}
