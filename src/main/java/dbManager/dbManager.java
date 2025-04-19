package dbManager;

import Utils.WebDocument;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class dbManager {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String CONNECTION_STRING = dotenv.get("MONGO_URL");

    private static final String DB_NAME = "search_engine";
    private static final String COLLECTION_NAME = "documents";

    private final MongoClient mongoClient;
    private final MongoCollection<Document> tokensCollection;  // Renamed for proper casing
    private MongoDatabase database;
    private final MongoCollection<Document> collection;

    public dbManager() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
        tokensCollection = database.getCollection("tokens");  // Renamed for proper casing
        System.out.println("Connected to MongoDB Atlas.");
    }

    // Insert a document
    public void insertDocument(String url, String title, String content) {
        Document doc = new Document("url", url)
                .append("title", title)
                .append("content", content)
                .append("timestamp", System.currentTimeMillis())
                .append("indexed", false);  // Ensure documents are indexed as false initially

        collection.insertOne(doc);
        System.out.println("Document inserted: " + title);
    }

    // Search documents by keyword
    public void searchByKeyword(String keyword) {
        Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        FindIterable<Document> results = collection.find(
                Filters.or(
                        Filters.regex("title", pattern),
                        Filters.regex("content", pattern)
                )
        );

        for (Document doc : results) {
            System.out.println("Title: " + doc.getString("title"));
            System.out.println("URL: " + doc.getString("url"));
            System.out.println("----");
        }
    }

    // Get documents with 'indexed' == false
    public ArrayList<WebDocument> getDocuments() {
        ArrayList<WebDocument> docs = new ArrayList<>();

        FindIterable<Document> results = collection.find(
                Filters.eq("indexed", false)
        );

        for (Document doc : results) {
            String url = doc.getString("url");
            String title = doc.getString("title");
            String content = doc.getString("content");
            String id = doc.getObjectId("_id").toString();  // Use ObjectId if _id is the default MongoDB ID field
            boolean indexed = doc.getBoolean("indexed", false);

            WebDocument webDoc = new WebDocument(Integer.parseInt(id), url, title, content);  // Convert ObjectId to String
            docs.add(webDoc);
        }

        return docs;
    }

    // Mark a document as indexed
    public void markAsIndexed(ObjectId id) {
        collection.updateOne(
                Filters.eq("_id", id),
                Updates.set("indexed", true)
        );
    }

    // Insert token into the tokens collection
    public void insertToken(String token, String docId, int frequency, List<Integer> positions) {
        try {
            Document docInfo = new Document("docId", docId)
                    .append("frequency", frequency)
                    .append("positions", positions.stream().distinct().toList()); // Remove duplicate positions

            // Update token with the document info
            tokensCollection.updateOne(
                    Filters.eq("_id", token),
                    Updates.combine(
                            Updates.set("docs." + docId, docInfo), // Prevent duplicate docId
                            Updates.setOnInsert("_id", token)  // Set _id if new token
                    ),
                    new com.mongodb.client.model.UpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            e.printStackTrace();  // Log the error for debugging
        }
    }

    // Close the database connection
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }

    public static void main(String[] args) {
        dbManager DBM = new dbManager();
        DBM.insertToken("anas", "123", 11, new ArrayList<>(Arrays.asList(1,2,50,5)));
        DBM.insertToken("anas", "1234", 11, new ArrayList<>(Arrays.asList(1,21,50,5)));
        DBM.insertToken("anas2", "1234", 11, new ArrayList<>(Arrays.asList(1,2,3,4)));
        DBM.close();
    }
}
