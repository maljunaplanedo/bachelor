package ru.dbhub;

import java.util.Optional;

public interface ArticleStorage {
    Optional<Article> getLastArticleOfSource(String source);


}
