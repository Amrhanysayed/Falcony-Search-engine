package Ranker;

import Utils.Posting;
import Utils.WebDocument;
import java.util.*;

public final class Helpers {
    // Private constructor prevents instantiation
    private Helpers() {}

    public static class WeightConfig {
        public final double bodyWeight;
        public final double titleWeight;
        public final double h1Weight;
        public final double h2Weight;

        public WeightConfig(double bodyWeight, double titleWeight, double h1Weight, double h2Weight) {
            this.bodyWeight = bodyWeight;
            this.titleWeight = titleWeight;
            this.h1Weight = h1Weight;
            this.h2Weight = h2Weight;
        }
    }

    public static Map<String, Double> RelevanceScore(List<String> tokens, Map<String, List<Posting>> tokenToPostings,
                                                     int totalDocCount, Map<String, WebDocument> candidateDocs,
                                                     WeightConfig weightConfig) {
        Map<String, Double> docScores = new HashMap<>();
        // Loop ON tokens

        for (String token : tokens) {
            List<Posting> postings = tokenToPostings.get(token);
            if (postings == null || postings.isEmpty()) continue; // Safe Check

            int df = postings.size();
            if (df == 0) continue; // Safe Check

            double idf = Math.log(totalDocCount / (double) df);

            for (Posting post : postings) {
                String docId = post.getDocId();
                WebDocument doc = candidateDocs.get(docId);
                double relevanceScore = TF_IDF(post, idf, weightConfig);

                docScores.put(docId, docScores.getOrDefault(docId, 0.0) + relevanceScore);
            }
        }
        return docScores;
    }

    public static Map<String, Double> ApplyPopularityScore(Map<String, Double> docScores,
                                                           Map<String, WebDocument> candidateDocs,
                                                           double popularityAlpha) {
        if (popularityAlpha <= 0) {
            return docScores; // No adjustment needed
        }

        for (Map.Entry<String, Double> entry : docScores.entrySet()) {
            WebDocument doc = candidateDocs.getOrDefault(entry.getKey(), null);
            if (doc != null) {
                double relevanceScore = entry.getValue();
                double popularity = doc.getPopularity();
                double combinedScore = (1 - popularityAlpha) * relevanceScore +
                        popularityAlpha * 10000 * popularity;

                entry.setValue(combinedScore);
            }
        }

        return docScores;
    }

    public static double TF_IDF(Posting post, double idf, WeightConfig weightConfig) {
        double bodyFreq = post.getFrequency("body");
        double titleFreq = post.getFrequency("title");
        double h1Freq = post.getFrequency("h1");
        double h2Freq = post.getFrequency("h2");

        // Avoid log(0) by adding 1 to frequencies
        double BodyTf = weightConfig.bodyWeight * (bodyFreq > 0 ? Math.log(bodyFreq + 1) : 0);
        double TitleTf = weightConfig.titleWeight * (titleFreq > 0 ? Math.log(titleFreq + 1) : 0);
        double H1Tf = weightConfig.h1Weight * (h1Freq > 0 ? Math.log(h1Freq + 1) : 0);
        double H2Tf = weightConfig.h2Weight * (h2Freq > 0 ? Math.log(h2Freq + 1) : 0);

        return idf * (BodyTf + TitleTf + H1Tf + H2Tf);
    }
}