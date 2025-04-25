package Ranker;

import Utils.WebDocument;

import java.util.List;
import java.util.Set;

public class RankerContext  {
    private Ranker ranker;

    // Set Ranking Strategy
    public void setRanker(Ranker r) {
        this.ranker = r;
    }

    public List<WebDocument> rank(List<String> queryTerms, Set<String> candidateDocsIds , String logicalOperator) {
        return ranker.rank(queryTerms, candidateDocsIds , logicalOperator);
    }



}
