package tools;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for connecting to and performing CRUD operations on MongoDB.
 * <p>
 * This class manages a single {@link MongoClient} connection per instance
 * and provides convenience methods for common database operations such as
 * insert, find, update, delete, and aggregation.
 *
 * <pre>
 * // Option 1: No auth / credentials embedded in URI
 * MongoDBUtilities mongo = new MongoDBUtilities("mongodb://localhost:27017");
 *
 * // Option 2: Credentials in the URI itself
 * MongoDBUtilities mongo = new MongoDBUtilities("mongodb://user:pass@host:27017/authDb");
 *
 * // Option 3: Explicit username, password, and auth database
 * MongoDBUtilities mongo = new MongoDBUtilities("mongodb://host:27017", "admin", "user", "pass");
 *
 * mongo.insertOne("myDb", "users", Map.of("name", "Alice", "age", 30));
 * mongo.close();
 * </pre>
 */
public class MongoDBUtilities implements AutoCloseable {

    private final MongoClient mongoClient;

    /**
     * Creates a new instance connected to the specified MongoDB URI.
     * <p>
     * Credentials can be embedded in the URI itself:
     * {@code mongodb://username:password@host:27017/authDatabase}
     *
     * @param connectionUri MongoDB connection string
     *                      (e.g. {@code mongodb://localhost:27017} or an Atlas SRV URI)
     */
    public MongoDBUtilities(String connectionUri) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionUri))
                .applyToConnectionPoolSettings(builder ->
                        builder.maxConnectionIdleTime(60, TimeUnit.SECONDS))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10, TimeUnit.SECONDS))
                .build();
        this.mongoClient = MongoClients.create(settings);
        Log.info("MongoDB connection established to: " + connectionUri);
    }

    /**
     * Creates a new authenticated instance using explicit credentials.
     *
     * @param connectionUri MongoDB connection string (host/port only, e.g. {@code mongodb://localhost:27017})
     * @param authDatabase  the database to authenticate against (commonly {@code "admin"})
     * @param username      the username
     * @param password      the password
     */
    public MongoDBUtilities(String connectionUri, String authDatabase, String username, String password) {
        MongoCredential credential = MongoCredential.createCredential(
                username, authDatabase, password.toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionUri))
                .credential(credential)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxConnectionIdleTime(60, TimeUnit.SECONDS))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10, TimeUnit.SECONDS))
                .build();
        this.mongoClient = MongoClients.create(settings);
        Log.info("MongoDB connection established to: " + connectionUri + " (authenticated as " + username + ")");
    }

    // ──────────────────────────────────────────────
    //  Database & Collection helpers
    // ──────────────────────────────────────────────

    /**
     * Returns the {@link MongoDatabase} for the given name.
     */
    public MongoDatabase getDatabase(String databaseName) {
        return mongoClient.getDatabase(databaseName);
    }

    /**
     * Returns the {@link MongoCollection} for the given database and collection name.
     */
    public MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        return getDatabase(databaseName).getCollection(collectionName);
    }

    /**
     * Lists all database names available on the connected server.
     */
    public List<String> listDatabaseNames() {
        List<String> names = new ArrayList<>();
        mongoClient.listDatabaseNames().forEach(names::add);
        return names;
    }

    /**
     * Lists all collection names in the specified database.
     */
    public List<String> listCollectionNames(String databaseName) {
        List<String> names = new ArrayList<>();
        getDatabase(databaseName).listCollectionNames().forEach(names::add);
        return names;
    }

    // ──────────────────────────────────────────────
    //  INSERT operations
    // ──────────────────────────────────────────────

    /**
     * Inserts a single document built from the supplied key-value map.
     *
     * @return the {@link InsertOneResult} containing the generated {@code _id}
     */
    public InsertOneResult insertOne(String databaseName, String collectionName, Map<String, Object> documentMap) {
        Document doc = new Document(documentMap);
        InsertOneResult result = getCollection(databaseName, collectionName).insertOne(doc);
        Log.info("Inserted document into " + databaseName + "." + collectionName
                + " | _id=" + result.getInsertedId());
        return result;
    }

    /**
     * Inserts a single {@link Document}.
     */
    public InsertOneResult insertOne(String databaseName, String collectionName, Document document) {
        InsertOneResult result = getCollection(databaseName, collectionName).insertOne(document);
        Log.info("Inserted document into " + databaseName + "." + collectionName
                + " | _id=" + result.getInsertedId());
        return result;
    }

    /**
     * Inserts multiple documents in a single batch.
     *
     * @return the {@link InsertManyResult} with all generated IDs
     */
    public InsertManyResult insertMany(String databaseName, String collectionName, List<Document> documents) {
        InsertManyResult result = getCollection(databaseName, collectionName).insertMany(documents);
        Log.info("Inserted " + result.getInsertedIds().size() + " documents into "
                + databaseName + "." + collectionName);
        return result;
    }

    // ──────────────────────────────────────────────
    //  FIND / READ operations
    // ──────────────────────────────────────────────

    /**
     * Returns all documents in a collection.
     */
    public List<Document> findAll(String databaseName, String collectionName) {
        List<Document> results = new ArrayList<>();
        getCollection(databaseName, collectionName).find().forEach(results::add);
        Log.info("Found " + results.size() + " documents in " + databaseName + "." + collectionName);
        return results;
    }

    /**
     * Returns documents matching the supplied BSON filter.
     *
     * @param filter a BSON filter (e.g. {@code Filters.eq("status", "active")})
     * @see Filters
     */
    public List<Document> find(String databaseName, String collectionName, Bson filter) {
        List<Document> results = new ArrayList<>();
        getCollection(databaseName, collectionName).find(filter).forEach(results::add);
        Log.info("Found " + results.size() + " documents in " + databaseName + "." + collectionName
                + " matching filter");
        return results;
    }

    /**
     * Returns the first document matching the filter, or {@code null} if none found.
     */
    public Document findOne(String databaseName, String collectionName, Bson filter) {
        return getCollection(databaseName, collectionName).find(filter).first();
    }

    /**
     * Returns the count of documents matching the filter.
     */
    public long count(String databaseName, String collectionName, Bson filter) {
        return getCollection(databaseName, collectionName).countDocuments(filter);
    }

    /**
     * Returns the total count of documents in a collection.
     */
    public long count(String databaseName, String collectionName) {
        return getCollection(databaseName, collectionName).countDocuments();
    }

    // ──────────────────────────────────────────────
    //  UPDATE operations
    // ──────────────────────────────────────────────

    /**
     * Updates the first document matching the filter.
     *
     * @param filter BSON filter for matching
     * @param update BSON update operations (e.g. {@code Updates.set("status", "closed")})
     * @return the {@link UpdateResult}
     * @see Updates
     */
    public UpdateResult updateOne(String databaseName, String collectionName, Bson filter, Bson update) {
        UpdateResult result = getCollection(databaseName, collectionName).updateOne(filter, update);
        Log.info("Updated " + result.getModifiedCount() + " document(s) in "
                + databaseName + "." + collectionName);
        return result;
    }

    /**
     * Updates all documents matching the filter.
     */
    public UpdateResult updateMany(String databaseName, String collectionName, Bson filter, Bson update) {
        UpdateResult result = getCollection(databaseName, collectionName).updateMany(filter, update);
        Log.info("Updated " + result.getModifiedCount() + " document(s) in "
                + databaseName + "." + collectionName);
        return result;
    }

    /**
     * Replaces a single document matching the filter with the given replacement document.
     */
    public UpdateResult replaceOne(String databaseName, String collectionName, Bson filter, Document replacement) {
        UpdateResult result = getCollection(databaseName, collectionName).replaceOne(filter, replacement);
        Log.info("Replaced " + result.getModifiedCount() + " document(s) in "
                + databaseName + "." + collectionName);
        return result;
    }

    // ──────────────────────────────────────────────
    //  DELETE operations
    // ──────────────────────────────────────────────

    /**
     * Deletes the first document matching the filter.
     */
    public DeleteResult deleteOne(String databaseName, String collectionName, Bson filter) {
        DeleteResult result = getCollection(databaseName, collectionName).deleteOne(filter);
        Log.info("Deleted " + result.getDeletedCount() + " document(s) from "
                + databaseName + "." + collectionName);
        return result;
    }

    /**
     * Deletes all documents matching the filter.
     */
    public DeleteResult deleteMany(String databaseName, String collectionName, Bson filter) {
        DeleteResult result = getCollection(databaseName, collectionName).deleteMany(filter);
        Log.info("Deleted " + result.getDeletedCount() + " document(s) from "
                + databaseName + "." + collectionName);
        return result;
    }

    /**
     * Drops an entire collection.
     */
    public void dropCollection(String databaseName, String collectionName) {
        getCollection(databaseName, collectionName).drop();
        Log.info("Dropped collection: " + databaseName + "." + collectionName);
    }

    // ──────────────────────────────────────────────
    //  AGGREGATION
    // ──────────────────────────────────────────────

    /**
     * Runs an aggregation pipeline and returns the resulting documents.
     *
     * @param pipeline list of BSON aggregation stages
     */
    public List<Document> aggregate(String databaseName, String collectionName, List<Bson> pipeline) {
        List<Document> results = new ArrayList<>();
        getCollection(databaseName, collectionName).aggregate(pipeline).forEach(results::add);
        Log.info("Aggregation returned " + results.size() + " documents from "
                + databaseName + "." + collectionName);
        return results;
    }

    // ──────────────────────────────────────────────
    //  CONNECTION lifecycle
    // ──────────────────────────────────────────────

    /**
     * Pings the server to verify the connection is alive.
     *
     * @return {@code true} if the ping succeeds
     */
    public boolean ping(String databaseName) {
        try {
            getDatabase(databaseName).runCommand(new Document("ping", 1));
            Log.info("MongoDB ping successful on database: " + databaseName);
            return true;
        } catch (Exception e) {
            Log.warn("MongoDB ping failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns the underlying {@link MongoClient} for advanced operations
     * not covered by this utility.
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * Closes the MongoDB connection.
     * This instance should not be used after calling close.
     */
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            Log.info("MongoDB connection closed.");
        }
    }
}
