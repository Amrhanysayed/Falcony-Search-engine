package dbManager;

import Utils.Posting;
import Utils.WebDocument;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    // crawler
    ///  TODO: url, doc
    public void insertDocument(String url, String title, String content) {

        Document doc = new Document("url", url)
                .append("title", title.trim())
                .append("content", content.trim())
                .append("timestamp", System.currentTimeMillis())
                .append("indexed", false);

        collection.insertOne(doc);
        System.out.println("Document inserted: " + title);
    }

    public void insertDocuments(List<Document> documents) {
        try {
            if (!documents.isEmpty()) {
                collection.insertMany(documents);
                System.out.println("Inserted " + documents.size() + " documents");
            }
        } catch (Exception e) {
            System.err.println("Failed to insert documents: " + e.getMessage());
        }
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
    public ConcurrentHashMap<String, WebDocument> getNonIndexedDocuments() {
        ConcurrentHashMap<String, WebDocument> docs = new ConcurrentHashMap<>();

        FindIterable<Document> results = collection.find(
                Filters.eq("indexed", false)
        );

        for (Document doc : results) {
            String url = doc.getString("url");
            String title = doc.getString("title");
            String content = doc.getString("content");
            String id = doc.getObjectId("_id").toString();  // Use ObjectId if _id is the default MongoDB ID field
            boolean indexed = doc.getBoolean("indexed", false);

            WebDocument webDoc = new WebDocument(id, url, title, content);  // Convert ObjectId to String
            docs.put(id, webDoc);
        }

        return docs;
    }

    // Mark a document as indexed
    public void markAsIndexed(List<String> ids) {
        List<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        Document filter = new Document("_id", new Document("$in", objectIds));
        Document update = new Document("$set", new Document("indexed", true));
        collection.updateMany(filter, update);
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

    public void insertTokens(Map<String, List<Posting>> invertedIndex) {
        try {
            // List to hold bulk write operations
            List<UpdateOneModel<Document>> bulkUpdates = new ArrayList<>();

            // Iterate over the inverted index
            for (Map.Entry<String, List<Posting>> entry : invertedIndex.entrySet()) {
                String token = entry.getKey();
                List<Posting> terms = entry.getValue();

                // Create updates for each TermInfo
                for (Posting posting : terms) {
                    String docId = posting.getDocId();
                    int frequency = posting.getFrequency();
                    List<Integer> positions = posting.getPositions().stream().distinct().toList();
                    ObjectId id = new ObjectId(docId);
                    // Create document info
                    Document docInfo = new Document("docId", id)
                            .append("frequency", frequency)
                            .append("positions", positions);

                    // Create UpdateOneModel for this TermInfo
                    UpdateOneModel<Document> update = new UpdateOneModel<>(
                            new Document("_id", token), // Filter by token
                            Updates.combine(
                                    Updates.set("docs." + docId, docInfo), // Set document info
                                    Updates.setOnInsert("_id", token) // Set _id if new token
                            ),
                            new UpdateOptions().upsert(true) // Enable upsert
                    );

                    bulkUpdates.add(update);
                }
            }

            // Execute bulk write if there are updates
            if (!bulkUpdates.isEmpty()) {
                tokensCollection.bulkWrite(bulkUpdates, new BulkWriteOptions().ordered(false));
                System.out.println("Bulk write completed for " + bulkUpdates.size() + " updates.");
            } else {
                System.out.println("No updates to perform.");
            }
        } catch (Exception e) {
            System.err.println("Error during bulk write: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    //
//    public List<WebDocument> getDocsForTokens(List<String> tokens, boolean intersect) {
//        try {
//            // Use a Set to ensure unique docIds
//            Set<String> docIdSet = new HashSet<>();
//            long startTime = System.currentTimeMillis();
//
//            // Query tokensCollection for documents where _id is in the token list
//            // Project only the 'docs' field to reduce data transfer
//            for (Document doc : tokensCollection.find(Filters.in("_id", tokens))
//                    .projection(new Document("docs", 1).append("_id", 0))) {
//                // Get the 'docs' subdocument
//                Document docs = doc.get("docs", Document.class);
//                if (docs != null) {
//                    Set<String> docIds = docs.keySet();
//                    if(intersect && !docIdSet.isEmpty()) {
//                        docIdSet.retainAll(docIds);
//                    }
//                    else {
//                        docIdSet.addAll(docIds);
//                    }
//                }
//            }
//
//            // Convert Set to List for return
//            List<String> docIdList = new ArrayList<>(docIdSet);
//
//            long endTime = System.currentTimeMillis();
//            System.out.printf("Retrieved %d unique docIds for %d tokens in %.2f seconds%n",
//                    docIdList.size(), tokens.size(), (endTime - startTime) / 1000.0);
//
//            return getDocumentsByIds(docIdList);
//        } catch (Exception e) {
//            System.err.println("Error retrieving docIds: " + e.getMessage());
//            e.printStackTrace();
//            return new ArrayList<>(); // Return empty list on error
//        }
//    }
//
//
//    public List<WebDocument> getDocumentsByIds(List<String> docIds) {
//        List<WebDocument> docs = new ArrayList<>();
//
//        List<ObjectId> objectIds = docIds.stream()
//                .map(ObjectId::new)
//                .collect(Collectors.toList());
//
//        for (Document doc : collection.find(Filters.in("_id", objectIds))) {
//            String id = doc.getObjectId("_id").toString();
//            String url = doc.getString("url");
//            String title = doc.getString("title");
//            String content = doc.getString("content");
//
//            WebDocument webDoc = new WebDocument(id, url, title, content);
//            docs.add(webDoc);
//        }
//
//        return docs;
//    }
//
//
//

    public Set<String> getDocIdsForTokens(List<String> tokens, boolean intersect) {
        try {
            // Use a Set to ensure unique docIds
            Set<String> docIdSet = new HashSet<>();
            long startTime = System.currentTimeMillis();
            for (Document doc : tokensCollection.find(Filters.in("_id", tokens))
                    .projection(new Document("docs", 1).append("_id", 1))) {
                // Get the 'docs' subdocument
                String token = doc.getString("_id");
                Document docs = doc.get("docs", Document.class);
                if (docs != null) {
                    Set<String> docIds = docs.keySet();
                    if(intersect && !docIdSet.isEmpty()) {
                        docIdSet.retainAll(docIds);
                    }
                    else {
                        docIdSet.addAll(docIds);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            System.out.printf("Retrieved %d unique docIds for %d tokens in %.2f seconds%n",
                    docIdSet.size(), tokens.size(), (endTime - startTime) / 1000.0);

            return docIdSet;
        } catch (Exception e) {
            System.err.println("Error retrieving docIds: " + e.getMessage());
            e.printStackTrace();
            return new HashSet<>(); // Return empty list on error
        }
    }

    public Map<String, List<Posting>> getPostingsForTokens(List<String> tokens, Set<String> docIdSet) {
        try {
            // Use a Set to ensure unique docIds
            Map<String, List<Posting>> tokenToTokenInfos = new HashMap<>(); // we need to return this as well

            for (Document doc : tokensCollection.find(Filters.in("_id", tokens))
                    .projection(new Document("docs", 1).append("_id", 1))) {
                // Get the 'docs' subdocument
                String token = doc.getString("_id");
                Document docs = doc.get("docs", Document.class);
                if (docs != null) {
                    // Extract docIds and TokenInfo for this token
                    List<Posting> postings = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : docs.entrySet()) {
                        String docId = entry.getKey();
                        if (docIdSet.contains(docId)) {
                            Document docInfo = (Document) entry.getValue();

                            // Create TokenInfo
                            Posting posting = new Posting(
                                    token,
                                    docInfo.getInteger("frequency", 0),
                                    docId,
                                    docInfo.getList("positions", Integer.class, new ArrayList<>())
                            );
                            postings.add(posting);
                        }
                    }

                    // Store TokenInfo list for this token
                    tokenToTokenInfos.put(token, postings);
                }
            }

            return tokenToTokenInfos;
        } catch (Exception e) {
            System.err.println("Error retrieving docIds: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>(); // Return empty list on error
        }
    }


    public Map<String , WebDocument> getDocumentsByIds(Set<String> docIds) {
        Map<String , WebDocument> docs = new HashMap<>();

        List<ObjectId> objectIds = docIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        for (Document doc : collection.find(Filters.in("_id", objectIds))) {
            String id = doc.getObjectId("_id").toString();
            String url = doc.getString("url");
            String title = doc.getString("title");
            String content = doc.getString("content");

            WebDocument webDoc = new WebDocument(id, url, title, content);
            docs.put(id , webDoc);
        }

        return docs;
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
        ArrayList<String> ids = new ArrayList<>(Arrays.asList("6803adf92a5a8a19e97290b7", "6803adf92a5a8a19e97290b8"));
        DBM.markAsIndexed(ids);
        DBM.close();
    }
}
