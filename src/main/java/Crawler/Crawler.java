package Crawler;

import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;

import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Crawler {
    protected final ConcurrentLinkedQueue<String> urlsToCrawl = new ConcurrentLinkedQueue<>(); // Use Queue for BFS
    protected static final Set<String> visited = ConcurrentHashMap.<String>newKeySet();
    RobotsManager RobotsM;
    ExecutorService executor;
    int numThreads = 8;

    public Crawler() {

    }

    public void startCrawl(String fileName) throws Exception {

        this.RobotsM = new RobotsManager(); // Initialize RobotsManager
        this.executor = Executors.newFixedThreadPool(numThreads); // Initialize thread poo
        readStartLinks(fileName); // Read seed URLs from file

        crawl(); // Start crawling
    }

    protected static String normalizeUrl(String url, String baseUrl) throws Exception {

        // Step 1: Resolve relative URL (if needed)
        if (!url.startsWith("http")) {
            Document doc = Jsoup.parse("<a href=\"" + url + "\"></a>", baseUrl);
            url = doc.select("a").first().absUrl("href");
        }

        // Step 2: Parse and normalize path
        URI uri = new URI(url).normalize();

        // Step 3: Lowercase scheme and host
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();

        // Step 4: Remove default ports
        int port = uri.getPort();
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = -1;
        }

        // Step 5: Handle query parameters (example: remove "session")
        String query = uri.getQuery();
        String newQuery = query;
        if (query != null) {
            Map<String, String> params = Arrays.stream(query.split("&"))
                    .map(p -> p.split("="))
                    .filter(p -> !p[0].equals("session"))
                    .collect(Collectors.toMap(p -> p[0], p -> p[1]));
            newQuery = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            if (newQuery.isEmpty())
                newQuery = null;
        }

        // Step 6: Remove fragment and rebuild
        return new URI(scheme, host, uri.getPath(), newQuery, null).toString();

    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Try-with-resources
            String line;
            while ((line = br.readLine()) != null) {

                try {
                    String normalized = normalizeUrl(line, null);
                    if (normalized != null && !normalized.isEmpty()) {
                        RobotsM.parseRobots(normalized); // Pre-fetch robots.txt
                        urlsToCrawl.add(normalized);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to normalize seed URL: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Seed URLs loaded: " + urlsToCrawl.size());
        }
    }

    private void crawl() {
        System.out.println("Starting crawl with " + numThreads + " threads...");
        for (int i = 0; i < numThreads; i++) {
            System.out.println("Submitting worker " + i);
            executor.submit(new CrawlerWorker());
        }
        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
                System.err.println("Crawl timed out after 1 hour");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.err.println("Crawl interrupted: " + e.getMessage());
        }
    }

}
