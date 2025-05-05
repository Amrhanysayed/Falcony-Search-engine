package Ranker;

import Utils.WebDocument;

import java.util.List;
import java.util.Set;

public interface Ranker {

     List<WebDocument> rank(List<String> queryTexts, List<String> tokensFirst, List<String> tokensSecond, Set<String> candidateDocsIds, String logicalOperator, Integer page, Integer docsPerPage);
}
