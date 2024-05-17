package ru.dbhub;

import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;

public abstract class PageLimitedNewsSource implements NewsSource {
    private final Integer maxPage;

    private int page = 1;

    PageLimitedNewsSource(@Nullable Integer maxPage) {
        this.maxPage = maxPage;
    }

    @Override
    public List<JustCollectedArticle> nextArticlesPage() throws IOException {
        if (maxPage != null && page > maxPage) {
            return List.of();
        }
        var result = nextArticlesPageImpl();
        ++page;
        return result;
    }

    protected int getPageNo() {
        return page;
    }

    protected abstract List<JustCollectedArticle> nextArticlesPageImpl() throws IOException;
}
