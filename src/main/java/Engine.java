import Crawler.Crawler;
import Indexer.Indexer;
import Pagerank.LinkGraphBuilder;
import Pagerank.PageRank;
import Pagerank.PageRankCalculator;
import QueryProcessor.QueryProcessor;
import dbManager.dbManager;


public class Engine {
    Engine() { }

    public static void main(String[] args) throws Exception {
        Crawler.main(args);
        Indexer.main(args);
        PageRank.main(args);

    }


}
