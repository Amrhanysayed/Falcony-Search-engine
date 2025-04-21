package Utils;

import org.jsoup.Jsoup;

public class WebDocument {
    public String docId, url, title, html;
    public int popularity;
    public int [] children;

    public WebDocument(String docId, String url, String title, String html , int popularity , int [] children) {
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
    }

    public String getId() {
        return docId;
    }

    public String getSoupedContent() {
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }

    public void Print()
    {
        System.out.println();
        System.out.println("Document ID: " + docId);
        System.out.println(url);
        System.out.println(title);
        System.out.println(html);
        System.out.println();

    }


}
