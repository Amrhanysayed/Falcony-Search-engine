package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerWorker implements Runnable {
  private final ConcurrentLinkedQueue<String> urlsToCrawl;
  private final Set<String> visited;
  private final AtomicInteger pageCount;
  private final int maxPages;
  private final RobotsManager robotsM;
  public CrawlerWorker(ConcurrentLinkedQueue<String> urlsToCrawl, Set<String> visited, AtomicInteger pageCount, int maxPages, RobotsManager robotsM) {
    this.urlsToCrawl = urlsToCrawl;
    this.visited = visited;
    this.pageCount = pageCount;
    this.maxPages = maxPages;
    this.robotsM = robotsM;

  }

  @Override
  public void run() {
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
        Document doc = Jsoup.connect(url).timeout(5000).get();

//        System.out.println("Crawling: " + url);

        if (pageCount.incrementAndGet() >= maxPages) {
          urlsToCrawl.clear();
          break;
        }

        Elements links = doc.select("a[href]");
        for (Element link : links) {
          String newUrl = link.absUrl("href");
          try {
            String normalized = Crawler.normalizeUrl(newUrl, url);
            if (normalized != null &&
                    !normalized.isEmpty() &&
                    !visited.contains(normalized) &&
                    robotsM.canCrawl(normalized)) {
              System.out.println("Found: " + normalized);
              urlsToCrawl.add(normalized);
            }
          } catch (Exception e) {
            System.err.println("Failed to normalize URL: " + newUrl + " - " + e.getMessage());
          }
        }
      } catch (IOException e) {
        System.err.println("Failed to fetch: " + url + " - " + e.getMessage());
      }
    }
  }
}