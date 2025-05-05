package Indexer;

import Backend.Image;
import ImageSearching.ImageFeatureExtractor;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;

import java.util.*;
import java.util.concurrent.*;

public class ImageIndexer implements IndexerInterface {
    private final Map<String, WebDocument> indexedDocuments;
    private Map<String, WebDocument> unindexedDocs;
    private final List<Image> imageList;
    private final Set<String> imageUrls;
    private final dbManager dbManager = new dbManager();
    private final ImageFeatureExtractor imageFeatureExtractor;
    private final int batchSize = 10;
    private final int MAX_IMAGES_PER_DOC = 16;

    private final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

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
            if (imageUrl.endsWith(".gif") || imageUrl.endsWith(".svg") || imageUrl.endsWith(".webp")) continue;
            if (imageUrls.contains(imageUrl)) continue;

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
                synchronized (imageList) {
                    imageList.add(image);
                }
                processedCount++;
                synchronized (imageUrls) {
                    imageUrls.add(imageUrl);
                }
                System.out.println("Image saved: " + imageUrl);
            } catch (Exception e) {
                System.err.println("Error indexing image: " + imageUrl + " | " + e.getMessage());
            }
        }
        synchronized (indexedDocuments) {
            indexedDocuments.put(document.getId(), document);
        }
    }

    public void runIndexer() throws Exception {
        System.out.println("Starting indexer...");

        while (!unindexedDocs.isEmpty()) {
            processDocuments();
            saveDataAndPrepareNextBatch();

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

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (WebDocument doc : unindexedDocs.values()) {
                Callable<Void> task = () -> {
                    processImagesInDocument(doc);
                    return null;
                };
                futures.add(executor.submit(task));
            }

            // Wait for all tasks to complete and handle exceptions
            for (Future<Void> future : futures) {
                try {
                    future.get(); // Blocks until the task completes
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Image processing interrupted: " + e.getMessage());
                    throw new RuntimeException("Image processing interrupted", e);
                } catch (ExecutionException e) {
                    System.err.println("Error processing document: " + e.getCause());
                    throw new RuntimeException("Error processing document", e.getCause());
                }
            }
        } finally {
            // Always shutdown the executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Image processing completed.");
    }

    private void saveDataAndPrepareNextBatch() {
        try {
            System.out.println("Saving remaining processed images...");
            if (!imageList.isEmpty()) {
                dbManager.saveImages(imageList);
            }

            ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
            dbManager.markAsIndexed(indexedIds, true);
            System.out.println("Indexing batch completed.");
            indexedDocuments.clear();
            imageList.clear();

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