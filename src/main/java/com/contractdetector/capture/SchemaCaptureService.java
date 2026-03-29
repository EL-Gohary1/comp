package com.contractdetector.capture;

import com.contractdetector.schema.SchemaInferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaCaptureService {

    private final ObjectMapper objectMapper;
    private final SchemaSampleRepository schemaSampleRepository;
    private final SchemaInferenceService schemaInferenceService;

    //capture the response async
    @Async("captureExecutor")
    public void captureAsync(ApiResponseSample sample) {
        log.debug("Async capture started for {} {}", sample.getMethod(), sample.getPath());
        capture(sample);
    }

    @Transactional
    public void capture(ApiResponseSample sample) {
        if (!shouldCapture(sample.getStatusCode(), sample.getContentType())) {
            log.debug("Skipping capture for status {} and content type {}", sample.getStatusCode(), sample.getContentType());
            return;
        }
        
        log.info("Capturing API response for test: {}.{}", sample.getTestClassName(), sample.getTestMethodName());
        //save to database
        schemaSampleRepository.save(sample);
        //generate the schema from captured response
        schemaInferenceService.inferAndRegisterSchema(sample);
    }

    public boolean shouldCapture(int statusCode, String contentType) {
        return statusCode >= 200 && statusCode < 300 
                && contentType != null 
                && contentType.contains("application/json");
    }
}
