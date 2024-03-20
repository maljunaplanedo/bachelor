package ru.dbhub.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.dbhub.Article;
import ru.dbhub.CollectorService;

import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {
    @Autowired
    private CollectorService collectorService;

    @GetMapping("")
    public List<Article> getArticles(@RequestParam long boundId) {
        return collectorService.getArticlesAfter(boundId);
    }
}
