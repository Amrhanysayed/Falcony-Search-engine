package Utils;

import org.jsoup.Jsoup;

public class Document {
    int docId;
    public String url, title, content;

    public Document(int docId, String url, String title, String content) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = Jsoup.parse(content).text().replaceAll("\\s+", " ").trim();
        //System.out.println(this.content);
    }
    public int getId() {
        return docId;
    }
}
