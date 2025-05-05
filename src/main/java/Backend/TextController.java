package Backend;

import ImageSearching.ImageFeatureExtractor;
import QueryProcessor.QueryProcessor;
import Utils.WebDocument;
import ai.onnxruntime.OrtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@RestController
public class TextController {
    private QueryProcessor queryProcessor;

    @PostConstruct
    public void init() throws Exception {
        queryProcessor = new QueryProcessor();
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/search")
    public List<WebDocument> handleQuery(@RequestParam(name = "query") String queryValue, @RequestParam(name = "limit") Integer limit, @RequestParam Integer page) throws Exception {
        System.out.println("query: " + queryValue);
        System.out.println("limit: " + limit);
        System.out.println("page: " + page);
        return queryProcessor.process(queryValue, page, limit);
    }
}