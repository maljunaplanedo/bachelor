package ru.dbhub.mvc;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.dbhub.PublisherConfig;
import ru.dbhub.PublisherService;

import java.util.Optional;

@RestController
public class PublisherConfigController {
    @Autowired
    private PublisherService publisherService;

    @GetMapping("/config")
    public Optional<PublisherConfig> getPublisherConfig() {
        return publisherService.getPublisherConfig();
    }

    @PostMapping("/config")
    public void setPublisherConfig(@RequestBody @Valid PublisherConfig config) {
        publisherService.setPublisherConfig(config);
    }
}
