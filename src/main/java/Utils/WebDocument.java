package Utils;

public class WebDocument {
    public String docId, url, title, html, content;
    public int popularity;
    public int [] children;

    public WebDocument(String docId, String url, String title, String content , int popularity , int [] children) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = content;
        this.popularity = popularity;
        this.children = children;
    }


    public WebDocument(String docId, String url, String title, String content) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = content;
    }

    public String getId() {
        return docId;
    }

    public void Print()
    {
        System.out.println();
        System.out.println("Document ID: " + docId);
        System.out.println(url);
        System.out.println(title);
        System.out.println(content);
        System.out.println();

    }


}
