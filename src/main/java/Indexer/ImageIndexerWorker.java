package Indexer;

import ImageSearching.ImageFeatureExtractor;
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
        ImageIndexer.processImagesInDocument(document, featureExtractor);
    }
}
