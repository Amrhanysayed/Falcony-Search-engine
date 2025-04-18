package Indexer;

import Utils.Document;
import opennlp.tools.tokenize.TokenizerME;

public class IndexerWorker implements Runnable {
    Document document;
    TokenizerME tokenizer;
    public IndexerWorker(Document document, Tokenizer tokenizer) {
        this.document = document;
        this.tokenizer = tokenizer.getTokenizerME();
    }
    @Override
    public void run() {
        System.out.println("indexing document: " + document.getId());
        Indexer.indexDocument(document, tokenizer);
    }
}
