package Backend;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import QueryProcessor.QueryProcessor;
import Utils.WebDocument;


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
    @CrossOrigin(origins = "*")
    @GetMapping("/suggestions")
    public List<String> handleSuggestions(@RequestParam(name = "query") String queryValue) throws Exception {
        System.out.println("query: " + queryValue);
        return queryProcessor.getSuggestions(queryValue);
    }

}