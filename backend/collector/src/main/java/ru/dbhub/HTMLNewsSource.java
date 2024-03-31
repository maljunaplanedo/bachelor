package ru.dbhub;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class HTMLNewsSource extends PageLimitedNewsSource {
    public record Config(
        String urlWithPageVar,
        String itemSelector,
        String linkSelector,
        String titleSelector,
        String textSelector,
        String timeSelector,
        String timeFormat,
        @Nullable String timeZone,
        boolean usesTimeTag,
        int maxPage,
        boolean useLinkForItemInfo
    ) {
    }

    private final Config config;

    public HTMLNewsSource(Config config) {
        super(config.maxPage());
        this.config = config;
    }

    private String extractLinkFromElement(Element element) {
        if (!element.tag().equals(Tag.valueOf("a"))) {
            throw new IllegalArgumentException();
        }
        return element.absUrl("href");
    }

    private long extractTimestampFromElement(Element element) {
        DateTimeFormatter dateTimeFormatter;
        if ("<ISO>".equals(config.timeFormat())) {
            dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        } else {
            dateTimeFormatter = DateTimeFormatter.ofPattern(config.timeFormat());
        }

        String dateTimeStr;
        if (config.usesTimeTag()) {
            dateTimeStr = element.attr("datetime");
        } else {
            dateTimeStr = element.text();
        }

        var temporalAccessor = dateTimeFormatter.parse(dateTimeStr);

        Instant instant;
        if (config.timeZone() == null) {
            instant = ZonedDateTime.from(temporalAccessor).toInstant();
        } else {
            LocalDateTime localDateTime;
            if (temporalAccessor.isSupported(ChronoField.SECOND_OF_DAY)) {
                localDateTime = LocalDateTime.from(temporalAccessor);
            } else {
                localDateTime = LocalDate.from(temporalAccessor).atStartOfDay();
            }

            instant = localDateTime.atZone(ZoneId.of(config.timeZone())).toInstant();
        }

        return instant.getEpochSecond();
    }

    @Override
    public List<JustCollectedArticle> doGetArticlesPage(int pageNo) throws IOException {
        var url = new UriTemplate(config.urlWithPageVar()).expand(pageNo).toString();

        var articlesListHtml = Jsoup.connect(url).get();

        List<JustCollectedArticle> articles = new ArrayList<>();
        for (var articleHtml : articlesListHtml.select(config.itemSelector())) {
            var link = extractLinkFromElement(requireNonNull(articleHtml.selectFirst(config.linkSelector())));

            if (config.useLinkForItemInfo()) {
                articleHtml = Jsoup.connect(link).get();
            }

            articles.add(new JustCollectedArticle(
                link,
                requireNonNull(articleHtml.selectFirst(config.titleSelector())).text(),
                requireNonNull(articleHtml.selectFirst(config.textSelector())).text(),
                extractTimestampFromElement(requireNonNull(articleHtml.selectFirst(config.timeSelector())))
            ));
        }

        return articles;
    }
}
