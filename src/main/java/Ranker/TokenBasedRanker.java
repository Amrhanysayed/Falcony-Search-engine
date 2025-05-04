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
    public List<WebDocument> rank(List<String> queryTexts, List<String> tokens, List<String> tokensSecond, Set<String> candidateDocsIds, String logicalOperator) {

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

        Map<String, List<Posting>> tokenToPostings = db.getPostingsForTokens(combinedTokens, candidateDocsIds);
        Map<String, WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);

        // Get relevance scores - pass weightConfig
        Map<String, Double> docScores = Helpers.RelevanceScore(combinedTokens, tokenToPostings, totalDocCount, candidateDocs, weightConfig);

        // Apply popularity adjustment
        docScores = Helpers.ApplyPopularityScore(docScores, candidateDocs, popularityAlpha);

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> candidateDocs.get(entry.getKey()))
                .toList();
    }
}