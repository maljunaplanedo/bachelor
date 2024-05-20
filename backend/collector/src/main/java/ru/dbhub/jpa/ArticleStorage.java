package ru.dbhub.jpa;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import ru.dbhub.Article;
import ru.dbhub.ArticleStorage;
import ru.dbhub.JustCollectedArticle;

import java.util.List;
import java.util.Optional;

@Entity
@Table(
    name = "Articles",
    indexes = {
        @Index(columnList = "timestamp"),
        @Index(columnList = "id,timestamp"),
        @Index(columnList = "link")
    }
)
class ArticleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    @Column(columnDefinition = "TEXT", unique = true)
    private String link;

    @Column(columnDefinition = "TEXT")
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

    Long getId() {
        return id;
    }
}

@Repository
interface ArticleRepository extends JpaRepository<ArticleModel, Long> {
    List<ArticleModel> findByIdGreaterThanOrderByIdAsc(long id, Limit limit);

    List<ArticleModel> findByIdLessThanEqualOrderByTimestampDesc(long id, Pageable pageable);

    Optional<ArticleModel> findTopByOrderByIdDesc();

    boolean existsByLink(String link);
}

@Entity
@Table(name = "LastTimestampsOfSources")
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
    public List<Article> getAfter(long boundId, int count) {
        return articleRepository.findByIdGreaterThanOrderByIdAsc(boundId, Limit.of(count)).stream()
            .map(ArticleModel::toArticle)
            .toList();
    }

    @Override
    public List<Article> getPage(long boundId, int page, int count) {
        return articleRepository
            .findByIdLessThanEqualOrderByTimestampDesc(boundId, PageRequest.of(page, count))
            .stream()
            .map(ArticleModel::toArticle)
            .toList();
    }

    @Override
    public Optional<Long> getMaxId() {
        return articleRepository
            .findTopByOrderByIdDesc()
            .map(ArticleModel::getId);
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
    public boolean has(String link) {
        return articleRepository.existsByLink(link);
    }

    @Override
    public void addJustCollected(String source, JustCollectedArticle justCollectedArticle) {
        articleRepository.save(ArticleModel.ofJustCollectedArticle(source, justCollectedArticle));
    }
}
