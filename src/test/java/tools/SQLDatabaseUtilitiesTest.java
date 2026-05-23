package tools;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.*;

/**
 * Unit tests for the {@link SQLDatabaseUtilities} class using H2 in-memory database.
 */
public class SQLDatabaseUtilitiesTest {

    private SQLDatabaseUtilities db;
    private static final String H2_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";

    @BeforeClass
    public void setupClass() {
        // Establish connection to H2 in-memory database
        db = new SQLDatabaseUtilities(H2_URL, "sa", "");
    }

    @BeforeMethod
    public void setupMethod() {
        // Drop table if exists and recreate it for a clean state
        db.dropTable("users");
        db.executeUpdate("CREATE TABLE users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), age INT, status VARCHAR(50))");
    }

    @AfterClass
    public void teardownClass() {
        if (db != null) {
            db.dropTable("users");
            db.close();
        }
    }

    @Test
    public void testPingAndConnection() {
        Assert.assertTrue(db.ping(), "Database should be reachable.");
        Assert.assertNotNull(db.getConnection(), "Underlying connection should not be null.");
    }

    @Test
    public void testInsertAndFindAll() {
        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "Alice");
        user1.put("age", 30);
        user1.put("status", "active");

        int affected = db.insertOne("users", user1);
        Assert.assertEquals(affected, 1, "Should insert one row.");

        List<Map<String, Object>> allUsers = db.findAll("users");
        Assert.assertEquals(allUsers.size(), 1, "Should find exactly 1 user.");
        Assert.assertEquals(allUsers.get(0).get("NAME"), "Alice");
        Assert.assertEquals(allUsers.get(0).get("AGE"), 30);
        Assert.assertEquals(allUsers.get(0).get("STATUS"), "active");
    }

    @Test
    public void testInsertManyAndFind() {
        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> user1 = new LinkedHashMap<>();
        user1.put("name", "Bob");
        user1.put("age", 25);
        user1.put("status", "active");
        users.add(user1);

        Map<String, Object> user2 = new LinkedHashMap<>();
        user2.put("name", "Charlie");
        user2.put("age", 35);
        user2.put("status", "inactive");
        users.add(user2);

        int affected = db.insertMany("users", users);
        Assert.assertEquals(affected, 2, "Should insert two rows.");

        List<Map<String, Object>> activeUsers = db.find("users", "status = ?", "active");
        Assert.assertEquals(activeUsers.size(), 1, "Should find 1 active user.");
        Assert.assertEquals(activeUsers.get(0).get("NAME"), "Bob");

        List<Map<String, Object>> inactiveUsers = db.find("users", "status = ?", "inactive");
        Assert.assertEquals(inactiveUsers.size(), 1, "Should find 1 inactive user.");
        Assert.assertEquals(inactiveUsers.get(0).get("NAME"), "Charlie");
    }

    @Test
    public void testFindOne() {
        db.insertOne("users", Map.of("name", "David", "age", 40, "status", "active"));

        Map<String, Object> user = db.findOne("users", "name = ?", "David");
        Assert.assertNotNull(user, "User should be found.");
        Assert.assertEquals(user.get("AGE"), 40);

        Map<String, Object> missingUser = db.findOne("users", "name = ?", "NonExistent");
        Assert.assertNull(missingUser, "User should not be found.");
    }

    @Test
    public void testCount() {
        Assert.assertEquals(db.count("users"), 0);

        db.insertOne("users", Map.of("name", "Eve", "age", 22, "status", "active"));
        db.insertOne("users", Map.of("name", "Frank", "age", 28, "status", "inactive"));

        Assert.assertEquals(db.count("users"), 2);
        Assert.assertEquals(db.count("users", "status = ?", "active"), 1);
        Assert.assertEquals(db.count("users", "age > ?", 25), 1);
    }

    @Test
    public void testUpdate() {
        db.insertOne("users", Map.of("name", "Grace", "age", 29, "status", "active"));

        int updated = db.updateOne("users", Map.of("age", 30, "status", "inactive"), "name = ?", "Grace");
        Assert.assertEquals(updated, 1, "Should update one row.");

        Map<String, Object> user = db.findOne("users", "name = ?", "Grace");
        Assert.assertEquals(user.get("AGE"), 30);
        Assert.assertEquals(user.get("STATUS"), "inactive");
    }

    @Test
    public void testDelete() {
        db.insertOne("users", Map.of("name", "Heidi", "age", 31, "status", "active"));
        Assert.assertEquals(db.count("users"), 1);

        int deleted = db.deleteOne("users", "name = ?", "Heidi");
        Assert.assertEquals(deleted, 1, "Should delete one row.");
        Assert.assertEquals(db.count("users"), 0);
    }

    @Test
    public void testListTableNames() {
        List<String> tables = db.listTableNames();
        Assert.assertTrue(tables.contains("USERS"), "Table names list should contain USERS table.");
    }

    @Test
    public void testGenericExecute() {
        boolean hasResultSet = db.execute("SELECT * FROM users");
        Assert.assertTrue(hasResultSet, "Select query should yield a result set.");

        boolean noResultSet = db.execute("UPDATE users SET age = 50 WHERE name = 'nonexistent'");
        Assert.assertFalse(noResultSet, "Update statement should not yield a result set.");
    }
}
