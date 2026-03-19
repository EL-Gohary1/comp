package com.contractdetector.capture;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
public class SchemaCaptureFilter implements Filter {

    private final SchemaCaptureService schemaCaptureService;
    
    private static final ThreadLocal<TestContext> testContextHolder = new ThreadLocal<>();

    public SchemaCaptureFilter(SchemaCaptureService schemaCaptureService) {
        this.schemaCaptureService = schemaCaptureService;
    }

    public static void setTestContext(String className, String methodName) {
        TestContext context = new TestContext(className, methodName);
        testContextHolder.set(context);
    }

    public static void clearTestContext() {
        testContextHolder.remove();
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
        Response response = ctx.next(requestSpec, responseSpec);

        try {
            TestContext testContext = testContextHolder.get();
            String testClass = testContext != null ? testContext.getClassName() : "UnknownClass";
            String testMethod = testContext != null ? testContext.getMethodName() : "UnknownMethod";

            ApiResponseSample sample = ApiResponseSample.builder()
                    .id(UUID.randomUUID().toString())
                    .method(requestSpec.getMethod())
                    .url(requestSpec.getURI())
                    .path(requestSpec.getDerivedPath())
                    .requestHeaders(requestSpec.getHeaders().toString())
                    .responseHeaders(response.getHeaders().toString())
                    .responseBody(response.getBody().asString())
                    .statusCode(response.getStatusCode())
                    .contentType(response.getContentType())
                    .timestamp(LocalDateTime.now())
                    .testClassName(testClass)
                    .testMethodName(testMethod)
                    .build();

            schemaCaptureService.captureAsync(sample);
        } catch (Exception e) {
            log.error("Failed to capture API response in filter", e);
        }

        return response;
    }

    @Data
    public static class TestContext {
        private final String className;
        private final String methodName;
    }
}
