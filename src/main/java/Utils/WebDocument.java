package Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebDocument {
    public final String docId; // Made final for immutability
    public String url, title, html;
    public int popularity;
    public int[] children;
    private List<String> images;
    private Document parsedDocument; // Cache the parsed document for better performance

    public WebDocument(String docId, String url, String title, String html, int popularity, int[] children) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.html = html;
        this.popularity = popularity;
        this.children = children;
    }

    public WebDocument(String docId, String url, String title, String html) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.html = html;
        this.popularity = 0; // Default value
        this.children = null; // Default value
    }

    public WebDocument(String docId, String url, String title, String html, List<String> images) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.html = html;
        this.images = images;
    }

    // Get the parsed document, creating it if necessary
    private Document getParsedDocument() {
        if (parsedDocument == null && html != null) {
            parsedDocument = Jsoup.parse(html);
        }
        return parsedDocument;
    }

    public String getId() {
        return docId;
    }

    public String getUrl() {
        return url;
    }

    public int getPopularity() {
        return popularity;
    }

    // Updated to use parsed document
    public String getTitle() {
        return title;
    }

    // Get HTML content
    public String getHTML() {
        return html;
    }

    // Get content of all h1 elements
    public List<String> getH1s() {
        List<String> h1Texts = new ArrayList<>();
        Document doc = getParsedDocument();

        if (doc != null) {
            Elements h1Elements = doc.body().select("h1");
            for (Element h1 : h1Elements) {
                h1Texts.add(h1.text());
            }
        }
        return h1Texts;
    }

    // Get content of all h2 elements
    public List<String> getH2s() {
        List<String> h2Texts = new ArrayList<>();
        Document doc = getParsedDocument();

        if (doc != null) {
            Elements h2Elements = doc.body().select("h2");
            for (Element h2 : h2Elements) {
                h2Texts.add(h2.text());
            }
        }
        return h2Texts;
    }

    public String getSoupedContent() {
        Document doc = getParsedDocument();
        if (doc != null) {
            return doc.text().replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    public List<String> getImages() {
        return images;
    }

    public void Print() {
        System.out.println();
        System.out.println("Document ID: " + docId);
        System.out.println(url);
        System.out.println(title);
        System.out.println(html);
        System.out.println();
    }

    @Override
    public int hashCode() {
        return docId != null ? docId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WebDocument other = (WebDocument) obj;
        return docId != null ? docId.equals(other.docId) : other.docId == null;
    }

    @Override
    public String toString() {
        return "WebDocument{docId='" + docId + "'}";
    }
}