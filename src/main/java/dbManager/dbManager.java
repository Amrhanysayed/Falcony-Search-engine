package dbManager;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;

import java.util.regex.Pattern;

public class dbManager {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String CONNECTION_STRING = dotenv.get("MONGO_URL");

    private static final String DB_NAME = "search_engine";
    private static final String COLLECTION_NAME = "documents";

    private final MongoClient mongoClient;
    private MongoDatabase database;
    private final MongoCollection<Document> collection;

    public dbManager() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);
        collection = database.getCollection(COLLECTION_NAME);
        System.out.println("Connected to MongoDB Atlas.");
    }

    // Insert a document
    public void insertDocument(String url, String title, String content) {
        Document doc = new Document("url", url)
                .append("title", title)
                .append("content", content)
                .append("timestamp", System.currentTimeMillis());

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

    // Close connection
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }
}
