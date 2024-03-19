package ru.dbhub.jpa;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.Article;
import ru.dbhub.ArticleStorage;
import ru.dbhub.JustCollectedArticle;

import java.util.List;

@Entity
@Table(name = "Articles", indexes = @Index(columnList = "source,link"))
class ArticleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String link;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String text;

    private Long timestamp;

    private ArticleModel() {
    }

    static ArticleModel ofJustCollectedArticle(String source, JustCollectedArticle justCollectedArticle) {
        var result = new ArticleModel();
        result.source = source;
        result.link = justCollectedArticle.link();
        result.title = justCollectedArticle.title();
        result.text = justCollectedArticle.text();
        result.timestamp = justCollectedArticle.timestamp();
        return result;
    }

    Article toArticle() {
        return new Article(id, source, link, title, text, timestamp);
    }
}

@Repository
interface ArticleRepository extends JpaRepository<ArticleModel, Long> {
    List<ArticleModel> findByIdGreaterThanEqualOrderById(long id);

    boolean existsBySourceAndLink(String source, String link);
}

@Entity
@Table(name = "LastTimestampOfSource")
class LastTimestampOfSourceModel {
    @Id
    private String source;

    private Long timestamp;

    private LastTimestampOfSourceModel() {
    }

    LastTimestampOfSourceModel(String source, long timestamp) {
        this.source = source;
        this.timestamp = timestamp;
    }

    long getTimestamp() {
        return timestamp;
    }
}

@Repository
interface LastTimestampOfSourceRepository extends JpaRepository<LastTimestampOfSourceModel, String> {
}

@Component
class ArticleStorageImpl implements ArticleStorage {
    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private LastTimestampOfSourceRepository lastTimestampOfSourceRepository;

    @Override
    public List<Article> getAfter(long boundId) {
        return articleRepository.findByIdGreaterThanEqualOrderById(boundId).stream()
            .map(ArticleModel::toArticle)
            .toList();
    }

    @Override
    public long getLastTimestampOfSource(String source) {
        return lastTimestampOfSourceRepository.findById(source)
            .map(LastTimestampOfSourceModel::getTimestamp)
            .orElse(0L);
    }

    @Override
    public void setLastTimestampOfSource(String source, long timestamp) {
        lastTimestampOfSourceRepository.save(new LastTimestampOfSourceModel(source, timestamp));
    }

    @Override
    public boolean has(String source, String link) {
        return articleRepository.existsBySourceAndLink(source, link);
    }

    @Override
    public void addJustCollected(String source, JustCollectedArticle justCollectedArticle) {
        articleRepository.save(ArticleModel.ofJustCollectedArticle(source, justCollectedArticle));
    }
}
