package ru.dbhub;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return mapperBuilder -> mapperBuilder
            .featuresToEnable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
    }
}
