import Crawler.Crawler;

public class Main {
    public static void main(String[] args) {
        String fileName = "src/seed.txt";

        // Create Crawler instance
        Crawler cr = new Crawler(); // No auto-crawling
        try {
            // Start crawling and wait for completion
            cr.startCrawl(fileName);
            System.out.println("Finished crawling.");
        } catch (Exception e) {
            System.err.println("Crawling failed: " + e.getMessage());
        }
    }
}