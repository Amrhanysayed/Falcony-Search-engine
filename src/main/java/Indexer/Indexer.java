package Indexer;

import ImageSearching.Image;
import ImageSearching.ImageFeatureExtractor;
import Utils.Utils;
import Utils.Tokenizer;
import Utils.WebDocument;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import Utils.Posting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.*;

public class Indexer {
    // Keeping static variables as in original code
    private static ConcurrentHashMap<String, List<Posting>> invertedIndex;
    private static ConcurrentHashMap<String, WebDocument> indexedDocuments;
    private static ConcurrentHashMap<String, WebDocument> unindexedDocs;
    private static ConcurrentLinkedQueue<Image> imageQueue;
    private static Tokenizer tokenizer;
    private final dbManager dbManager;
    private final ImageFeatureExtractor imageFeatureExtractor;
    private static final int numThreads = 9;
    private static final int batchSize = 200;

    // Added constants for image optimization
    private static final int MAX_IMAGES_PER_DOC = 10;
    private static final int IMAGE_SAVE_BATCH_SIZE = 100;

    public Indexer() throws Exception {
        invertedIndex = new ConcurrentHashMap<>();
        indexedDocuments = new ConcurrentHashMap<>();
        tokenizer = new Tokenizer();
        dbManager = new dbManager();
        unindexedDocs = dbManager.getNonIndexedDocuments(batchSize);
        imageFeatureExtractor = new ImageFeatureExtractor();
        imageFeatureExtractor.init();

        imageQueue = new ConcurrentLinkedQueue<>();
    }

    public static void indexDocument(WebDocument document, TokenizerME tokenizer) {
        indexedDocuments.put(document.getId(), document);
        String soup = document.getSoupedContent();
        // TODO Remove
        String[] tokens = tokenizer.tokenize(soup);


        Map<String, Integer> termFreq = new HashMap<>();

        PorterStemmer stemmer = new PorterStemmer();

        Map<String, List<Integer>> positions = new HashMap<>();
        int lastPosition = 0;
        for (String token : tokens) {
            // Clean token using regex
            // TODO: check this regex
            String cleaned = Utils.CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            if (cleaned.isEmpty() || Utils.STOP_WORDS.contains(cleaned)) {
                continue;
            }

            // Stem token
            String stemmed = stemmer.stem(cleaned);

            termFreq.put(stemmed, termFreq.getOrDefault(stemmed, 0) + 1);
            positions.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(lastPosition++);
        }

        // Update inverted index
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            int freq = entry.getValue();
            Posting tInfo = new Posting(term, freq, document.getId(), positions.computeIfAbsent(term, k -> new ArrayList<>()));
            invertedIndex.computeIfAbsent(term, k -> new ArrayList<>());
            boolean dup = false;
            for (Posting tokenInfo : invertedIndex.get(term)) {
                if(tokenInfo.getDocId().equals(document.getId())) {
                    tokenInfo.setTokenInfo(tInfo);
                    dup = true;
                    break;
                }
            }
            if(!dup) {
                invertedIndex.get(term).add(tInfo);
            }
        }
    }

    // Modified image processing method with size and count limits
    public static void processImagesInDocument(WebDocument document, ImageFeatureExtractor imageFeatureExtractor) {
        List<String> images = document.getImages();
        int processedCount = 0;

        for (String imageUrl : images) {
            if (imageUrl.isEmpty()) continue;

            // Limit to MAX_IMAGES_PER_DOC per document
            if (processedCount >= MAX_IMAGES_PER_DOC) {
                System.out.println("Reached max image limit (" + MAX_IMAGES_PER_DOC + ") for document: " + document.getUrl());
                break;
            }

            try {
                System.out.println("Processing image: " + imageUrl);
                byte[] imageBytes = Utils.downloadImage(imageUrl);

                float[] features = imageFeatureExtractor.extractFeatures(imageBytes);
                Image image = new Image();
                image.setFeatures(features);
                image.setUrl(imageUrl);
                image.setDocUrl(document.getUrl());
                image.setId(UUID.randomUUID().toString());
                imageQueue.add(image);
                processedCount++;

                // Save images periodically to prevent memory build-up
                if (imageQueue.size() >= IMAGE_SAVE_BATCH_SIZE) {
                    saveImageQueueBatch();
                }

                System.out.println("✅ Image saved: " + imageUrl);
            } catch (Exception e) {
                System.err.println("❌ Error indexing image: " + imageUrl + " | " + e.getMessage());
            }
        }
    }

    // New helper method to save images in batches
    private static void saveImageQueueBatch() {
        if (imageQueue.isEmpty()) return;

        List<Image> imgsBatch = new ArrayList<>(IMAGE_SAVE_BATCH_SIZE);
        int count = 0;

        while (!imageQueue.isEmpty() && count < IMAGE_SAVE_BATCH_SIZE) {
            imgsBatch.add(imageQueue.poll());
            count++;
        }

        if (!imgsBatch.isEmpty()) {
            try {
                System.out.println("📦 Saving intermediate batch of " + imgsBatch.size() + " images");
                // Assuming this is a static reference to dbManager, or you can pass the instance
                new dbManager().saveImages(imgsBatch);
                System.out.println("✅ Intermediate image batch saved");
            } catch (Exception e) {
                System.err.println("❌ Failed to save intermediate image batch: " + e.getMessage());
                // Put images back in queue if save fails
                imageQueue.addAll(imgsBatch);
            }
        }
    }

    public void runIndexer() throws Exception {
        System.out.println("Starting indexer...");

        while (!unindexedDocs.isEmpty()) {
            // Process text content first
            processTextInDocuments();

            // Then process images after text processing is complete
            processImagesInDocuments();

            // Update database and clear collections
            saveDataAndPrepareNextBatch();

            // Check if we need to run another batch
            if (!unindexedDocs.isEmpty()) {
                System.out.println("Running another indexing batch.");
            }
        }

        System.out.println("Indexing completed.");
    }

    private void processTextInDocuments() throws InterruptedException {
        System.out.println("Processing text content...");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try {
            // Submit text processing tasks
            for (WebDocument doc : unindexedDocs.values()) {
                executor.submit(new IndexerWorker(doc, tokenizer));
            }

            // Initiate shutdown and wait for completion
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                System.err.println("Text indexing timed out, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (Exception e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw e;
        }

        System.out.println("Text processing completed.");
    }

    private void processImagesInDocuments() throws InterruptedException {
        System.out.println("Processing images...");
        // Use fewer threads for image processing as requested
        ExecutorService imageExecutor = Executors.newFixedThreadPool(numThreads / 2);

        try {
            // Submit image processing tasks
            for (WebDocument doc : unindexedDocs.values()) {
                imageExecutor.submit(new ImageIndexerWorker(doc, imageFeatureExtractor));
            }

            // Initiate shutdown and wait for completion
            imageExecutor.shutdown();
            if (!imageExecutor.awaitTermination(10, TimeUnit.MINUTES)) {
                System.err.println("Image indexing timed out, forcing shutdown");
                imageExecutor.shutdownNow();
            }
        } catch (Exception e) {
            imageExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            throw e;
        }

        System.out.println("Image processing completed.");
    }

    private void saveDataAndPrepareNextBatch() {
        try {
            // Mark documents as indexed
            ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
            dbManager.markAsIndexed(indexedIds);
            System.out.println("Indexing completed.");
            indexedDocuments.clear();

            // Insert tokens into database
            System.out.println("Updating tokens in database...");
            dbManager.insertTokens(invertedIndex);
            invertedIndex.clear();

            // Save any remaining processed images
            System.out.println("Saving remaining processed images...");
            List<Image> imgsList = new ArrayList<>();
            while (!imageQueue.isEmpty()) {
                imgsList.add(imageQueue.poll());
            }

            if (!imgsList.isEmpty()) {
               // dbManager.saveImages(imgsList);
            }

            // Get next batch of unindexed documents
            unindexedDocs.clear();
            unindexedDocs = dbManager.getNonIndexedDocuments(batchSize);
        } catch (Exception e) {
            System.err.println("Error persisting data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // For debugging
    public void printIndex() {
        for (Map.Entry<String, List<Posting>> entry : invertedIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey());
            for (Posting p : entry.getValue()) {
                System.out.println("  DocID: " + p.docId + ", Freq: " + p.getFrequency());
                System.out.println("  Pos: " + p.getPositions());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Indexer indexer = new Indexer();
        indexer.runIndexer();
    }
}