package Indexer;

import Utils.Utils;
import Utils.Tokenizer;
import Utils.WebDocument;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import Utils.Posting;

import java.util.*;
import java.util.concurrent.*;

public class TextIndexer implements IndexerInterface {
    // Keeping static variables as in original code
    private static ConcurrentHashMap<String, List<Posting>> invertedIndex;
    private static ConcurrentHashMap<String, WebDocument> indexedDocuments;
    private static ConcurrentHashMap<String, WebDocument> unindexedDocs;
    private static Tokenizer tokenizer;
    private final dbManager dbManager;
    private static final int numThreads = 9;
    private static final int batchSize = 200;

    public TextIndexer() throws Exception {
        invertedIndex = new ConcurrentHashMap<>();
        indexedDocuments = new ConcurrentHashMap<>();
        tokenizer = new Tokenizer();
        dbManager = new dbManager();
        unindexedDocs = dbManager.getNonIndexedDocuments(batchSize, false);
    }

    public static void indexDocument(WebDocument document, TokenizerME tokenizer) {
        indexedDocuments.put(document.getId(), document);
        String soup = document.getSoupedContent();
        String title = document.getTitle();
        List<String> H1s = document.getH1s();
        List<String> H2s = document.getH2s();
        // TODO Remove
        String[] tokens = tokenizer.tokenize(soup);

        // Initialize stemmer
        PorterStemmer stemmer = new PorterStemmer();

        // Process each component to get stemmed tokens
        List<String> titleStemmedTokens = processText(title, tokenizer, stemmer);
        List<String> h1StemmedTokens = processTextList(H1s, tokenizer, stemmer);
        List<String> h2StemmedTokens = processTextList(H2s, tokenizer, stemmer);


        Map<String, Map<String, Integer>> freqs = new HashMap<>();
        int count = 0;
        for (String token : tokens) {
            // Clean token using regex
            // TODO: check this regex
            String cleaned = Utils.CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            if (cleaned.isEmpty() || Utils.STOP_WORDS.contains(cleaned)) {
                continue;
            }

            // Stem token
            String stemmed = stemmer.stem(cleaned);

            Map<String, Integer> postingFreqs = freqs.computeIfAbsent(stemmed, k -> new HashMap<>());

            if(!postingFreqs.containsKey("title")) { // to only count it one time
                int titleCount = Collections.frequency(titleStemmedTokens, stemmed);
                int h1Count = Collections.frequency(h1StemmedTokens, stemmed);
                int h2Count = Collections.frequency(h2StemmedTokens, stemmed);
                postingFreqs.putIfAbsent("title", titleCount);
                postingFreqs.putIfAbsent("h1", h1Count);
                postingFreqs.putIfAbsent("h2", h2Count);
                postingFreqs.put("body", postingFreqs.getOrDefault("body", 0) - h1Count - h2Count);
            }
            postingFreqs.put("body", postingFreqs.getOrDefault("body", 0) + 1);
        }

        // Update inverted index
        for (Map.Entry<String, Map<String, Integer>> entry : freqs.entrySet()) {
            String term = entry.getKey();
            Map<String, Integer> postingFreqs = entry.getValue();
            Posting tInfo = new Posting(term, document.getId(), postingFreqs);
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

    // Helper method to process a single text string
    private static List<String> processText(String text, TokenizerME tokenizer, PorterStemmer stemmer) {
        List<String> stemmedTokens = new ArrayList<>();
        if (text != null && !text.isEmpty()) {
            String[] tokens = tokenizer.tokenize(text);
            for (String token : tokens) {
                // Clean token
                String cleaned = Utils.CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
                if (!cleaned.isEmpty() && !Utils.STOP_WORDS.contains(cleaned)) {
                    // Stem token and add to list
                    String stemmed = stemmer.stem(cleaned);
                    stemmedTokens.add(stemmed);
                }
            }
        }
        return stemmedTokens;
    }

    // Helper method to process a list of text strings
    private static List<String> processTextList(List<String> textList, TokenizerME tokenizer, PorterStemmer stemmer) {
        List<String> stemmedTokens = new ArrayList<>();
        for (String text : textList) {
            stemmedTokens.addAll(processText(text, tokenizer, stemmer));
        }
        return stemmedTokens;
    }

    public void runIndexer() throws Exception {
        System.out.println("Starting indexer...");

        while (!unindexedDocs.isEmpty()) {
            // Process text content
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

    private void saveDataAndPrepareNextBatch() {
        try {
            // Insert tokens into database
            System.out.println("Updating tokens in database...");
            dbManager.insertTokens(invertedIndex);
            invertedIndex.clear();

            // Mark documents as indexed
            ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
            dbManager.markAsIndexed(indexedIds, false);
            System.out.println("Indexing batch completed.");
            indexedDocuments.clear();

            // Get next batch of unindexed documents
            unindexedDocs.clear();
            unindexedDocs = dbManager.getNonIndexedDocuments(batchSize, false);
        } catch (Exception e) {
            System.err.println("Error persisting data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        TextIndexer textIndexer = new TextIndexer();
        textIndexer.runIndexer();
    }
}