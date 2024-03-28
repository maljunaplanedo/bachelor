package ru.dbhub;

import java.io.IOException;
import java.util.List;

public abstract class PageLimitedNewsSource implements NewsSource {
    private final int maxPage;

    PageLimitedNewsSource(int maxPage) {
        this.maxPage = maxPage;
    }

    @Override
    public final List<JustCollectedArticle> getArticlesPage(int pageNo) throws IOException {
        if (pageNo > maxPage) {
            return List.of();
        }
        return doGetArticlesPage(pageNo);
    }

    protected abstract List<JustCollectedArticle> doGetArticlesPage(int pageNo) throws IOException;
}
