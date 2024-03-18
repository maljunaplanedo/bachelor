package ru.dbhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Component
public class CollectorService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    private Storage<ConfigsAccessor> configsStorage;

    private <C> C parseConfig(String configString, Class<C> configClass) throws BadConfigFormatException {
        try {
            return jsonMapper.readValue(configString, configClass);
        } catch (JsonProcessingException exception) {
            throw new BadConfigFormatException();
        }
    }

    private Collector createCollector(String config) throws BadConfigFormatException {
        return new Collector(parseConfig(config, Collector.Config.class));
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

        var sourceConfigClass = Arrays.stream(sourceClass.getDeclaredClasses())
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

    public String getCollectorConfig() {
        var configsStorageTransaction = configsStorage.getTransaction();
        var result = configsStorageTransaction.get().getCollectorConfig();
        configsStorageTransaction.commit();
        return result;
    }

    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        var configsStorageTransaction = configsStorage.getTransaction();
        var result = configsStorageTransaction.get().getNewsSourceConfigs();
        configsStorageTransaction.commit();
        return result;
    }

    public void validateAndSetCollectorConfig(String config) throws BadConfigFormatException {
        createCollector(config);
        var configsStorageTransaction = configsStorage.getTransaction();
        configsStorageTransaction.get().setCollectorConfig(config);
        configsStorageTransaction.commit();
    }

    public void validateAndSetNewsSourceConfig(
        String source, NewsSourceTypeAndConfig typeAndConfig
    ) throws BadConfigException {
        createNewsSource(typeAndConfig);
        var configsStorageTransaction = configsStorage.getTransaction();
        configsStorageTransaction.get().setNewsSourceConfig(source, typeAndConfig);
        configsStorageTransaction.commit();
    }

    private Collector createCollectorForCollect(ConfigsAccessor configsAccessor) {
        try {
            return createCollector(configsAccessor.getCollectorConfig());
        } catch (BadConfigFormatException exception) {
            throw new RuntimeException(exception);
        }
    }

    private Map<String, NewsSource> createNewsSourcesForCollect(ConfigsAccessor configsAccessor) {
        Map<String, NewsSource> newsSources = new HashMap<>();

        configsAccessor.getNewsSourceConfigs().forEach((name, typeAndConfig) -> {
            try {
                newsSources.put(name, createNewsSource(typeAndConfig));
            } catch (BadConfigException exception) {
                throw new RuntimeException(exception);
            }
        });

        return newsSources;
    }

    @Scheduled(fixedRateString = "${ru.dbhub.collect-rate}")
    public void collect() {
        try {
            validateAndSetCollectorConfig("{\"maxArticles\":3,\"keywords\":[]}");
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

        var configsStorageTransaction = configsStorage.getTransaction();

        var collector = createCollectorForCollect(configsStorageTransaction.get());

        createNewsSourcesForCollect(configsStorageTransaction.get()).forEach((name, source) -> {
            try {
                logger.debug(collector.getArticlesFromSourceSince(source, 0).toString());
            } catch (IOException exception) {
                logger.error("", exception);
            }
        });
    }
}
