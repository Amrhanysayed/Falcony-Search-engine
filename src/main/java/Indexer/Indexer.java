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
    private static ConcurrentHashMap<String, List<Posting>> invertedIndex;
    private static ConcurrentHashMap<String, WebDocument> indexedDocuments;
    private static ConcurrentHashMap<String, WebDocument> unindexedDocs;
    private static ConcurrentLinkedQueue<Image> imageQueue;
    private static Tokenizer tokenizer;
    private ExecutorService executor, imageExecutor;
    private dbManager dbManager;
    private ImageFeatureExtractor imageFeatureExtractor;
    private final int numThreads = 8;

    public Indexer() throws Exception {
        invertedIndex = new ConcurrentHashMap<>();
        indexedDocuments = new ConcurrentHashMap<>();
        tokenizer = new Tokenizer();
        dbManager = new dbManager();
        unindexedDocs = dbManager.getNonIndexedDocuments(100);
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
                if(tokenInfo.getDocId() == document.getId()) {
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

    public static void processImagesInDocument(WebDocument document, ImageFeatureExtractor imageFeatureExtractor) {
        List<String> images = document.getImages();
        for (String imageUrl : images) {
            if (imageUrl.isEmpty()) continue;

            try {
                System.out.println("Processing image: " + imageUrl);
                byte[] imageBytes = Utils.downloadImage(imageUrl); // implement this
                float[] features = imageFeatureExtractor.extractFeatures(imageBytes);
                Image image = new Image();
                image.setFeatures(features);
                image.setUrl(imageUrl);
                image.setDocUrl(document.getUrl());
                image.setId(UUID.randomUUID().toString());
                imageQueue.add(image);
                System.out.println("✅ Image saved: " + imageUrl);
            } catch (Exception e) {
                System.err.println("❌ Error indexing image: " + imageUrl + " | " + e.getMessage());
            }
        }

    }

    public void runIndexer() throws Exception {
        System.out.println("Starting indexer...");
        executor = Executors.newFixedThreadPool(numThreads);
        imageExecutor = Executors.newFixedThreadPool(3);
        try {
            while (!unindexedDocs.isEmpty()) {
                for (WebDocument doc : unindexedDocs.values()) {
                    executor.submit(new IndexerWorker(doc, tokenizer));
                    imageExecutor.submit(new ImageIndexerWorker(doc, imageFeatureExtractor));
                }
                unindexedDocs.clear();
            }
        } finally {
            // Shutdown executor and wait for tasks to complete
            executor.shutdown();
            imageExecutor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't finish
                }
                if(!imageExecutor.awaitTermination(10, TimeUnit.MINUTES)) {
                    imageExecutor.shutdownNow();
                }
                ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
                dbManager.markAsIndexed(indexedIds);
                indexedDocuments.clear();

                unindexedDocs = dbManager.getNonIndexedDocuments(100);

                System.out.println("Indexing batch completed, updating tokens.");
                dbManager.insertTokens(invertedIndex);
                System.out.println("Tokens indexed.");

                invertedIndex.clear();

                try {
                    List<Image> imgsList = new ArrayList<>();
                    while (!imageQueue.isEmpty()) {
                        imgsList.add(imageQueue.poll());
                    }

                    dbManager.saveImages(imgsList);

                    imageQueue.clear();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                if (!unindexedDocs.isEmpty()) {
                    System.out.println("Running another indexing batch.");
                    runIndexer();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                imageExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
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
        // mehlbya
        indexer.runIndexer();
        //indexer.printIndex();
    }
}
