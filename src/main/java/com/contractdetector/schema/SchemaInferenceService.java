package com.contractdetector.schema;

import com.contractdetector.capture.ApiResponseSample;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasquatch.jsonschemainferrer.FormatInferrers;
import com.saasquatch.jsonschemainferrer.JsonSchemaInferrer;
import com.saasquatch.jsonschemainferrer.SpecVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaInferenceService {

    private final ObjectMapper objectMapper;
    private final SchemaRegistryService schemaRegistryService;


    public void inferAndRegisterSchema(ApiResponseSample sample) {
        log.info("Inferring schema for: {} {}", sample.getMethod(), sample.getPath());

        String normalizedPath = normalizePath(sample.getPath());
        String generatedSchema = generateSchema(sample.getResponseBody());

        ApiSchema apiSchema = ApiSchema.builder()
                .id(UUID.randomUUID().toString())
                .endpointPath(normalizedPath)
                .httpMethod(sample.getMethod())
                .schemaJson(generatedSchema)
                .sampleId(sample.getId())
                .inferredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        schemaRegistryService.storeSchema(apiSchema);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        // Replace UUIDs with {uuid}
        String uuidRegex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        path = path.replaceAll(uuidRegex, "{uuid}");

        // Replace digit-only IDs with {id}
        String idRegex = "(?<=/)\\d+(?=/|$)";
        path = path.replaceAll(idRegex, "{id}");

        return path;
    }

    private String generateSchema(String responseBody) {
        log.debug("Generating real JSON Schema from response body...");
        try {
            if (responseBody == null || responseBody.isEmpty()) {
                return "{\"type\": \"null\"}";
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            JsonSchemaInferrer localInferrer = JsonSchemaInferrer.newBuilder()
                                                                 .setSpecVersion(SpecVersion.DRAFT_06)
                                                                 .addFormatInferrers(FormatInferrers.dateTime(), FormatInferrers.email())
                                                                 .build();

            JsonNode schemaNode = localInferrer.inferForSample(jsonNode);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaNode);

        } catch (Exception e) {
            log.error("CRITICAL ERROR in Schema Generation: ", e);
            return "{\"$schema\": \"http://json-schema.org/draft-06/schema#\", \"type\": \"object\"}";
        }
    }
}
