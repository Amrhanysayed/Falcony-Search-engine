import Crawler.Crawler;
import Indexer.ImageIndexer;
import Indexer.TextIndexer;
import Pagerank.PageRank;


public class Engine {
    Engine() { }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        Crawler.main(args);

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime) / 1000;

        System.out.println("Crawler took " + duration + " seconds.");
        startTime = System.currentTimeMillis();

        TextIndexer indexer = new TextIndexer();
        indexer.runIndexer();

        endTime = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000;
        System.out.println("Text Indexer took " + duration + " seconds");

        startTime = System.currentTimeMillis();

        ImageIndexer imageIndexer = new ImageIndexer();
        imageIndexer.runIndexer();

        endTime = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000;
        System.out.println("Image Indexer took " + duration + " seconds");

        startTime = System.currentTimeMillis();

        PageRank.main(args);

        endTime = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000;
        System.out.println("Page Rank took " + duration + " seconds");

    }
}
