package ru.dbhub;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.util.UriTemplate;

public class TelegramNewsSource extends HTMLNewsSource {
    public record Config(
        @NotNull String channelName
    ) {
    }

    public TelegramNewsSource(Config config) {
        super(new HTMLNewsSource.Config(
            new UriTemplate("https://t.me/s/{channelName}").expand(config.channelName()).toString(),
            ":not(.service_message) .tgme_widget_message_bubble:not(:has(:is(.tgme_widget_message_bubble > .message_media_not_supported_wrap, .tgme_widget_message_poll)))",
            ".tgme_widget_message_date",
            null,
            ".tgme_widget_message_text",
            ".tgme_widget_message_date > time",
            "<ISO>",
            null,
            true,
            false,
            null,
            false,
            false,
            false,
            "link[rel=\"prev\"]",
            null,
            0,
            false
        ));
    }
}
