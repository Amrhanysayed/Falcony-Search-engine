//package Ranker;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class TokenBasedRanker implements Ranker {
//
//    private final int popularityAlpha ;
//    public TokenBasedRanker(int popularityAlpha )
//    {
//        this.popularityAlpha = popularityAlpha;
//    }
//
//
//    List<Integer> rank(List<String> tokens, List<Integer> candidateDocs){
//
////        List<String> tokens,
////        Map<String, List<Posting>> invertedIndex,
////        Map<Integer, Integer> docLengths,
////        Map<Integer, Integer> docPopularity,
////        int totalDocuments
////        double avgDocLength = docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(1.0);
////
//        // loop over tokens
//        // for each token
//        // handle the algo calling pop * alpha + bmsocre (doc based)
//        // sort docs
//        // return sorted list of docs ids
//
//
//
//
//        //double idf = Math.log(1 + (totalDocuments - df + 0.5) / (df + 0.5));
//
//    }
//
//
//    private double BM25Calculator()
//    {
//        double numerator = tf * (k1 + 1);
//        double denominator = tf + k1 * (1 - b + b * docLen / avgDocLen);
//        return idf * (numerator / denominator);
//    }
//
//}