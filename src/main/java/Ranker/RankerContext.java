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

    public List<WebDocument> rank(List<String> queryTexts , List<String> tokensFirst , List<String> tokensSecond, Set<String> candidateDocsIds , String logicalOperator, Integer page, Integer docsPerPage) {
        return ranker.rank(queryTexts,tokensFirst , tokensSecond, candidateDocsIds , logicalOperator, page, docsPerPage);
    }



}
