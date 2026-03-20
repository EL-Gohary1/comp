package com.contractdetector.impact;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticTestAnalyzer {

    private final JavaParser javaParser;

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete"
    );

    public List<TestCase> findAffectedTests(String endpointPattern, Path testSourceDir) {
        List<TestCase> affectedTests = new ArrayList<>();

        try {
            if (!Files.exists(testSourceDir)) {
                log.warn("Test directory not found: {}", testSourceDir);
                return affectedTests;
            }

            List<Path> testFiles = Files.walk(testSourceDir)
                                        .filter(path -> path.toString().endsWith("Test.java"))
                                        .collect(Collectors.toList());

            for (Path testFile : testFiles) {
                try {
                    analyzeTestFile(testFile, endpointPattern, affectedTests);
                } catch (Exception e) {
                    log.warn("Failed to analyze file: {}", testFile, e);
                }
            }

        } catch (IOException e) {
            log.error("Error walking test directory", e);
        }

        return affectedTests;
    }

    private void analyzeTestFile(Path testFile, String endpointPattern, List<TestCase> affectedTests) throws IOException {
        CompilationUnit cu = javaParser.parse(testFile).getResult()
                                       .orElseThrow(() -> new IOException("JavaParser failed to parse file"));

        String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                             .map(c -> c.getNameAsString())
                             .orElse(testFile.getFileName().toString().replace(".java", ""));

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            if (method.isAnnotationPresent("Test")) {

                List<RestAssuredCall> calls = extractRestAssuredCalls(method);

                for (RestAssuredCall call : calls) {
                    if (matchesEndpoint(call.getPath(), endpointPattern)) {

                        affectedTests.add(TestCase.builder()
                                                  .id(UUID.randomUUID().toString())
                                                  .filePath(testFile.toAbsolutePath().toString())
                                                  .className(className)
                                                  .methodName(method.getNameAsString())
                                                  .lineNumber(method.getBegin().map(p -> p.line).orElse(0))
                                                  .endpointPattern(endpointPattern)
                                                  .confidence(TestCase.Confidence.HIGH) // أصبح HIGH لأننا نستخدم Parser
                                                  .httpMethod(call.getHttpMethod())
                                                  .path(call.getPath())
                                                  .assertions(call.getAssertions())
                                                  .jsonPathExpressions(call.getJsonPathExpressions())
                                                  .build());
                        break;
                    }
                }
            }
        });
    }

    private List<RestAssuredCall> extractRestAssuredCalls(MethodDeclaration method) {
        List<RestAssuredCall> calls = new ArrayList<>();

        method.findAll(MethodCallExpr.class).forEach(call -> {
            String methodName = call.getNameAsString().toLowerCase();

            if (HTTP_METHODS.contains(methodName)) {
                String path = extractPathFromCall(call);
                List<String> assertions = extractAssertions(method);
                List<String> jsonPaths = extractJsonPaths(assertions);

                calls.add(RestAssuredCall.builder()
                                         .httpMethod(methodName.toUpperCase())
                                         .path(path)
                                         .assertions(assertions)
                                         .jsonPathExpressions(jsonPaths)
                                         .build());
            }
        });
        return calls;
    }

    private String extractPathFromCall(MethodCallExpr httpCall) {
        return httpCall.getArguments().stream()
                       .filter(arg -> arg instanceof StringLiteralExpr)
                       .map(arg -> ((StringLiteralExpr) arg).getValue())
                       .findFirst()
                       .orElse("");
    }

    private List<String> extractAssertions(MethodDeclaration method) {
        List<String> assertions = new ArrayList<>();
        Set<String> assertionMethods = Set.of("body", "statusCode", "header", "contentType");

        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (assertionMethods.contains(call.getNameAsString())) {
                assertions.add(call.toString());
            }
        });
        return assertions;
    }

    private List<String> extractJsonPaths(List<String> assertions) {
        List<String> jsonPaths = new ArrayList<>();
        for (String assertion : assertions) {
            if (assertion.contains("body(")) {
                String rawPath = assertion.replaceAll(".*body\\s*\\(\\s*\"([^\"]+)\".*", "$1");
                if (!rawPath.equals(assertion)) {
                    jsonPaths.add("$." + rawPath);
                }
            }
        }
        return jsonPaths;
    }

    private boolean matchesEndpoint(String actualPath, String endpointPattern) {
        if (actualPath == null || actualPath.isEmpty()) return false;

        String regex = endpointPattern
                .replaceAll("\\{[^}]+\\}", "[^/]+") // تحويل أي {param} إلى Regex يقبل أي نص
                .replace("/", "\\/");

        return actualPath.matches(regex) || actualPath.contains(endpointPattern.replace("{id}", ""));
    }

    @Data
    @Builder
    private static class RestAssuredCall {
        private String httpMethod;
        private String path;
        private List<String> assertions;
        private List<String> jsonPathExpressions;
    }
}