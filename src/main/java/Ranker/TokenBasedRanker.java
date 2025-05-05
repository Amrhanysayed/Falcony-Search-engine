package Ranker;

import Utils.Posting;
import Utils.WebDocument;
import dbManager.dbManager;
import java.util.*;

public class TokenBasedRanker implements Ranker {

    private dbManager db;
    private final double popularityAlpha;
    private final Helpers.WeightConfig weightConfig;

    public TokenBasedRanker(double popularityAlpha) throws Exception {
        this(popularityAlpha, new Helpers.WeightConfig(1.0, 1.5, 1.3, 1.2)); // Default weights
    }

    public TokenBasedRanker(double popularityAlpha, Helpers.WeightConfig weightConfig) throws Exception {
        this.popularityAlpha = popularityAlpha;
        this.weightConfig = weightConfig;
        this.db = new dbManager();
    }

    @Override
    public List<WebDocument> rank(List<String> queryTexts, List<String> tokens, List<String> tokensSecond, Set<String> candidateDocsIds, String logicalOperator, Integer page, Integer docsPerPage) {

        Integer totalDocCount = db.getTotalDocCount();
        if (totalDocCount == 0) {
            return new ArrayList<>();
        }

        // Combine tokensFirst and tokensSecond without duplicates
        Set<String> combinedTokensSet = new HashSet<>(tokens);
        if (tokensSecond != null) {
            combinedTokensSet.addAll(tokensSecond);
        }
        List<String> combinedTokens = new ArrayList<>(combinedTokensSet);

        double startTime = System.currentTimeMillis();
        Map<String, List<Posting>> tokenToPostings = db.getPostingsForTokens(combinedTokens, candidateDocsIds);
        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000;
        System.out.println("Get postings for tokens took " + duration + " seconds");
        startTime = System.currentTimeMillis();
        Map<String, WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);
        endTime = System.currentTimeMillis();
        duration = (endTime - startTime) / 1000;
        System.out.println("Get docs by id took " + duration + " seconds");
        // Get relevance scores - pass weightConfig
        Map<String, Double> docScores = Helpers.RelevanceScore(combinedTokens, tokenToPostings, totalDocCount, candidateDocs, weightConfig);

        // Apply popularity adjustment
        docScores = Helpers.ApplyPopularityScore(docScores, candidateDocs, popularityAlpha);

        // Calculate skip value for pagination (page is 1-based)
        int skip = (page - 1) * docsPerPage;

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .skip(skip)
                .limit(docsPerPage)
                .map(entry -> candidateDocs.get(entry.getKey()))
                .toList();
    }
}