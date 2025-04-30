package Crawler;

import com.mongodb.client.MongoCollection;
import dbManager.dbManager;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Crawler {
    private final ConcurrentLinkedQueue<String> urlsToCrawl = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pageCount = new AtomicInteger(0); /// thread safe int
    private final int maxPages = 6000;
    private final RobotsManager robotsM;
    private final ExecutorService executor;
    private final int numThreads = 16;
    private final dbManager mongo; // database agent
    private static Set<String> excludedParams; // file for reading normalization
    // cache for normalized URLs to avoid re-normalizing
    private final ConcurrentHashMap<String, String> urlNormalizeCache = new ConcurrentHashMap<>(10000);
    // cache for canCrawl in robots  results
    private final ConcurrentHashMap<String, Boolean> canCrawlCache = new ConcurrentHashMap<>(1000);

    public Crawler() {
        this.mongo = new dbManager();
        this.robotsM = new RobotsManager();
        this.executor = new ThreadPoolExecutor(
                numThreads,
                numThreads,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadPoolExecutor.CallerRunsPolicy() // If queue is full, execute in the calling thread
        );
        readExcludeParams();
        loadState();
        addShutdownHook();
    }

    void readExcludeParams() {
        Set<String> tempParams = ConcurrentHashMap.newKeySet();
        try {

            try (BufferedReader reader = new BufferedReader(new  FileReader("src/exclude_params.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        tempParams.add(line.toLowerCase());
                    }
                }
                System.out.println("Loaded " + tempParams.size() + " excluded parameters");
            }
        } catch (IOException e) {
            System.err.println("Failed to load exclude_params.txt: " + e.getMessage());
            tempParams.addAll(Arrays.asList("session", "utm_source", "utm_medium", "utm_campaign", "ref", "fbclid"));
        }
        excludedParams = Collections.unmodifiableSet(tempParams);
    }

    private void loadState() {
        Map<String, Object> state = mongo.loadCrawlerState();
        if (state != null) {
            List<String> savedUrlsToCrawl = (List<String>) state.get("urlsToCrawl");
            List<String> savedVisited = (List<String>) state.get("visited");
            int savedPageCount = (Integer) state.get("pageCount");

            // Only load a subset of visited URLs to prevent memory issues
            int maxVisitedToLoad = 100000;
            if (savedVisited.size() > maxVisitedToLoad) {
                savedVisited = savedVisited.subList(savedVisited.size() - maxVisitedToLoad, savedVisited.size());
                System.out.println("Loaded " + maxVisitedToLoad + " of " + savedVisited.size() + " visited URLs");
            }

            urlsToCrawl.addAll(savedUrlsToCrawl);
            visited.addAll(savedVisited);
            pageCount.set(savedPageCount);
        }
    }

    private void saveState() {
        int currentCount = pageCount.get();
        mongo.saveCrawlerState(urlsToCrawl, visited, currentCount);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered, saving crawler state");
            saveState();
            close();
        }));
    }

    public void startCrawl(String filename) throws Exception {
        if (urlsToCrawl.isEmpty()) {
            readStartLinks(filename);
        }
        crawl();
    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String normalized = normalizeUrl(line, null);
                    if (normalized != null && !normalized.isEmpty() && !visited.contains(normalized)) {
                        robotsM.parseRobots(normalized);
                        urlsToCrawl.add(normalized);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to normalize seed URL: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading seed file: " + e.getMessage());
        }
        System.out.println("Seed URLs loaded: " + urlsToCrawl.size());
    }

    private void crawl() {
        List<Future<?>> futures = new ArrayList<>();

        // Create worker pool with shared document batch
        BlockingQueue<Document> documentBatchQueue = new LinkedBlockingQueue<>(6000); /// this will be send to the dbwriter

        // Start database writer thread
        DbWriterThread dbWriter = new DbWriterThread(documentBatchQueue, mongo);
        Thread dbWriterThread = new Thread(dbWriter);
        dbWriterThread.start();

        // Start worker threads
        for (int i = 0; i < numThreads; i++) {
            CrawlerWorker worker = new CrawlerWorker(
                    urlsToCrawl,
                    visited,
                    pageCount,
                    maxPages,
                    robotsM,
                    documentBatchQueue,
                    this,
                    canCrawlCache
            );
            futures.add(executor.submit(worker));
        }

        // save state  5 minutes
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::saveState, 5, 5, TimeUnit.MINUTES);

        // Wait for workers to finish
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Worker failed: " + e.getMessage());
            }
        }

        // Signal database writer to finish
        dbWriter.shutdown();
        try {
            dbWriterThread.join(30000);
        } catch (InterruptedException e) {
            System.err.println("DB writer shutdown interrupted");
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        saveState();
        executor.shutdown();
    }

    public String normalizeUrl(String url, String baseUrl) {
        // Check cache first
        String cacheKey = url + "|" + (baseUrl == null ? "" : baseUrl);
        String cached = urlNormalizeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        // Quick filters for common non-http URLs
        if (url.startsWith("javascript:") || url.startsWith("mailto:") || url.startsWith("tel:") ||
                url.startsWith("#") || url.startsWith("data:")) {
            return null;
        }

        // Filter out certain file types
        if (url.endsWith(".pdf") || url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".png") || url.endsWith(".gif") || url.endsWith(".css") ||
                url.endsWith(".js") || url.endsWith(".zip") || url.endsWith(".mp4") ||
                url.endsWith(".mp3")) {
            return null;
        }

        // Sanitize input URL
        url = url.replaceAll("\\?+", "?");

        while (url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }

        // Resolve relative URLs
        if (!url.startsWith("http") && baseUrl != null && !baseUrl.isEmpty()) {
            try {
                org.jsoup.nodes.Document doc = Jsoup.parse("<a href=\"" + url + "\"></a>", baseUrl);
                url = doc.select("a").first().absUrl("href");
            } catch (Exception e) {
                return null;
            }
        }

        // Parse and normalize URL
        URI uri;
        try {
            uri = new URI(url).normalize();
        } catch (Exception e) {
            return null;
        }

        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "http";
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return null;
        }

        String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
        if (host.isEmpty()) {
            return null;
        }

        int port = uri.getPort();
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = -1;
        }

        String path = uri.getPath() != null ? uri.getPath() : "/";
        String query = uri.getQuery();
        String newQuery = null;

        if (query != null && !query.isEmpty()) {
            try {

                StringBuilder queryBuilder = new StringBuilder();
                boolean firstParam = true;

                for (String param : query.split("&")) {
                    String[] parts = param.split("=", 2);
                    if (parts.length == 2 && !parts[0].isEmpty() && !excludedParams.contains(parts[0].toLowerCase())) {
                        if (!firstParam) {
                            queryBuilder.append('&');
                        } else {
                            firstParam = false;
                        }
                        queryBuilder.append(parts[0]).append('=').append(parts[1]);
                    }
                }

                if (queryBuilder.length() > 0) {
                    newQuery = queryBuilder.toString();
                }
            } catch (Exception e) {
                newQuery = null;
            }
        }

        try {
            String normalizedUrl = new URI(scheme, null, host, port, path, newQuery, null).toString();
            // Cache the result
            if (urlNormalizeCache.size() < 10000) { // Limit cache size
                urlNormalizeCache.put(cacheKey, normalizedUrl);
            }

            while (normalizedUrl.endsWith("?")) {
                normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
            }
            return normalizedUrl;
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        saveState();
        executor.shutdownNow();
        System.out.println("Crawler closed");
    }

    public static void main(String[] args) {
        String fileName = "src/seed.txt";
        Crawler cr = new Crawler();
        try {
            cr.startCrawl(fileName);
            System.out.println("Finished crawling.");
        } catch (Exception e) {
            System.err.println("Crawling failed: " + e.getMessage());
        } finally {
            cr.close();
            System.exit(0);
        }
    }


    /// //////////////////////////////////////////////////////////////////////////////////////////

    // Database writer thread to handle inserts in the background
    private static class DbWriterThread implements Runnable {
        private final BlockingQueue<Document> queue;
        private final dbManager dbManager;
        private volatile boolean running = true;
        private static final int BATCH_SIZE = 500;

        public DbWriterThread(BlockingQueue<Document> queue, dbManager dbManager) {
            this.queue = queue;
            this.dbManager = dbManager;
        }

        public void shutdown() {
            running = false;
        }

        @Override
        public void run() {
            List<Document> batch = new ArrayList<>(BATCH_SIZE);

            while (running || !queue.isEmpty()) {
                try {
                    Document doc = queue.poll(1, TimeUnit.SECONDS);
                    if (doc != null) {
                        batch.add(doc);
                    }
                    System.out.println("batch size is = "+batch.size());
                    // Insert batch if large enough or if no more documents are coming
                    if (batch.size() >= BATCH_SIZE || (!running && !batch.isEmpty() && queue.isEmpty())) {
                        try {
                            dbManager.insertDocuments(batch);
                            System.out.println("///////////////////////////Inserted batch of//////////////////////////  " + batch.size() + " documents");
                        } catch (Exception e) {
                            System.err.println("Batch insert failed: " + e.getMessage());
                        }
                        batch.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Insert any remaining documents
            if (!batch.isEmpty()) {
                try {
                    dbManager.insertDocuments(batch);
                    System.out.println("Inserted final batch of " + batch.size() + " documents");
                } catch (Exception e) {
                    System.err.println("Final batch insert failed: " + e.getMessage());
                }
            }
        }
    }
}