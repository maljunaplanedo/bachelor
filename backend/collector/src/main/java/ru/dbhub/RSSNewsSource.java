package ru.dbhub;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class RSSNewsSource extends PageLimitedNewsSource {
    public record Config(
        @NotNull String url,
        @NotNull Boolean isPaged,
        @NotNull Integer maxPage
    ) {
    }

    private final SyndFeedInput syndFeedInput = new SyndFeedInput();

    private final Config config;

    public RSSNewsSource(Config config) {
        super(config.maxPage());
        this.config = config;
    }

    private List<SyndEntry> getEntries(int pageNo) throws IOException {
        if (!config.isPaged() && pageNo > 1) {
            return List.of();
        }

        var urlBuilder = UriComponentsBuilder.fromHttpUrl(config.url);
        if (config.isPaged()) {
            urlBuilder.queryParam("paged", pageNo);
        }
        var url = urlBuilder.build().toUri().toURL();

        SyndFeed syndFeed;
        try {
            syndFeed = syndFeedInput.build(new XmlReader(url));
        } catch (FeedException exception) {
            throw new IOException(exception);
        }

        List<SyndEntry> entries = new ArrayList<>();
        for (var entryObj : syndFeed.getEntries()) {
            entries.add((SyndEntry) entryObj);
        }

        return entries;
    }

    private JustCollectedArticle entryToArticle(SyndEntry entry) {
        return new JustCollectedArticle(
            entry.getLink(),
            entry.getTitle(),
            entry.getDescription().getValue(),
            entry.getPublishedDate().getTime() / 1000
        );
    }

    @Override
    public List<JustCollectedArticle> doGetArticlesPage(int pageNo) throws IOException {
        return getEntries(pageNo).stream()
            .map(this::entryToArticle)
            .toList();
    }
}
