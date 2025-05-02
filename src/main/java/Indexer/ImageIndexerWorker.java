package Indexer;

import ImageSearching.ImageFeatureExtractor;
import Utils.Tokenizer;
import Utils.WebDocument;

public class ImageIndexerWorker implements Runnable {
    WebDocument document;
    ImageFeatureExtractor featureExtractor;

    public ImageIndexerWorker(WebDocument document, ImageFeatureExtractor extractor) {
        this.document = document;
        this.featureExtractor = extractor;
    }
    @Override
    public void run() {
        System.out.println("indexing document: " + document.getId());
        Indexer.processImagesInDocument(document, featureExtractor);
    }
}
