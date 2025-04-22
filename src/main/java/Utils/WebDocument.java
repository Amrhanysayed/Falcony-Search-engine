package Utils;

import org.jsoup.Jsoup;

public class WebDocument {
    public final String docId; // Made final for immutability
    public String url, title, html;
    public int popularity;
    public int[] children;

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

    public String getId() {
        return docId;
    }

    public String getSoupedContent() {
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
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