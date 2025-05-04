package dbManager;

import Backend.Image;
import Utils.Posting;
import Utils.WebDocument;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteError;
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
    private static final String IMAGES_CONNECTION_STRING = dotenv.get("MONGO_IMAGES_URL");

    private static final String DB_NAME = "search_engine";
    private static final String COLLECTION_NAME = "documents";

    private final MongoClient mongoClient;
    private final MongoClient imagesMongoClient;
    private final MongoCollection<Document> tokensCollection;  // Renamed for proper casing
    private MongoDatabase database;
    private MongoDatabase imagesDatabase;
    private final MongoCollection<Document> docsCollections;
    private final MongoCollection<Document> crawlerStateCollection;
    private final MongoCollection<Document> imageCollection;

    private static final int BULK_WRITE_BATCH_SIZE = 1000;

    public dbManager() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        imagesMongoClient = MongoClients.create(IMAGES_CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);
        docsCollections = database.getCollection(COLLECTION_NAME);
        tokensCollection = database.getCollection("tokens");  // Renamed for proper casing

        imagesDatabase = imagesMongoClient.getDatabase(DB_NAME);
        imageCollection = imagesDatabase.getCollection("images");


        crawlerStateCollection= database.getCollection("crawler_state");
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

        docsCollections.insertOne(doc);
        System.out.println("Document inserted: " + title);
    }

    public void insertDocuments(List<Document> documents) {
        try {
            if (!documents.isEmpty()) {
                docsCollections.insertMany(documents);
                System.out.println("Inserted " + documents.size() + " documents");
            }
        } catch (Exception e) {
            System.err.println("Failed to insert documents: " + e.getMessage());
        }
    }

    public void saveCrawlerState(Queue<String> urlsToCrawl, Set<String> visited, int pageCount) {
        try {
            Document stateDoc = new Document("_id", "crawler_state")
                    .append("urlsToCrawl", new ArrayList<>(urlsToCrawl))
                    .append("visited", new ArrayList<>(visited))
                    .append("pageCount", pageCount)
                    .append("timestamp", System.currentTimeMillis());

            crawlerStateCollection.replaceOne(
                    Filters.eq("_id", "crawler_state"),
                    stateDoc,
                    new ReplaceOptions().upsert(true)
            );
            System.out.println("Saved crawler state: " + pageCount + " pages, " +
                    urlsToCrawl.size() + " URLs to crawl, " + visited.size() + " visited");
        } catch (Exception e) {
            System.err.println("Failed to save crawler state: " + e.getMessage());
        }
    }

    public Map<String, Object> loadCrawlerState() {
        try {
            Document stateDoc = crawlerStateCollection.find(Filters.eq("_id", "crawler_state")).first();
            if (stateDoc == null) {
                System.out.println("No crawler state found, starting fresh");
                return null;
            }
            Map<String, Object> state = new HashMap<>();
            state.put("urlsToCrawl", stateDoc.getList("urlsToCrawl", String.class, new ArrayList<>()));
            state.put("visited", stateDoc.getList("visited", String.class, new ArrayList<>()));
            state.put("pageCount", stateDoc.getInteger("pageCount", 0));
            System.out.println("Loaded crawler state: " + state.get("pageCount") + " pages, " +
                    ((List<?>) state.get("urlsToCrawl")).size() + " URLs to crawl, " +
                    ((List<?>) state.get("visited")).size() + " visited");
            return state;
        } catch (Exception e) {
            System.err.println("Failed to load crawler state: " + e.getMessage());

            return null;
        }
    }



    // Search documents by keyword
    public void searchByKeyword(String keyword) {
        Pattern pattern = Pattern.compile(keyword, Pattern.CASE_INSENSITIVE);
        FindIterable<Document> results = docsCollections.find(
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
    public ConcurrentHashMap<String, WebDocument> getNonIndexedDocuments(int limit, boolean isImages) {
        ConcurrentHashMap<String, WebDocument> docs = new ConcurrentHashMap<>();
        String flag_filter = isImages ? "images_indexed" : "indexed";

        // Create the projection to include/exclude fields
        Document projection = new Document()
                .append("_id", 1)
                .append("url", 1)
                .append("title", 1)
                .append("content", 1);

        // Only include the images field if isImages is true
        if (isImages) {
            projection.append("images", 1);
        }

        FindIterable<Document> results = docsCollections.find(
                        Filters.eq(flag_filter, false)
                )
                .projection(projection)
                .limit(limit);

        for (Document doc : results) {
            String url = doc.getString("url");
            String title = doc.getString("title");
            String content = doc.getString("content");
            String id = doc.getObjectId("_id").toString();
            // Get images only if isImages is true; otherwise, use null or empty list
            List<String> images = isImages ? doc.getList("images", String.class) : null;

            WebDocument webDoc = new WebDocument(id, url, title, content, images);
            docs.put(id, webDoc);
        }

        return docs;
    }

    // Mark a document as indexed
    public void markAsIndexed(List<String> ids, boolean isImages) {
        List<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        String flag_filter = isImages ? "images_indexed" : "indexed";
        System.out.println("Marking indexed " + ids.size() + " documents as indexed");
        System.out.println(ids);

        Document filter = new Document("_id", new Document("$in", objectIds));
        Document update = new Document("$set", new Document(flag_filter, true));
        docsCollections.updateMany(filter, update);
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
                    Map<String, Integer> positions = posting.getFrequencies(); // Get the map of positions by section
                    ObjectId id = new ObjectId(docId);

                    // Create document info with the positions map instead of frequency and positions list
                    Document docInfo = new Document("docId", id)
                            .append("positions", new Document(positions));

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
                int totalUpdates = bulkUpdates.size();
                System.out.println("Preparing to write " + totalUpdates + " token updates");

                for (int i = 0; i < totalUpdates; i += BULK_WRITE_BATCH_SIZE) {
                    int endIndex = Math.min(i + BULK_WRITE_BATCH_SIZE, totalUpdates);
                    List<UpdateOneModel<Document>> batch = bulkUpdates.subList(i, endIndex);
                    try {
                        tokensCollection.bulkWrite(batch, new BulkWriteOptions().ordered(false));
                        System.out.println("Bulk write completed for batch " + (i / BULK_WRITE_BATCH_SIZE + 1) +
                                " (" + batch.size() + " updates)");
                    } catch (com.mongodb.MongoException e) {
                        System.err.println("Failed to write batch " + (i / BULK_WRITE_BATCH_SIZE + 1) +
                                ": " + e.getMessage());
                        throw e;
                    }
                }
            } else {
                System.out.println("No token updates to perform.");
            }
        } catch (Exception e) {
            System.err.println("Error during bulk write: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
                            Document positionsDoc = docInfo.get("positions", Document.class);
                            Map<String, Integer> freqs = new HashMap<>();
                            if (positionsDoc != null) {
                                for (String key : positionsDoc.keySet()) {
                                    Object value = positionsDoc.get(key);
                                    if (value instanceof Integer) {
                                        freqs.put(key, (Integer) value);
                                    }
                                }
                            }
                            Posting posting = new Posting(token, docId, freqs);
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

    public int getTotalDocCount() {
        long count = docsCollections.countDocuments();
        return (int) count;
    }

    public Set<String> FilterDocsIdsByPhrase(Set<String> docIds , String phrase) {

        Set<String> filteredDocIds = new HashSet<>();

        List<ObjectId> objectIds = docIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        for (Document doc : docsCollections.find(
                Filters.and(Filters.in("_id", objectIds),
                        Filters.regex("content", phrase, "i"))).projection(Projections.include("_id")))
        {
            String id = doc.getObjectId("_id").toString();
            filteredDocIds.add(id);
        }

        return filteredDocIds;
    }


    public Map<String , WebDocument> getDocumentsByIds(Set<String> docIds) {
        Map<String , WebDocument> docs = new HashMap<>();

        List<ObjectId> objectIds = docIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toList());

        System.out.println("WHERE IS MY CANDODO");
        for (Document doc : docsCollections.find(Filters.in("_id", objectIds)).projection(
                Projections.include("_id", "url" , "title", "popularity"))) {
            String id = doc.getObjectId("_id").toString();
            String url = doc.getString("url");
            String title = doc.getString("title");
            double popularity = doc.getDouble("popularity");

            WebDocument webDoc = new WebDocument(id, url, title, "" , popularity);
            docs.put(id , webDoc);
        }

        return docs;
    }

    public List<Document> getDocumentsForGraphBuilder() {
        Document projection = new Document("_id", 1).append("url", 1).append("links", 1);
        return docsCollections.find().projection(projection).into(new ArrayList<>());
    }
    public void savePageRanks(Map<String, Double> pageRanks) {
//        System.out.println("[PageRankCalculator] Saving PageRanks to database...");
        double totalSum = pageRanks.values().stream().mapToDouble(Double::doubleValue).sum();
        pageRanks.replaceAll((id, rank) -> rank / totalSum);

        List<WriteModel<Document>> bulkUpdates = new ArrayList<>();

        for (Map.Entry<String, Double> entry : pageRanks.entrySet()) {
            String pageId = entry.getKey();
            Double pageRank = entry.getValue();
            bulkUpdates.add(new UpdateOneModel<>(
                    new Document("_id", new ObjectId(pageId)),
                    new Document("$set", new Document("popularity", pageRank))
            ));
        }

        if (!bulkUpdates.isEmpty()) {
            try {
                docsCollections.bulkWrite(bulkUpdates);
            } catch (MongoBulkWriteException e) {
//                System.err.println("[PageRankCalculator] Error during bulk write: " + e.getMessage());
            }
        }

//        System.out.println("[PageRankCalculator] PageRanks saved successfully.");
    }

    public void saveImages(List<Image> images) {
        if (images.isEmpty()) {
            System.out.println("No images to process.");
            return;
        }

        // Extract unique URLs from incoming images
        Map<String, Image> uniqueIncomingImages = new HashMap<>();
        for (Image img : images) {
            // Keep only the first occurrence of each URL
            uniqueIncomingImages.putIfAbsent(img.getUrl(), img);
        }

        if (uniqueIncomingImages.isEmpty()) {
            System.out.println("⚠️ No unique images to process after deduplication.");
            return;
        }

        // Check against database in a single query
        Set<String> uniqueUrls = uniqueIncomingImages.keySet();
        Set<String> existingUrls = new HashSet<>();

        // Fetch only the URL field for efficiency
        imageCollection.find(Filters.in("url", new ArrayList<>(uniqueUrls)))
                .projection(Projections.include("url"))
                .forEach(doc -> existingUrls.add(doc.getString("url")));

        // Prepare documents for non-duplicates
        List<Document> toInsert = new ArrayList<>();
        int skippedCount = 0;

        for (Map.Entry<String, Image> entry : uniqueIncomingImages.entrySet()) {
            String url = entry.getKey();
            if (existingUrls.contains(url)) {
                skippedCount++;
                continue;
            }

            Image img = entry.getValue();
            Document imageDoc = createImageDocument(img);
            toInsert.add(imageDoc);
        }

        // Reporting
        System.out.println("Total images received: " + images.size());
        System.out.println("Unique incoming images: " + uniqueIncomingImages.size());
        System.out.println("⚠️ Skipped " + skippedCount + " duplicates from database.");

        // Bulk insert if any remain
        if (!toInsert.isEmpty()) {
            try {
                imageCollection.insertMany(
                        toInsert,
                        new InsertManyOptions().ordered(false)
                );
                System.out.println("✅ Successfully inserted " + toInsert.size() + " new images.");
            } catch (MongoBulkWriteException e) {
                int successCount = e.getWriteResult().getInsertedCount();
                System.out.println("✅ Partially successful: inserted " + successCount + " images.");
                System.err.println("❌ Failed to insert " + e.getWriteErrors().size() + " images.");

                // Log only the first few errors to avoid overwhelming logs
                int errorLimit = Math.min(e.getWriteErrors().size(), 5);
                for (int i = 0; i < errorLimit; i++) {
                    BulkWriteError err = e.getWriteErrors().get(i);
                    System.err.println("  - Error at index " + err.getIndex() + ": " + err.getMessage());
                }
                if (errorLimit < e.getWriteErrors().size()) {
                    System.err.println("  - (+" + (e.getWriteErrors().size() - errorLimit) + " more errors)");
                }
            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
            }
        } else {
            System.out.println("⚠️ No new images to insert after deduplication.");
        }
    }

    private Document createImageDocument(Image img) {
        // Convert float[] features to List<Double> for MongoDB
        List<Double> featureList = new ArrayList<>(img.getFeatures().length);
        for (float f : img.getFeatures()) {
            featureList.add((double) f);
        }

        return new Document()
                .append("_id", img.getId())
                .append("url", img.getUrl())
                .append("docUrl", img.getDocUrl())
                .append("features", featureList)
                .append("_class", img.getClass().getName());
    }


    // Close the database connection
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }

}
