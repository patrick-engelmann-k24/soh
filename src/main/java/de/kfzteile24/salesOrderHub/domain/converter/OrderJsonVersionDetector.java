package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderJsonVersionDetector {

    @NonNull
    private final ObjectMapper objectMapper;

    public String detectVersion(String orderJson) throws JsonProcessingException {
        final var version = objectMapper.readValue(orderJson, VersionExtractor.class).getVersion();

        if (version == null) {
            throw new IllegalArgumentException("The provided JSON does not contain a version");
        } else {
            return version;
        }
    }

    public boolean isVersion3(String orderJson) throws JsonProcessingException {
        return detectVersion(orderJson).startsWith("3.");
    }

    @Data
    private static class VersionExtractor {
        private String version;
    }

}
