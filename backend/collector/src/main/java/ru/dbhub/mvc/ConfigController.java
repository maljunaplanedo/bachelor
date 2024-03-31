package ru.dbhub.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import ru.dbhub.*;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/configs")
public class ConfigController {
    @Autowired
    private CollectorService collectorService;

    @GetMapping("/collector")
    public String getCollectorConfig() {
        return collectorService.getCollectorConfig().orElse("");
    }

    @GetMapping("/sources")
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return collectorService.getNewsSourceConfigs();
    }

    @PostMapping("/collector")
    public void setCollectorConfig(@RequestBody String config) {
        try {
            collectorService.validateAndSetCollectorConfig(config);
        } catch (BadConfigFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Bad config format");
        }
    }

    @PostMapping("/sources")
    public void setNewsSourceConfigs(
        @RequestBody Map<String, NewsSourceTypeAndConfig> sourceConfigs
    ) throws BadConfigException {
        collectorService.validateAndSetNewsSourceConfigs(sourceConfigs);
    }

    @DeleteMapping("/sources")
    public void deleteNewsSourceConfigs(@RequestBody List<String> sourceNames) {
        collectorService.removeNewsSourceConfigs(sourceNames);
    }
}
