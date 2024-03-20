package ru.dbhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CollectorService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    @Lazy
    private CollectorService self;

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
    public Optional<String> getCollectorConfig() {
        return configsStorage.getCollectorConfig();
    }

    @Transactional
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return configsStorage.getNewsSourceConfigs();
    }

    private void doSetCollectorConfig(String config) {
        configsStorage.setCollectorConfig(config);
    }

    private void setCollectorConfig(String config) {
        boolean collectorConfigWasAbsent = configsStorage.getCollectorConfig().isEmpty();
        doSetCollectorConfig(config);
        if (collectorConfigWasAbsent) {
            collect();
        }
    }

    @Transactional
    public void validateAndSetCollectorConfig(String config) throws BadConfigFormatException {
        parseCollectorConfig(config);
        setCollectorConfig(config);
    }

    private void setNewsSourceConfig(String source, NewsSourceTypeAndConfig typeAndConfig) {
        configsStorage.setNewsSourceConfig(source, typeAndConfig);
    }

    @Transactional
    public void validateAndSetNewsSourceConfig(
        String source, NewsSourceTypeAndConfig typeAndConfig
    ) throws BadConfigException {
        createNewsSource(typeAndConfig);
        setNewsSourceConfig(source, typeAndConfig);
    }

    @Transactional
    public List<Article> getArticlesAfter(long boundId) {
        return articleStorage.getAfter(boundId);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    @Transactional
    public void collect() {
        var collectorConfigStringOptional = getCollectorConfig();
        if (collectorConfigStringOptional.isEmpty()) {
            return;
        }

        CollectorConfig collectorConfig;
        try {
            collectorConfig = parseCollectorConfig(collectorConfigStringOptional.get());
        } catch (BadConfigFormatException exception) {
            throw new RuntimeException(exception);
        }

        scheduler.schedule(self::collect, collectorConfig.rate(), TimeUnit.SECONDS);

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
    }
}
