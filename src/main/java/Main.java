import Crawler.Crawler;

public class Main {
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