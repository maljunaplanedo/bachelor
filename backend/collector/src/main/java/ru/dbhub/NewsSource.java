package ru.dbhub;

import java.io.IOException;
import java.util.List;

public interface NewsSource {
    record TypeAndConfig(String type, String config) {
    }

    List<JustCollectedArticle> getArticlesPage(int pageNo) throws IOException;
}
