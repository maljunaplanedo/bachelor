package ru.dbhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CollectorService {
    private static final long NO_BOUND_ARTICLE_ID = -1;

    private static final long ARTICLE_ID_BEFORE_ALL = 0;

    private static final long ARTICLE_ID_AFTER_ALL = Long.MAX_VALUE;

    private static final long RETRY_FIND_CONFIGS_RATE = 60;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private Validator validator;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    @Lazy
    private CollectorService self;

    @Autowired
    private ConfigsStorage configsStorage;

    @Autowired
    private ArticleStorage articleStorage;

    @Autowired
    private CollectSynchronizer collectSynchronizer;

    private <C> C parseConfig(JsonNode configJson, Class<C> configClass) throws BadConfigFormatException {
        C result;
        try {
            result = jsonMapper.treeToValue(configJson, configClass);
        } catch (JsonProcessingException exception) {
            throw new BadConfigFormatException();
        }

        if (result == null || !validator.validate(result).isEmpty()) {
            throw new BadConfigFormatException();
        }

        return result;
    }

    private CollectorConfig parseCollectorConfig(JsonNode config) throws BadConfigFormatException {
        return parseConfig(config, CollectorConfig.class);
    }

    private NewsSource createNewsSource(NewsSourceConfig sourceConfig) throws BadConfigException {
        Class<?> sourceClass;
        try {
            var packageName = getClass().getPackageName();
            sourceClass = Class.forName(
                packageName + (packageName.isEmpty() ? "" : ".") + sourceConfig.type() + "NewsSource"
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

        var parsedConfig = parseConfig(sourceConfig.config(), sourceConfigClass);

        try {
            return (NewsSource) sourceConstructorOfConfig.newInstance(parsedConfig);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new BadConfigSourceTypeException();
        }
    }

    @Transactional
    public Optional<JsonNode> getCollectorConfig() {
        return configsStorage.getCollectorConfig();
    }

    @Transactional
    public Map<String, NewsSourceConfig> getNewsSourceConfigs() {
        return configsStorage.getNewsSourceConfigs();
    }

    private void setCollectorConfig(JsonNode config) {
        configsStorage.setCollectorConfig(config);
    }

    @Transactional
    public void validateAndSetCollectorConfig(JsonNode config) throws BadConfigFormatException {
        parseCollectorConfig(config);
        setCollectorConfig(config);
    }

    private void setNewsSourceConfig(String source, NewsSourceConfig sourceConfig) {
        configsStorage.setNewsSourceConfig(source, sourceConfig);
    }

    @Transactional
    public void validateAndSetNewsSourceConfigs(
        Map<String, NewsSourceConfig> sourceConfigs,
        boolean removeOld
    ) throws BadConfigException {
        if (removeOld) {
            configsStorage.removeAllNewsSourceConfigs();
        }

        for (var sourceNameToConfig : sourceConfigs.entrySet()) {
            createNewsSource(sourceNameToConfig.getValue());
            setNewsSourceConfig(sourceNameToConfig.getKey(), sourceNameToConfig.getValue());
        }
    }

    @Transactional
    public void removeNewsSourceConfigs(List<String> sourceNames) {
        sourceNames.forEach(configsStorage::removeNewsSourceConfig);
    }

    private long getCurrentBoundArticleId() {
        return articleStorage.getMaxId().orElse(0L);
    }

    @Transactional
    public ArticlesAndBoundId getArticlesAfter(long boundId, int limit) {
        var articles = articleStorage.getAfter(boundId == NO_BOUND_ARTICLE_ID ? ARTICLE_ID_BEFORE_ALL : boundId, limit);
        return new ArticlesAndBoundId(
            articles,
            articles.isEmpty() ? getCurrentBoundArticleId() : articles.getLast().id()
        );
    }

    @Transactional
    public ArticlesAndBoundId getArticlesPage(long boundId, int page, int count) {
        return new ArticlesAndBoundId(
            articleStorage.getPage(boundId == NO_BOUND_ARTICLE_ID ? ARTICLE_ID_AFTER_ALL : boundId, page, count),
            boundId == NO_BOUND_ARTICLE_ID ? getCurrentBoundArticleId() : boundId
        );
    }

    private void scheduleCollect(long delay) {
        scheduler.schedule(
            () -> {
                try {
                    self.collect();
                } catch (Exception exception) {
                    logger.error("Collect threw an exception", exception);
                }
            },
            delay,
            TimeUnit.SECONDS
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void collectAsync() {
        scheduleCollect(0);
    }

    @Transactional
    public void collect() {
        var collectorConfigStringOptional = getCollectorConfig();
        if (collectorConfigStringOptional.isEmpty()) {
            scheduleCollect(RETRY_FIND_CONFIGS_RATE);
            return;
        }

        CollectorConfig collectorConfig;
        try {
            collectorConfig = parseCollectorConfig(collectorConfigStringOptional.get());
        } catch (BadConfigFormatException exception) {
            throw new RuntimeException(exception);
        }

        scheduleCollect(collectorConfig.rate());

        if (!collectSynchronizer.shouldCollect()) {
            logger.info("Not collecting news because the synchronizer decided so");
            return;
        }

        logger.info("Starting to collect news");

        var collector = new Collector(collectorConfig, articleStorage);

        getNewsSourceConfigs().forEach((name, sourceConfig) -> {
            NewsSource source;
            try {
                source = createNewsSource(sourceConfig);
            } catch (BadConfigException exception) {
                throw new RuntimeException(exception);
            }

            try {
                collector.collect(name, source, sourceConfig.requiresFiltering());
            } catch (IOException exception) {
                logger.error("IOException trying to collect news from source {}", name, exception);
            }
        });

        logger.info("Finished collecting news");
    }
}
