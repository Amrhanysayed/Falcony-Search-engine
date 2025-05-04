package Indexer;

import Backend.Image;
import ImageSearching.ImageFeatureExtractor;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;

import java.util.*;
import java.util.concurrent.*;

public class ImageIndexer implements IndexerInterface {
    // Keeping static variables as in original code
    private final Map<String, WebDocument> indexedDocuments;
    private Map<String, WebDocument> unindexedDocs;
    private final List<Image> imageList;
    private final Set<String> imageUrls;
    private final dbManager dbManager = new dbManager();
    private final ImageFeatureExtractor imageFeatureExtractor;
    private final int batchSize = 100;

    // Added constants for image optimization
    private final int MAX_IMAGES_PER_DOC = 16;

    public ImageIndexer() throws Exception {
        indexedDocuments = new HashMap<>();
        unindexedDocs = dbManager.getNonIndexedDocuments(batchSize, true);
        imageFeatureExtractor = new ImageFeatureExtractor();

        imageList = new ArrayList<>();
        imageUrls = new HashSet<>();
    }

    public void processImagesInDocument(WebDocument document) {
        List<String> images = document.getImages();
        int processedCount = 0;

        for (String imageUrl : images) {
            if (imageUrl.isEmpty()) continue;
            if(imageUrl.endsWith(".gif") || imageUrl.endsWith(".svg") || imageUrl.endsWith(".webp")) continue;
            if(imageUrls.contains(imageUrl)) continue;

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
                imageList.add(image);
                processedCount++;
                imageUrls.add(imageUrl);
                System.out.println("✅ Image saved: " + imageUrl);
            } catch (Exception e) {
                System.err.println("❌ Error indexing image: " + imageUrl + " | " + e.getMessage());
            }
        }
        indexedDocuments.put(document.getId(), document);
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

        imageList.clear();
        imageUrls.clear();

        System.out.println("Indexing completed.");
    }

    private void processDocuments() {
        System.out.println("Processing images...");
        // Use fewer threads for image processing as requested

        try {
            // Submit image processing tasks
            for (WebDocument doc : unindexedDocs.values()) {
                processImagesInDocument(doc);
            }

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw e;
        }

        System.out.println("Image processing completed.");
    }

    private void saveDataAndPrepareNextBatch() {
        try {
            // Save any remaining processed images
            System.out.println("Saving remaining processed images...");

            if (!imageList.isEmpty()) {
               dbManager.saveImages(imageList);
            }

            // Mark documents as indexed
            ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
            dbManager.markAsIndexed(indexedIds, true);
            System.out.println("Indexing batch completed.");
            indexedDocuments.clear();

            imageList.clear();

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