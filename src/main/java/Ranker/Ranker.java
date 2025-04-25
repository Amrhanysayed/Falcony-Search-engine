package Ranker;

import Utils.WebDocument;

import java.util.List;
import java.util.Set;

public interface Ranker {

     List<WebDocument> rank(List<String> tokens, Set<String> candidateDocsIds , String logicalOperator) ;

}
