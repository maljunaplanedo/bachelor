package ru.dbhub;

import jakarta.validation.constraints.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.text.ParsePosition;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

class HTMLNewsSource extends PageLimitedNewsSource {
    public record Config(
        @NotNull String urlWithPageVar,
        @NotNull String itemSelector,
        @NotNull String linkSelector,
        @Nullable String titleSelector,
        @NotNull String textSelector,
        @NotNull String timeSelector,
        @NotNull String timeFormat,
        @Nullable String timeZone,
        boolean usesTimeTag,
        boolean containsRussianMonthNameGen,
        @NotNull Integer maxPage,
        boolean useLinkForItemInfo,
        @Nullable String nextPageLinkSelector
    ) {
    }

    public class BadArticleFormatException extends IOException {
        public BadArticleFormatException(String message) {
            super(makeFullErrorMessage(message));
        }

        public BadArticleFormatException(String message, Throwable cause) {
            super(makeFullErrorMessage(message), cause);
        }
    }

    private final Config config;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String pageUrl;

    private Integer articleIdx;

    public HTMLNewsSource(Config config) {
        super(config.maxPage());
        this.config = config;
    }

    private String makeFullErrorMessage(String message) {
        var fullMessageBuilder = new StringBuilder(message + ", page " + pageUrl);
        if (articleIdx != null) {
            fullMessageBuilder.append(", article index ").append(articleIdx);
        }
        return fullMessageBuilder.toString();
    }

    private <T> T throwBadFormatIfNull(@Nullable T value, String itemName) throws BadArticleFormatException {
        if (value == null) {
            throw new BadArticleFormatException(itemName + " is absent");
        }
        return value;
    }

    private String extractLink(Element articleHtml) throws BadArticleFormatException {
        var linkElement = throwBadFormatIfNull(articleHtml.selectFirst(config.linkSelector()), "Link");
        if (!linkElement.tag().equals(Tag.valueOf("a"))) {
            throw new BadArticleFormatException("Link does not have tag \"a\", page " + pageUrl + ",");
        }
        return linkElement.absUrl("href");
    }

    private long extractTimestamp(Element articleHtml) throws BadArticleFormatException {
        var timeElement = throwBadFormatIfNull(articleHtml.selectFirst(config.timeSelector()), "Time");
        try {
            DateTimeFormatter dateTimeFormatter;
            if ("<ISO>".equals(config.timeFormat())) {
                dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
            } else {
                dateTimeFormatter = DateTimeFormatter.ofPattern(config.timeFormat());
            }

            String dateTimeStr;
            if (config.usesTimeTag()) {
                dateTimeStr = timeElement.attr("datetime");
            } else {
                dateTimeStr = timeElement.text();
            }

            if (config.containsRussianMonthNameGen()) {
                dateTimeStr = dateTimeStr
                    .replace("Января", "01")
                    .replace("Февраля", "02")
                    .replace("Марта", "03")
                    .replace("Апреля", "04")
                    .replace("Мая", "05")
                    .replace("Июня", "06")
                    .replace("Июля", "07")
                    .replace("Августа", "08")
                    .replace("Сентября", "09")
                    .replace("Октября", "10")
                    .replace("Ноября", "11")
                    .replace("Декабря", "12");
            }

            var temporalAccessor = dateTimeFormatter.parse(dateTimeStr, new ParsePosition(0));

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
        } catch (DateTimeException exception) {
            throw new BadArticleFormatException("Time has wrong format", exception);
        }
    }

    private String extractText(Element articleHtml) throws BadArticleFormatException {
        return throwBadFormatIfNull(articleHtml.selectFirst(config.textSelector()), "Text").text();
    }

    private String extractTitle(Element articleHtml) throws BadArticleFormatException {
        if (config.titleSelector() != null) {
            return throwBadFormatIfNull(articleHtml.selectFirst(config.titleSelector()), "Title").text();
        }
        return extractText(articleHtml).split("[.!?]")[0];
    }

    private void computePageUrlIfNeeded() {
        if (pageUrl == null) {
            pageUrl = new UriTemplate(config.urlWithPageVar()).expand(getPageNo()).toString();
        }
    }

    private @Nullable String extractNextPageUrlIfNeeded(Element articlesListHtml) throws BadArticleFormatException {
        if (config.nextPageLinkSelector() != null) {
            return throwBadFormatIfNull(
                articlesListHtml.selectFirst(config.nextPageLinkSelector()),
                "Next page url"
            ).absUrl("href");
        }
        return null;
    }

    @Override
    public List<JustCollectedArticle> nextArticlesPageImpl() throws IOException {
        computePageUrlIfNeeded();

        var articlesListHtml = Jsoup.connect(pageUrl).get();

        List<JustCollectedArticle> articles = new ArrayList<>();
        var articleHtmls = articlesListHtml.select(config.itemSelector());

        long exceptionsCount = 0;
        for (articleIdx = 0; articleIdx < articleHtmls.size(); ++articleIdx) {
            try {
                var articleHtml = articleHtmls.get(articleIdx);

                var link = extractLink(articleHtml);
                if (config.useLinkForItemInfo()) {
                    articleHtml = Jsoup.connect(link).get();
                }

                articles.add(new JustCollectedArticle(
                    link,
                    extractTitle(articleHtml),
                    extractText(articleHtml),
                    extractTimestamp(articleHtml)
                ));
            } catch (BadArticleFormatException badArticleFormatException) {
                logger.error("Bad article format:", badArticleFormatException);
                ++exceptionsCount;
                if (2 * exceptionsCount >= articleHtmls.size()) {
                    throw badArticleFormatException;
                }
            }
        }
        articleIdx = null;

        pageUrl = extractNextPageUrlIfNeeded(articlesListHtml);

        return articles;
    }
}
