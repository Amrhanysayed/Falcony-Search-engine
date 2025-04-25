package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Collections;


import dbManager.dbManager;

public class Crawler {
    private final ConcurrentLinkedQueue<String> urlsToCrawl = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pageCount = new AtomicInteger(0);
    int maxPages = 6000;
    private final RobotsManager robotsM;
    private final ExecutorService executor;
    private final int numThreads = 8;
    private final dbManager mongo ;

    private static  Set<String> excludedParams ;


    public Crawler() {
        mongo =  new dbManager();
        this.robotsM = new RobotsManager();
        executor = Executors.newFixedThreadPool(numThreads);
        this.readExcludedParams();
    }

    public void startCrawl(String filename) throws Exception {
        readStartLinks(filename);
        crawl();
    }



    private void readExcludedParams() {
        // Initialize excludedParams by reading exclude_params.txt
        Set<String> tempParams = ConcurrentHashMap.newKeySet();
        try {
            // Use Crawler.class.getResourceAsStream for robustness
            InputStream inputStream = Crawler.class.getResourceAsStream("/exclude_params.txt");
            if (inputStream == null) {
                throw new IOException("Resource not found: /exclude_params.txt");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        tempParams.add(line.toLowerCase());
                    }
                }
                System.out.println("Loaded " + tempParams.size() + " excluded parameters: " + tempParams);
            }
        } catch (IOException e) {
            System.err.println("Failed to load exclude_params.txt: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
            // Initialize with default parameters if file is missing
            tempParams.addAll(Arrays.asList("session", "utm_source", "utm_medium", "utm_campaign", "ref", "fbclid"));
        }
        excludedParams = Collections.unmodifiableSet(tempParams);
    }

    protected static String normalizeUrl(String url, String baseUrl) throws Exception {
        if (!url.startsWith("http") && baseUrl != null) {
            Document doc = Jsoup.parse("<a href=\"" + url + "\"></a>", baseUrl);
            url = doc.select("a").first().absUrl("href");
        }
        URI uri = new URI(url).normalize();
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
        String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        if (host.isEmpty()) {
            return null;
        }
        int port = uri.getPort();
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = -1;
        }
        String query = uri.getQuery();
        String newQuery = query;
        if (query != null) {
            Map<String, String> params = Arrays.stream(query.split("&"))
                    .map(p -> p.split("=", 2))
                    .filter(p -> p.length == 2 && !excludedParams.contains(p[0].toLowerCase()))
                    .collect(Collectors.toMap(p -> p[0], p -> p[1], (v1, v2) -> v1));
            newQuery = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            if (newQuery.isEmpty()) {
                newQuery = null;
            }
        }
        try {
            String normalized = new URI(scheme, host, uri.getPath(), newQuery, null).toString();
            System.out.println("Normalized " + url + " to " + normalized);
            return normalized;
        } catch (Exception e) {
            System.err.println("Failed to normalize: " + url + " - " + e.getMessage());
            return null;
        }
    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String normalized = normalizeUrl(line, null);
                    if (normalized != null && !normalized.isEmpty()) {
                        robotsM.parseRobots(normalized);
                        urlsToCrawl.add(normalized);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to normalize seed URL: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading seed file: " + e.getMessage());
        }
        System.out.println("Seed URLs loaded: " + urlsToCrawl.size());
    }

    private void crawl() {
        for (int i = 0; i < numThreads; i++) {
            executor.submit(new CrawlerWorker(urlsToCrawl, visited, pageCount, maxPages, robotsM,mongo));
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
                System.err.println("Crawl timed out after 1 hour");
            } else {
                System.out.println("Crawl completed successfully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.err.println("Crawl interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        String fileName = "src/seed.txt";


        Crawler cr = new Crawler();
        try {
            cr.startCrawl(fileName);
            System.out.println("Finished crawling.");
        } catch (Exception e) {
            System.err.println("Crawling failed: " + e.getMessage());
        }
    }

}