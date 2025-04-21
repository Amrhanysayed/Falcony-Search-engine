package Indexer;

import Utils.WebDocument;
import Utils.StopWords;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import org.jsoup.Jsoup;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Indexer {
    private static ConcurrentHashMap<String, List<TermInfo>> invertedIndex;
    private static ConcurrentHashMap<String, WebDocument> indexedDocuments;
    private static ConcurrentHashMap<String, WebDocument> unindexedDocs;
    private static Tokenizer tokenizer;
    private ExecutorService executor;
    private dbManager dbManager;
    private final int numThreads = 10;

    private static final Set<String> STOP_WORDS = StopWords.getStopWords();
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]");
    public Indexer() throws Exception {
        invertedIndex = new ConcurrentHashMap<>();
        indexedDocuments = new ConcurrentHashMap<>();
        tokenizer = new Tokenizer();
        dbManager = new dbManager();
        unindexedDocs = dbManager.getNonIndexedDocuments();
    }

    public static void indexDocument(WebDocument document, TokenizerME tokenizer) {
        indexedDocuments.put(document.getId(), document);
        String soup = Jsoup.parse(document.content).text().replaceAll("\\s+", " ").trim();
        String[] tokens = tokenizer.tokenize(soup);


        Map<String, Integer> termFreq = new HashMap<>();

        PorterStemmer stemmer = new PorterStemmer();

        Map<String, List<Integer>> positions = new HashMap<>();
        int lastPosition = 0;
        for (String token : tokens) {
            // Clean token using regex
            // TODO: check this regex
            String cleaned = CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            if (cleaned.isEmpty() || STOP_WORDS.contains(cleaned)) {
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
            TermInfo tInfo = new TermInfo(term, freq, document.getId(), positions.computeIfAbsent(term, k -> new ArrayList<>()));
            invertedIndex.computeIfAbsent(term, k -> new ArrayList<>());
            boolean dup = false;
            for (TermInfo termInfo : invertedIndex.get(term)) {
                if(termInfo.getDocId() == document.getId()) {
                    termInfo.setTermInfo(tInfo);
                    dup = true;
                    break;
                }
            }
            if(!dup) {
                invertedIndex.get(term).add(tInfo);
            }
        }
    }

    public void runIndexer() throws Exception {
        System.out.println("Starting indexer...");
        executor = Executors.newFixedThreadPool(numThreads);
        try {
            while (!unindexedDocs.isEmpty()) {
                for (WebDocument doc : unindexedDocs.values()) {
                    executor.submit(new IndexerWorker(doc, tokenizer));
                }
                unindexedDocs.clear();
            }
        } finally {
            // Shutdown executor and wait for tasks to complete
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't finish
                }
                ArrayList<String> indexedIds = new ArrayList<>(indexedDocuments.keySet());
                dbManager.markAsIndexed(indexedIds);
                indexedDocuments.clear();

                unindexedDocs = dbManager.getNonIndexedDocuments();

                System.out.println("Indexing batch completed, updating tokens.");
                dbManager.insertTokens(invertedIndex);
                System.out.println("Tokens indexed.");

                invertedIndex.clear();

                if (!unindexedDocs.isEmpty()) {
                    System.out.println("Running another indexing batch.");
                    runIndexer();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // For debugging
    public void printIndex() {
        for (Map.Entry<String, List<TermInfo>> entry : invertedIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey());
            for (TermInfo p : entry.getValue()) {
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
