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
public class ArticlesController {
    private static final String LONG_MAX_VALUE_STR = String.valueOf(Long.MAX_VALUE);

    @Autowired
    private CollectorService collectorService;

    @GetMapping("/after")
    public List<Article> getArticlesAfter(@RequestParam long boundId) {
        return collectorService.getArticlesAfter(boundId);
    }

    @GetMapping("/page")
    public List<Article> getArticlesPage(
        @RequestParam(defaultValue = "9223372036854775807") long boundId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam int count
    ) {
        return collectorService.getArticlesPage(boundId, page, count);
    }
}
