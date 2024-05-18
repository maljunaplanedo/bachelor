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

import static java.util.Objects.requireNonNull;

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
        @Nullable Integer maxPage,
        boolean useLinkForTitle,
        boolean useLinkForText,
        boolean useLinkForTime,
        @Nullable String nextPageLinkSelector,
        @Nullable String byDatePagingFormat,
        int attemptsToFindNonEmptyPage,
        boolean removeTimeFromTitleAndText
    ) {
    }

    public class ArticleIOException extends IOException {
        public ArticleIOException(String message) {
            super(makeFullErrorMessage(message));
        }

        public ArticleIOException(String message, Throwable cause) {
            super(makeFullErrorMessage(message), cause);
        }
    }

    private final Config config;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String pageUrl;

    private Integer articleIdx;

    private LocalDate dateForByDatePaging;

    public HTMLNewsSource(Config config) {
        super(config.maxPage());
        this.config = config;
        this.dateForByDatePaging = config.byDatePagingFormat() == null
            ? null
            : LocalDate.now(ZoneId.of(requireNonNull(config.timeZone())));
    }

    private String makeFullErrorMessage(String message) {
        var fullMessageBuilder = new StringBuilder(message + ", page " + pageUrl);
        if (articleIdx != null) {
            fullMessageBuilder.append(", article index ").append(articleIdx);
        }
        return fullMessageBuilder.toString();
    }

    private <T> T throwBadFormatIfNull(@Nullable T value, String itemName) throws ArticleIOException {
        if (value == null) {
            throw new ArticleIOException(itemName + " is absent");
        }
        return value;
    }

    private String extractLink(Element articleHtml) throws ArticleIOException {
        var linkElement = throwBadFormatIfNull(articleHtml.selectFirst(config.linkSelector()), "Link");
        if (!linkElement.tag().equals(Tag.valueOf("a"))) {
            throw new ArticleIOException("Link does not have tag \"a\"");
        }
        return linkElement.absUrl("href");
    }

    private static DateTimeFormatter makeDateTimeFormatter(String timeFormat) {
        if ("<ISO>".equals(timeFormat)) {
            return DateTimeFormatter.ISO_DATE_TIME;
        }
        return DateTimeFormatter.ofPattern(timeFormat);
    }

    private String extractRawTime(Element articleHtml) throws ArticleIOException {
        var timeElement = throwBadFormatIfNull(articleHtml.selectFirst(config.timeSelector()), "Time");
        if (config.usesTimeTag()) {
            return timeElement.attr("datetime");
        }
        return timeElement.text();
    }

    private long extractTimestamp(Element articleHtml) throws ArticleIOException {
        try {
            var dateTimeFormatter = makeDateTimeFormatter(config.timeFormat());

            var rawTime = extractRawTime(articleHtml);

            if (config.containsRussianMonthNameGen()) {
                rawTime = rawTime
                    .toLowerCase()
                    .replace("января", "01")
                    .replace("февраля", "02")
                    .replace("марта", "03")
                    .replace("апреля", "04")
                    .replace("мая", "05")
                    .replace("июня", "06")
                    .replace("июля", "07")
                    .replace("августа", "08")
                    .replace("сентября", "09")
                    .replace("октября", "10")
                    .replace("ноября", "11")
                    .replace("декабря", "12");
            }

            var temporalAccessor = dateTimeFormatter.parse(rawTime, new ParsePosition(0));

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
            throw new ArticleIOException("Time has wrong format", exception);
        }
    }

    private String removeTimeFromTitleOrTextIfNeeded(Element articleHtml, String text) throws ArticleIOException {
        return config.removeTimeFromTitleAndText() ? text.replace(extractRawTime(articleHtml), "") : text;
    }

    private String extractText(Element articleHtml) throws ArticleIOException {
        return removeTimeFromTitleOrTextIfNeeded(
            articleHtml,
            throwBadFormatIfNull(articleHtml.selectFirst(config.textSelector()), "Text").text()
        ).strip();
    }

    private String extractTitle(Element articleHtml) throws ArticleIOException {
        String title;
        if (config.titleSelector() != null) {
            title = throwBadFormatIfNull(articleHtml.selectFirst(config.titleSelector()), "Title").text();
        } else {
            title = extractText(articleHtml).split("[.!?\\n]| https://")[0];
        }
        return removeTimeFromTitleOrTextIfNeeded(articleHtml, title).strip();
    }

    private void computePageUrlIfNeeded() {
        if (pageUrl == null) {
            if (config.nextPageLinkSelector() != null && getPageNo() > 1) {
                return;
            }

            String pageForUrl;
            if (dateForByDatePaging == null) {
                pageForUrl = String.valueOf(getPageNo());
            } else {
                pageForUrl = makeDateTimeFormatter(requireNonNull(config.byDatePagingFormat()))
                    .format(dateForByDatePaging);
            }

            pageUrl = new UriTemplate(config.urlWithPageVar()).expand(pageForUrl).toString();
        }
    }

    private @Nullable String extractNextPageUrlIfNeeded(Element articlesListHtml) throws ArticleIOException {
        if (config.nextPageLinkSelector() != null) {
            var nextPageLink = articlesListHtml.selectFirst(config.nextPageLinkSelector());
            return nextPageLink == null ? null : nextPageLink.absUrl("href");
        }
        return null;
    }

    private Element loadPageHtml(String url) throws ArticleIOException {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException ioException) {
            throw new ArticleIOException("Failed to load page", ioException);
        }
    }

    public List<JustCollectedArticle> doNextArticlesPage() throws IOException {
        computePageUrlIfNeeded();
        if (pageUrl == null) {
            return List.of();
        }

        var articlesListHtml = loadPageHtml(pageUrl);

        List<JustCollectedArticle> articles = new ArrayList<>();
        var articleHtmls = articlesListHtml.select(config.itemSelector());

        long exceptionsCount = 0;
        for (articleIdx = 0; articleIdx < articleHtmls.size(); ++articleIdx) {
            try {
                var articleHtml = articleHtmls.get(articleIdx);

                var link = extractLink(articleHtml);

                Element articleHtmlFromLink = null;
                if (config.useLinkForTitle() || config.useLinkForText() || config.useLinkForTime()) {
                    articleHtmlFromLink = loadPageHtml(link);
                }

                articles.add(new JustCollectedArticle(
                    link,
                    config.useLinkForTitle() ? extractTitle(articleHtmlFromLink) : extractTitle(articleHtml),
                    config.useLinkForText() ? extractText(articleHtmlFromLink) : extractText(articleHtml),
                    config.useLinkForTime() ? extractTimestamp(articleHtmlFromLink) : extractTimestamp(articleHtml)
                ));
            } catch (ArticleIOException articleIOException) {
                logger.error("Article parsing failed:", articleIOException);
                ++exceptionsCount;
                if (2 * exceptionsCount >= articleHtmls.size()) {
                    throw articleIOException;
                }
            }
        }
        articleIdx = null;

        if (dateForByDatePaging != null) {
            dateForByDatePaging = dateForByDatePaging.plusDays(-1);
        }

        pageUrl = extractNextPageUrlIfNeeded(articlesListHtml);

        return articles;
    }

    @Override
    public List<JustCollectedArticle> nextArticlesPageImpl() throws IOException {
        for (int attempt = 0; attempt < config.attemptsToFindNonEmptyPage() + 1; ++attempt) {
            var result = doNextArticlesPage();
            if (!result.isEmpty()) {
                return result;
            }
        }
        return List.of();
    }
}
