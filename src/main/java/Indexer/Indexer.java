package Indexer;

import Utils.Document;
import Utils.StopWords;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

public class Indexer {
    private Map<String, List<TermInfo>> invertedIndex;
    private Map<Integer, Document> documents;
    private TokenizerME tokinzer;

    private static final Set<String> STOP_WORDS = StopWords.getStopWords();
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]");
    public Indexer() throws Exception {
        invertedIndex = new HashMap<>();
        documents = new HashMap<>();

        try {
            InputStream modelInput = Indexer.class.getResourceAsStream("/en-token.bin");
            TokenizerModel model = new TokenizerModel(modelInput);
            tokinzer = new TokenizerME(model);
            modelInput.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Document parseDocument(int id, String url, String title, String content) throws Exception {
        String soup = Jsoup.parse(content).text();
        return new Document(id, url, title, soup);
    }

    public void indexDocument(Document document) {
        documents.put(document.getId(), document);

        String[] tokens = tokinzer.tokenize(document.content);

        Map<String, Integer> termFreq = new HashMap<>();
        int position = 0;
        PorterStemmer stemmer = new PorterStemmer();

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
        }

        // Update inverted index
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            int freq = entry.getValue();
            TermInfo tInfo = new TermInfo(term, freq, document.getId());
            invertedIndex.computeIfAbsent(term, k -> new ArrayList<>()).add(tInfo);
        }
    }

    // For debugging
    public void printIndex() {
        for (Map.Entry<String, List<TermInfo>> entry : invertedIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey());
            for (TermInfo p : entry.getValue()) {
                System.out.println("  DocID: " + p.docId + ", Freq: " + p.getFrequency());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Indexer indexer = new Indexer();
        Document d1 = new Document(1,
                "http://example.com",
                "Sample Page",
                "<html><body>This is a sample cost-wise document about Java programming document.</body></html>"),
                d2 = new Document(2,
                        "http://test.com",
                        "Test Page",
                        "<html><body>Java is great for building search engines.</body></html>"
                );

        indexer.indexDocument(d1);
        indexer.indexDocument(d2);
        indexer.printIndex();
    }
}
