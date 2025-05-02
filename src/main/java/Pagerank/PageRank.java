package Pagerank;

import dbManager.dbManager;

public class PageRank {
    public static void main(String[] args) {
        LinkGraphBuilder builder = new LinkGraphBuilder();
        dbManager db = new dbManager();
        builder.buildUrlIdMaps();
        builder.buildLinkGraphInMemory();
        builder.shutdownExecutor();

        PageRankCalculator calculator = new PageRankCalculator(
                builder.getIncomingLinks(),
                builder.getOutDegreeCache(),
                db.getDocumentsForGraphBuilder()
        );

        calculator.initializePageRanks();
        calculator.calculatePageRanks();
        calculator.savePageRanks();
    }
}