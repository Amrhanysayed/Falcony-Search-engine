package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import dbManager.dbManager;

public class CrawlerWorker implements Runnable {
  private final ConcurrentLinkedQueue<String> urlsToCrawl;
  private final Set<String> visited;
  private final AtomicInteger pageCount;
  private final int maxPages;
  private final RobotsManager robotsM;
  private final dbManager dbManager;
  private ExecutorService linkProcessor; // Thread pool for link processing

  public CrawlerWorker(ConcurrentLinkedQueue<String> urlsToCrawl,
                       Set<String> visited,
                       AtomicInteger pageCount,
                       int maxPages,
                       RobotsManager robotsM,
                       dbManager dbManager) {
    this.urlsToCrawl = urlsToCrawl;
    this.visited = visited;
    this.pageCount = pageCount;
    this.maxPages = maxPages;
    this.robotsM = robotsM;
    this.dbManager = dbManager;
    // Create a thread pool for link processing (e.g., 4 threads per worker)
    this.linkProcessor = Executors.newFixedThreadPool(4);
  }

  @Override
  public void run() {
    List<org.bson.Document> batch = new ArrayList<>();
    while (!urlsToCrawl.isEmpty() && pageCount.get() < maxPages) {
      String url = urlsToCrawl.poll();
      if (url == null || !visited.add(url)) {
        continue;
      }

      if (!robotsM.canCrawl(url)) {
        System.out.println("Blocked by robots.txt: " + url);
        continue;
      }

      try {
        Document doc = Jsoup.connect(url)
                .timeout(5000)
                .ignoreContentType(true)
                .get();

        System.out.println("Crawling: " + url);

        // Extract title and content
          String title = !doc.title().isEmpty() ? doc.title() : "Untitled";
          String content = doc.body().html(); // TODO TEST


        // Extract links
        Elements links = doc.select("a[href]");
        System.out.println("links sized " + links.size());

        // Start timing
        long startTime = System.currentTimeMillis();

        // Submit link processing tasks
        List<Runnable> linkTasks = new ArrayList<>();
        List<String> linksText = new ArrayList<>();

        for (Element link : links) {
          String newUrl = link.absUrl("href");
          linkTasks.add(() -> processLink(newUrl, url));
          linksText.add(newUrl); // TODO: insert normalized URL instead
        }

        // Add to batch for MongoDB
        org.bson.Document bsonDoc = new org.bson.Document("url", url)
                .append("title", title)
                .append("content", content)
                .append("timestamp", System.currentTimeMillis())
                .append("indexed", false)
                .append("links", linksText);
        batch.add(bsonDoc);

        // Insert batch if large enough

        if (batch.size() >= 100) {

          dbManager.insertDocuments(batch);
          batch.clear();
        }

        if (pageCount.incrementAndGet() >= maxPages) {
          urlsToCrawl.clear();
          break;
        }

        // Execute tasks in parallel
        for (Runnable task : linkTasks) {
          linkProcessor.submit(task);
        }

        // Shutdown and wait for link processing to complete
        linkProcessor.shutdown();
        try {
          if (!linkProcessor.awaitTermination(10, TimeUnit.SECONDS)) {
            linkProcessor.shutdownNow();
            System.err.println("Link processing timed out for URL: " + url);
          }
        } catch (InterruptedException e) {
          linkProcessor.shutdownNow();
          System.err.println("Link processing interrupted for URL: " + url);
          Thread.currentThread().interrupt();
        }

        // Recreate thread pool for next URL
        linkProcessor = Executors.newFixedThreadPool(4);

        // End timing
        long endTime = System.currentTimeMillis();
        System.out.println("Link processing took: " + (endTime - startTime) + " ms");

      } catch (IOException e) {
        System.err.println("Failed to fetch: " + url + " - " + e.getMessage());
      }
    }

    // Insert remaining batch
    if (!batch.isEmpty()) {
      dbManager.insertDocuments(batch);
    }

    // Clean up link processor
    linkProcessor.shutdownNow();
  }

  private void processLink(String newUrl, String baseUrl) {
    try {
      String normalized = Crawler.normalizeUrl(newUrl, baseUrl);
      if (normalized != null &&
              !normalized.isEmpty() &&
              !visited.contains(normalized) &&
              robotsM.canCrawl(normalized)) {
        System.out.println("Found: " + normalized);
        urlsToCrawl.add(normalized);
        System.out.println("the queue size is: " + urlsToCrawl.size());
      }
    } catch (Exception e) {
      System.err.println("Failed to normalize URL: " + newUrl + " - " + e.getMessage());
    }
  }
}