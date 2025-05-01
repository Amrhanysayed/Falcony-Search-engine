package Crawler;

import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerWorker implements Runnable {
  private final ConcurrentLinkedQueue<String> urlsToCrawl ;
  private final Set<String> visited;
  private final AtomicInteger pageCount;
  private final int maxPages;
  private final RobotsManager robotsM;
  private final BlockingQueue<Document> documentQueue;
  private final Crawler crawler;
  private final ConcurrentHashMap<String, Boolean> canCrawlCache;

  // Constants
  private static final int CONNECT_TIMEOUT = 15000; // 15 seconds
  private static final int MAX_IMAGES_PER_PAGE = 50;

  public CrawlerWorker(
          ConcurrentLinkedQueue<String> urlsToCrawl,
          Set<String> visited,
          AtomicInteger pageCount,
          int maxPages,
          RobotsManager robotsM,
          BlockingQueue<Document> documentQueue,
          Crawler crawler,
          ConcurrentHashMap<String, Boolean> canCrawlCache) {
    this.urlsToCrawl = urlsToCrawl;
    this.visited = visited;
    this.pageCount = pageCount;
    this.maxPages = maxPages;
    this.robotsM = robotsM;
    this.documentQueue = documentQueue;
    this.crawler = crawler;
    this.canCrawlCache = canCrawlCache;
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted() && pageCount.get() < maxPages) {
      String url = urlsToCrawl.poll();
      if (url == null) {
        // If no URLs, wait a bit before trying again
        try {
          Thread.sleep(100);
          continue;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }


      // Skip if already visited
      if (!visited.add(url)) {
        continue;
      }

      // Check robots.txt (with caching)
      if (!canCrawl(url)) {
        continue;
      }

      try {

        org.jsoup.nodes.Document doc = Jsoup.connect(url)
                .timeout(CONNECT_TIMEOUT)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .maxBodySize(1_000_000) // 1MB
                .get();

        int currentCount = pageCount.incrementAndGet();

        // Extract page data
        String title = doc.title() != null && !doc.title().isEmpty() ? doc.title() : "Untitled";
        String content = doc.body() != null ? doc.body().html() : "";


        Elements links = doc.select("a[href]");
        Elements images = doc.select("img[src]");

        Set<String> linksText = ConcurrentHashMap.newKeySet();
        Set<String> imageUrls = ConcurrentHashMap.newKeySet();

        // Process links
        int linkCount = 0;
        for (Element link : links) {
          String newUrl = link.absUrl("href");
          if (newUrl.isEmpty()) continue;

          String normalizedUrl = crawler.normalizeUrl(newUrl, url);
          if (normalizedUrl != null && normalizedUrl.length() < 500) { // Avoid extremely long URLs
            linksText.add(normalizedUrl);
            // Only add to crawl queue if not visited and allowed by robots.txt
            if (!visited.contains(normalizedUrl) && canCrawl(normalizedUrl)) {
              if(urlsToCrawl.size()<10000){
              urlsToCrawl.add(normalizedUrl);

              }
              linkCount++;
            }
          }
        }


        // Process images with limit 50 image per page
        int imageCount = 0;
        for (Element image : images) {
          if (imageCount >= MAX_IMAGES_PER_PAGE) break;

          String imageUrl = image.absUrl("src");
          if (imageUrl.isEmpty()) continue;

          String normalizedImageUrl = crawler.normalizeUrl(imageUrl, url);
          if (normalizedImageUrl != null) {
            imageUrls.add(normalizedImageUrl);
            imageCount++;
          }
        }

        // Create document and add to queue
        Document bsonDoc = new Document("url", url)
                .append("title", title)
                .append("content", content)
                .append("timestamp", System.currentTimeMillis())
                .append("indexed", false)
                .append("links", linksText)
                .append("images", imageUrls);

        try {
          // Add to queue with timeout to prevent blocking forever
          if (!documentQueue.offer(bsonDoc, 15, TimeUnit.SECONDS)) {
            System.err.println("Failed to queue document: " + url + " - queue full");
          }else{
            System.out.println("sucess to add : " + url);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

        // check if we finish the 600 pages
        if (pageCount.get() >= maxPages) {
          break;
        }

      } catch (IOException e) {
          System.err.println("Failed to fetch: " + url + " - " + e.getMessage());
      }
    }
  }

  private boolean canCrawl(String url) {
    // Check cache first
    Boolean cached = canCrawlCache.get(url);
    if (cached != null) {
      return cached;
    }

    boolean result = robotsM.canCrawl(url);

    // Cache the result if cache isn't too large
    if (canCrawlCache.size() < 1000) {
      canCrawlCache.put(url, result);
    }

    return result;
  }
}