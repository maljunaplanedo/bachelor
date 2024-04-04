package ru.dbhub.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.dbhub.Article;
import ru.dbhub.ArticlesAndBoundId;
import ru.dbhub.CollectorService;

import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticlesController {
    @Autowired
    private CollectorService collectorService;

    @GetMapping("/after")
    public ArticlesAndBoundId getArticlesAfter(@RequestParam long boundId) {
        return collectorService.getArticlesAfter(boundId);
    }

    @GetMapping("/page")
    public ArticlesAndBoundId getArticlesPage(
        @RequestParam long boundId, @RequestParam int page, @RequestParam int count
    ) {
        return collectorService.getArticlesPage(boundId, page, count);
    }
}
