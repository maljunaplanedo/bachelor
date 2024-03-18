package ru.dbhub;

import java.io.IOException;
import java.util.List;

interface NewsSource {

    List<JustCollectedArticle> getArticlesPage(int pageNo) throws IOException;
}
