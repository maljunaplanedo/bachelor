package ru.dbhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class CollectorService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ConfigsStorage configsStorage;

    @Autowired
    private ArticleStorage articleStorage;

    private <C> C parseConfig(String configString, Class<C> configClass) throws BadConfigFormatException {
        try {
            return jsonMapper.readValue(configString, configClass);
        } catch (JsonProcessingException exception) {
            throw new BadConfigFormatException();
        }
    }

    private CollectorConfig parseCollectorConfig(String config) throws BadConfigFormatException {
        return parseConfig(config, CollectorConfig.class);
    }

    private NewsSource createNewsSource(NewsSourceTypeAndConfig typeAndConfig) throws BadConfigException {
        Class<?> sourceClass;
        try {
            var packageName = getClass().getPackageName();
            sourceClass = Class.forName(
                packageName + (packageName.isEmpty() ? "" : ".") + typeAndConfig.type() + "NewsSource"
            );
        } catch (ClassNotFoundException exception) {
            throw new BadConfigSourceTypeException();
        }

        var sourceConfigClass = Arrays.stream(sourceClass.getClasses())
            .filter(clazz -> clazz.getSimpleName().equals("Config"))
            .findFirst().orElseThrow(BadConfigSourceTypeException::new);

        Constructor<?> sourceConstructorOfConfig;
        try {
            sourceConstructorOfConfig = sourceClass.getConstructor(sourceConfigClass);
        } catch (NoSuchMethodException exception) {
            throw new BadConfigSourceTypeException();
        }

        var parsedConfig = parseConfig(typeAndConfig.config(), sourceConfigClass);

        try {
            return (NewsSource) sourceConstructorOfConfig.newInstance(parsedConfig);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new BadConfigSourceTypeException();
        }
    }

    @Transactional
    public String getCollectorConfig() {
        return configsStorage.getCollectorConfig();
    }

    @Transactional
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return configsStorage.getNewsSourceConfigs();
    }

    @Transactional
    private void setCollectorConfig(String config) {
        configsStorage.setCollectorConfig(config);
    }

    public void validateAndSetCollectorConfig(String config) throws BadConfigFormatException {
        parseCollectorConfig(config);
        setCollectorConfig(config);
    }

    @Transactional
    private void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig) {
        configsStorage.setNewsSourceConfig(source, typeAndConfig);
    }

    public void validateAndSetNewsSourceConfig(
        String source, NewsSourceTypeAndConfig typeAndConfig
    ) throws BadConfigException {
        createNewsSource(typeAndConfig);
        setNewsSourceConfig(source, typeAndConfig);
    }

    private Map<String, NewsSource> createNewsSourcesForCollect() {
        Map<String, NewsSource> newsSources = new HashMap<>();

        getNewsSourceConfigs().forEach((name, typeAndConfig) -> {
            try {
                newsSources.put(name, createNewsSource(typeAndConfig));
            } catch (BadConfigException exception) {
                throw new RuntimeException(exception);
            }
        });

        return newsSources;
    }

    @Transactional
    List<Article> getArticlesAfter(long boundId) {
        return articleStorage.getAfter(boundId);
    }

    @PostConstruct
    public void collect() {
        try {
            validateAndSetCollectorConfig("{\"rate\":60,\"maxArticles\":3,\"keywords\":[]}");
        } catch (BadConfigFormatException e) {
            throw new RuntimeException(e);
        }

        try {
            validateAndSetNewsSourceConfig(
                "habr.com",
                new NewsSourceTypeAndConfig(
                    "HTML",
                    "{" +
                        "\"urlWithPageVar\":\"https://habr.com/ru/articles/page{page}\"," +
                        "\"itemSelector\":\".tm-articles-list__item\"," +
                        "\"linkSelector\":\".tm-article-snippet__readmore\"," +
                        "\"titleSelector\":\".tm-title\"," +
                        "\"textSelector\":\".article-formatted-body\"," +
                        "\"timeSelector\":\".tm-article-datetime-published time\"" +
                        "}"
                )
            );
        } catch (BadConfigException e) {
            throw new RuntimeException(e);
        }

        ///////////////////////////////////////

        CollectorConfig collectorConfig;
        try {
            collectorConfig = parseCollectorConfig(getCollectorConfig());
        } catch (BadConfigFormatException e) {
            throw new RuntimeException(e);
        }

        var collector = new Collector(collectorConfig);

        getNewsSourceConfigs().forEach((name, typeAndConfig) -> {
            NewsSource source;
            try {
                source = createNewsSource(typeAndConfig);
            } catch (BadConfigException exception) {
                throw new RuntimeException(exception);
            }

            try {
                collector.collect(name, source, articleStorage);
            } catch (IOException exception) {
                logger.error("", exception);
            }
        });

        taskScheduler.schedule(
            this::collect,
            triggerContext ->
                triggerContext.getClock().instant().plus(Duration.ofSeconds(collectorConfig.rate()))
        );
    }
}
