package Indexer;

import Utils.Tokenizer;
import Utils.WebDocument;
import opennlp.tools.tokenize.TokenizerME;

public class IndexerWorker implements Runnable {
    WebDocument document;
    TokenizerME tokenizer;
    public IndexerWorker(WebDocument document, Tokenizer tokenizer) {
        this.document = document;
        this.tokenizer = tokenizer.getTokenizerME();
    }
    @Override
    public void run() {
        System.out.println("indexing document: " + document.getId());
        Indexer.indexDocument(document, tokenizer);
    }
}
