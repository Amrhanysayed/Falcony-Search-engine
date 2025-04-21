//package Ranker;
//
//import java.util.List;
//
//public interface Ranker {
//    /**
//     * Ranks the given candidate documents based on preprocessed tokens or phrases.
//     *
//     * @param queryTerms a list of stemmed tokens or phrases extracted by the QueryProcessor.
//     * @param candidateDocs list of document IDs to be ranked.
//     * TODO candidateDocs should be passed as docs ?
//     * @return a list of document IDs sorted by relevance score.
//     */
//
////            List<String> tokens,
////            Map<String, List<Posting>> invertedIndex,
////            Map<Integer, Integer> docLengths,
////            Map<Integer, Integer> docPopularity,
////            int totalDocuments
//    List<Integer> rank(List<String> queryTerms, List<Integer> candidateDocs);
//}
