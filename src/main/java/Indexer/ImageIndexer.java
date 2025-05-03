package Indexer;

import ImageSearching.Image;
import ImageSearching.ImageFeatureExtractor;
import Utils.Posting;
import Utils.Tokenizer;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;

import java.util.*;
import java.util.concurrent.*;

public class ImageIndexer implements IndexerInterface {
    // Keeping static variables as in original code
    private static ConcurrentHashMap<String, WebDocument> indexedDocuments;
    private static ConcurrentHashMap<String, WebDocument> unindexedDocs;
    private static ConcurrentLinkedQueue<Image> imageQueue;
    private final dbManager dbManager;
    private final ImageFeatureExtractor imageFeatureExtractor;
    private static final int numThreads = 4;
    private static final int batchSize = 100;

    // Added constants for image optimization
    private static final int MAX_IMAGES_PER_DOC = 10;
    private static final int IMAGE_SAVE_BATCH_SIZE = 100;

    public ImageIndexer() throws Exception {
        indexedDocuments = new ConcurrentHashMap<>();
        dbManager = new dbManager();
        unindexedDocs = dbManager.getNonIndexedDocuments(batchSize, true);
        imageFeatureExtractor = new ImageFeatureExtractor();
        imageFeatureExtractor.init();

        imageQueue = new ConcurrentLinkedQueue<>();
    }

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
            // process images
            processDocuments();

            // Update database and clear collections
            saveDataAndPrepareNextBatch();

            // Check if we need to run another batch
            if (!unindexedDocs.isEmpty()) {
                System.out.println("Running another indexing batch.");
            }
        }

        System.out.println("Indexing completed.");
    }

    private void processDocuments() throws InterruptedException {
        System.out.println("Processing images...");
        // Use fewer threads for image processing as requested
        ExecutorService imageExecutor = Executors.newFixedThreadPool(numThreads);

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
            // Save any remaining processed images
            System.out.println("Saving remaining processed images...");
            List<Image> imgsList = new ArrayList<>();
            while (!imageQueue.isEmpty()) {
                imgsList.add(imageQueue.poll());
            }

            if (!imgsList.isEmpty()) {
               dbManager.saveImages(imgsList);
            }

            // Mark documents as indexed
            ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
            dbManager.markAsIndexed(indexedIds, true);
            System.out.println("Indexing batch completed.");
            indexedDocuments.clear();

            // Get next batch of unindexed documents
            unindexedDocs.clear();
            unindexedDocs = dbManager.getNonIndexedDocuments(batchSize, true);
        } catch (Exception e) {
            System.err.println("Error persisting data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ImageIndexer indexer = new ImageIndexer();
        indexer.runIndexer();
    }
}