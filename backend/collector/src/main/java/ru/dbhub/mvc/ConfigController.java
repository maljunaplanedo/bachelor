package ru.dbhub.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/collector")
    public void setCollectorConfig(@RequestBody String config) {
        try {
            collectorService.validateAndSetCollectorConfig(config);
        } catch (BadConfigFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_FORMAT.getDetail());
        }
    }

    @GetMapping("/sources")
    public Map<String, NewsSourceTypeAndConfig> getNewsSourceConfigs() {
        return collectorService.getNewsSourceConfigs();
    }

    @PostMapping("/sources")
    public void setNewsSourceConfigs(@RequestBody Map<String, NewsSourceTypeAndConfig> sourceConfigs) {
        try {
            collectorService.validateAndSetNewsSourceConfigs(sourceConfigs);
        } catch (BadConfigFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_FORMAT.getDetail());
        } catch (BadConfigSourceTypeException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_SOURCE_TYPE.getDetail());
        } catch (BadConfigException unreachable) {
            throw new RuntimeException(unreachable);
        }
    }

    @DeleteMapping("/sources")
    public void deleteNewsSourceConfigs(@RequestBody List<String> sourceNames) {
        collectorService.removeNewsSourceConfigs(sourceNames);
    }
}
