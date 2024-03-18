package ru.dbhub;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class HTMLNewsSource implements NewsSource {
    public record Config(
        String urlWithPageVar,
        String itemSelector,
        String linkSelector,
        String titleSelector,
        String textSelector,
        String timeSelector
    ) {
    }

    private final Config config;

    public HTMLNewsSource(Config config) {
        this.config = config;
    }

    private String extractLinkFromElement(Element element) {
        if (!element.tag().equals(Tag.valueOf("a"))) {
            throw new IllegalArgumentException();
        }
        return element.absUrl("href");
    }

    private long extractTimestampFromElement(Element element) {
        if (!element.tag().equals(Tag.valueOf("time"))) {
            throw new IllegalArgumentException();
        }
        var datetimeStr = element.attr("datetime");
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(datetimeStr)).toEpochMilli() / 1000;
    }

    @Override
    public List<JustCollectedArticle> getArticlesPage(int pageNo) throws IOException {
        var url = new UriTemplate(config.urlWithPageVar).expand(pageNo).toString();

        var htmlDocument = Jsoup.connect(url).get();

        var logger = LoggerFactory.getLogger(getClass());
        logger.error(config.linkSelector());

        return htmlDocument.select(config.itemSelector()).stream()
            .map(item ->
                new JustCollectedArticle(
                    extractLinkFromElement(requireNonNull(item.selectFirst(config.linkSelector()))),
                    requireNonNull(item.selectFirst(config.titleSelector())).text(),
                    requireNonNull(item.selectFirst(config.textSelector())).text(),
                    extractTimestampFromElement(requireNonNull(item.selectFirst(config.timeSelector())))
                )
            )
            .toList();
    }
}
