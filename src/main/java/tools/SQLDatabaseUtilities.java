package tools;

import java.sql.*;
import java.util.*;

/**
 * Utility class for connecting to and performing CRUD operations on SQL databases using JDBC.
 * <p>
 * This class manages a single {@link Connection} and provides convenience methods
 * for executing queries, updates, and performing common database operations such as
 * insert, find, update, delete, and table lifecycle management.
 *
 * <pre>
 * // Option 1: Basic connection
 * SQLDatabaseUtilities db = new SQLDatabaseUtilities("jdbc:h2:mem:testdb");
 *
 * // Option 2: Credentials-based connection
 * SQLDatabaseUtilities db = new SQLDatabaseUtilities("jdbc:mysql://localhost:3306/mydb", "user", "pass");
 *
 * db.insertOne("users", Map.of("name", "Alice", "age", 30));
 * db.close();
 * </pre>
 */
public class SQLDatabaseUtilities implements AutoCloseable {
    private static final Logger log = Log.getLogger(SQLDatabaseUtilities.class);

    private final Connection connection;

    /**
     * Creates a new instance connected to the specified JDBC URL.
     *
     * @param url JDBC connection URL (e.g. {@code jdbc:h2:mem:testdb} or {@code jdbc:sqlite:sample.db})
     */
    public SQLDatabaseUtilities(String url) {
        try {
            this.connection = DriverManager.getConnection(url);
            log.info("SQL connection established to: " + url);
        } catch (SQLException e) {
            log.warn("Failed to connect to: " + url + " - " + e.getMessage());
            throw new RuntimeException("Error establishing SQL database connection", e);
        }
    }

    /**
     * Creates a new authenticated instance using explicit credentials.
     *
     * @param url      JDBC connection URL
     * @param username the username
     * @param password the password
     */
    public SQLDatabaseUtilities(String url, String username, String password) {
        try {
            this.connection = DriverManager.getConnection(url, username, password);
            log.info("SQL connection established to: " + url + " (authenticated as " + username + ")");
        } catch (SQLException e) {
            log.warn("Failed to connect to: " + url + " with user " + username + " - " + e.getMessage());
            throw new RuntimeException("Error establishing authenticated SQL database connection", e);
        }
    }

    /**
     * Creates a new instance using connection properties.
     *
     * @param url  JDBC connection URL
     * @param info custom properties
     */
    public SQLDatabaseUtilities(String url, Properties info) {
        try {
            this.connection = DriverManager.getConnection(url, info);
            log.info("SQL connection established to: " + url + " with properties");
        } catch (SQLException e) {
            log.warn("Failed to connect to: " + url + " - " + e.getMessage());
            throw new RuntimeException("Error establishing SQL database connection with properties", e);
        }
    }

    /**
     * Returns the underlying {@link Connection} for advanced operations.
     */
    public Connection getConnection() {
        return connection;
    }

    // ──────────────────────────────────────────────
    //  Generic Query / Execution Operations
    // ──────────────────────────────────────────────

    /**
     * Runs a query with the given parameters and returns a list of rows represented as Maps.
     * Column ordering is preserved using {@link LinkedHashMap}.
     *
     * @param sql    the SQL query with optional placeholders (?)
     * @param params optional placeholder values to bind
     * @return a List of Maps representing the result set rows
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String label = rsmd.getColumnLabel(i);
                        row.put(label, rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.warn("Execute query failed: " + sql + " | Details: " + e.getMessage());
            throw new RuntimeException("Error executing SQL query", e);
        }
        return results;
    }

    /**
     * Runs an update (INSERT, UPDATE, DELETE) query and returns the number of affected rows.
     *
     * @param sql    the SQL update statement with optional placeholders (?)
     * @param params optional placeholder values to bind
     * @return the number of rows affected
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Execute update failed: " + sql + " | Details: " + e.getMessage());
            throw new RuntimeException("Error executing SQL update", e);
        }
    }

    /**
     * Runs an arbitrary SQL statement and returns true if the first result is a ResultSet,
     * false otherwise.
     *
     * @param sql    the SQL statement with optional placeholders (?)
     * @param params optional placeholder values to bind
     * @return true if first result is a ResultSet, false if update count or no result
     */
    public boolean execute(String sql, Object... params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }
            return ps.execute();
        } catch (SQLException e) {
            log.warn("Execute failed: " + sql + " | Details: " + e.getMessage());
            throw new RuntimeException("Error executing SQL statement", e);
        }
    }

    // ──────────────────────────────────────────────
    //  Metadata / Connection Helpers
    // ──────────────────────────────────────────────

    /**
     * Pings the database to verify the connection is alive and valid.
     *
     * @return {@code true} if the connection is active and valid
     */
    public boolean ping() {
        try {
            boolean valid = connection != null && !connection.isClosed() && connection.isValid(5);
            if (valid) {
                log.info("SQL database connection is healthy.");
            } else {
                log.warn("SQL database connection is not active or is closed.");
            }
            return valid;
        } catch (Exception e) {
            log.warn("SQL database ping failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all user-created table names in the connected database.
     *
     * @return List of table names
     */
    public List<String> listTableNames() {
        List<String> tables = new ArrayList<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to retrieve table names: " + e.getMessage());
            throw new RuntimeException("Error reading database metadata", e);
        }
        return tables;
    }

    // ──────────────────────────────────────────────
    //  INSERT Operations
    // ──────────────────────────────────────────────

    /**
     * Inserts a single row built from the supplied key-value map.
     *
     * @param tableName the target table name
     * @param rowMap    the column-to-value map to insert
     * @return the number of affected rows (typically 1)
     */
    public int insertOne(String tableName, Map<String, Object> rowMap) {
        if (rowMap == null || rowMap.isEmpty()) {
            throw new IllegalArgumentException("Row map cannot be null or empty.");
        }
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder();
        List<Object> values = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : rowMap.entrySet()) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(entry.getKey());
            placeholders.append("?");
            values.add(entry.getValue());
            i++;
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        int affected = executeUpdate(sql.toString(), values.toArray());
        log.info("Inserted document into table " + tableName + " | Affected=" + affected);
        return affected;
    }

    /**
     * Inserts multiple rows in a batch.
     * Assumes all rows have the same set of column keys.
     *
     * @param tableName the target table name
     * @param rows      list of column-to-value maps to insert
     * @return the total number of affected rows across all batch queries
     */
    public int insertMany(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<String, Object> firstRow = rows.get(0);
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" (");
        StringBuilder placeholders = new StringBuilder();
        List<String> keys = new ArrayList<>(firstRow.keySet());
        int keySize = keys.size();
        for (int i = 0; i < keySize; i++) {
            if (i > 0) {
                sql.append(", ");
                placeholders.append(", ");
            }
            sql.append(keys.get(i));
            placeholders.append("?");
        }
        sql.append(") VALUES (").append(placeholders).append(")");

        String query = sql.toString();
        int totalAffected = 0;
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            boolean autoCommit = connection.getAutoCommit();
            if (autoCommit) {
                connection.setAutoCommit(false);
            }
            try {
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < keySize; i++) {
                        ps.setObject(i + 1, row.get(keys.get(i)));
                    }
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                connection.commit();
                for (int res : results) {
                    if (res > 0) {
                        totalAffected += res;
                    } else if (res == Statement.SUCCESS_NO_INFO) {
                        totalAffected++;
                    }
                }
                log.info("Batch inserted " + rows.size() + " rows into " + tableName);
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                if (autoCommit) {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            log.warn("Batch insert failed on table: " + tableName + " | Details: " + e.getMessage());
            throw new RuntimeException("Error running batch insert", e);
        }
        return totalAffected;
    }

    // ──────────────────────────────────────────────
    //  FIND / READ Operations
    // ──────────────────────────────────────────────

    /**
     * Returns all rows in a table.
     */
    public List<Map<String, Object>> findAll(String tableName) {
        List<Map<String, Object>> results = executeQuery("SELECT * FROM " + tableName);
        log.info("Found " + results.size() + " row(s) in " + tableName);
        return results;
    }

    /**
     * Returns rows matching the supplied SQL where clause.
     *
     * @param tableName   the target table name
     * @param whereClause SQL where condition (e.g. {@code "status = ? AND age > ?"})
     * @param params      placeholder bind values
     */
    public List<Map<String, Object>> find(String tableName, String whereClause, Object... params) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        List<Map<String, Object>> results = executeQuery(sql.toString(), params);
        log.info("Found " + results.size() + " row(s) in " + tableName + " matching condition");
        return results;
    }

    /**
     * Returns the first row matching the where clause, or {@code null} if none found.
     */
    public Map<String, Object> findOne(String tableName, String whereClause, Object... params) {
        List<Map<String, Object>> results = find(tableName, whereClause, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Returns the total count of rows in the table.
     */
    public long count(String tableName) {
        return count(tableName, null);
    }

    /**
     * Returns the count of rows matching the where clause.
     */
    public long count(String tableName, String whereClause, Object... params) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        List<Map<String, Object>> results = executeQuery(sql.toString(), params);
        if (!results.isEmpty()) {
            Object val = results.get(0).values().iterator().next();
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        }
        return 0;
    }

    // ──────────────────────────────────────────────
    //  UPDATE Operations
    // ──────────────────────────────────────────────

    private int updateRows(String tableName, Map<String, Object> columnValues, String whereClause, Object[] params) {
        if (columnValues == null || columnValues.isEmpty()) {
            throw new IllegalArgumentException("Column values cannot be null or empty.");
        }
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> values = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append(" = ?");
            values.add(entry.getValue());
            i++;
        }
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
            if (params != null) {
                values.addAll(Arrays.asList(params));
            }
        }
        return executeUpdate(sql.toString(), values.toArray());
    }

    /**
     * Updates matching rows (analogous to updateOne, but updates all matching records in SQL).
     *
     * @return the number of updated rows
     */
    public int updateOne(String tableName, Map<String, Object> columnValues, String whereClause, Object... params) {
        int affected = updateRows(tableName, columnValues, whereClause, params);
        log.info("Updated " + affected + " row(s) in " + tableName);
        return affected;
    }

    /**
     * Updates all rows matching the filter.
     */
    public int updateMany(String tableName, Map<String, Object> columnValues, String whereClause, Object... params) {
        int affected = updateRows(tableName, columnValues, whereClause, params);
        log.info("Updated " + affected + " row(s) in " + tableName);
        return affected;
    }

    // ──────────────────────────────────────────────
    //  DELETE Operations
    // ──────────────────────────────────────────────

    private int deleteRows(String tableName, String whereClause, Object[] params) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return executeUpdate(sql.toString(), params);
    }

    /**
     * Deletes the matching row(s).
     */
    public int deleteOne(String tableName, String whereClause, Object... params) {
        int affected = deleteRows(tableName, whereClause, params);
        log.info("Deleted " + affected + " row(s) from " + tableName);
        return affected;
    }

    /**
     * Deletes all matching rows.
     */
    public int deleteMany(String tableName, String whereClause, Object... params) {
        int affected = deleteRows(tableName, whereClause, params);
        log.info("Deleted " + affected + " row(s) from " + tableName);
        return affected;
    }

    /**
     * Drops the specified table if it exists.
     */
    public void dropTable(String tableName) {
        executeUpdate("DROP TABLE IF EXISTS " + tableName);
        log.info("Dropped table: " + tableName);
    }

    // ──────────────────────────────────────────────
    //  CONNECTION Lifecycle
    // ──────────────────────────────────────────────

    /**
     * Closes the database connection.
     * This instance should not be used after calling close.
     */
    @Override
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    log.info("SQL connection closed.");
                }
            } catch (SQLException e) {
                log.warn("Error closing SQL connection: " + e.getMessage());
            }
        }
    }
}
