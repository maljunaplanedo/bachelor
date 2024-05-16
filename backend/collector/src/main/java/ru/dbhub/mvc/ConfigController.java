package ru.dbhub.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    public JsonNode getCollectorConfig() {
        return collectorService.getCollectorConfig().orElse(NullNode.getInstance());
    }

    @PostMapping("/collector")
    public void setCollectorConfig(@RequestBody @NotNull JsonNode config) {
        try {
            collectorService.validateAndSetCollectorConfig(config);
        } catch (BadConfigFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_FORMAT.getDetail());
        }
    }

    @GetMapping("/sources")
    public Map<String, NewsSourceConfig> getNewsSourceConfigs() {
        return collectorService.getNewsSourceConfigs();
    }

    private void doSetNewsSourceConfigs(Map<String, NewsSourceConfig> sourceConfigs, boolean removeOld) {
        try {
            collectorService.validateAndSetNewsSourceConfigs(sourceConfigs, removeOld);
        } catch (BadConfigFormatException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_FORMAT.getDetail());
        } catch (BadConfigSourceTypeException exception) {
            throw new ResponseStatusException(BAD_REQUEST, CollectorConfigError.BAD_SOURCE_TYPE.getDetail());
        } catch (BadConfigException unreachable) {
            throw new RuntimeException(unreachable);
        }
    }

    @PostMapping("/sources")
    public void setNewsSourceConfigs(@RequestBody @Valid Map<String, @NotNull NewsSourceConfig> sourceConfigs) {
        doSetNewsSourceConfigs(sourceConfigs, false);
    }

    @PutMapping("/sources")
    public void resetNewsSourceConfigs(
        @RequestBody @Valid Map<String, @NotNull NewsSourceConfig> sourceConfigs
    ) {
        doSetNewsSourceConfigs(sourceConfigs, true);
    }

    @DeleteMapping("/sources")
    public void deleteNewsSourceConfigs(@RequestBody @Valid List<@NotNull String> sourceNames) {
        collectorService.removeNewsSourceConfigs(sourceNames);
    }
}
