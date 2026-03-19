package com.contractdetector.impact;

import com.contractdetector.change.SchemaDiff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder implementation that scans the project's {@code src/test/java} directory
 * for test source files that reference the changed endpoint path.
 *
 * <h3>Current heuristic</h3>
 * A test class is flagged as "affected" if its source file contains a literal string
 * matching the {@code endpointPath} (or key segments of it).  This is an intentionally
 * simple first pass; a future version should use a proper AST parser (e.g. JavaParser)
 * to resolve method-level references.
 *
 * <h3>Extending this service</h3>
 * Replace the {@link #scanFile} method with a JavaParser-based visitor that resolves
 * RestAssured {@code .when().get("/path")} call chains to achieve method-level precision.
 */
@Slf4j
@Service
public class StaticTestAnalyzer {

    @Value("${contract-detector.scan.test-sources-root:src/test/java}")
    private String testSourcesRoot;

    /**
     * Scans test sources for files referencing the given endpoint path.
     *
     * @param endpointPath normalised endpoint path (e.g. {@code /api/users/{id}})
     * @param diff         the detected schema diff (unused by the heuristic but
     *                     available for future refined scanning)
     * @return list of {@code ClassName#methodName} descriptors (best-effort)
     */
    public List<String> findAffectedTests(String endpointPath, SchemaDiff diff) {
        List<String> affected = new ArrayList<>();

        Path root = Paths.get(testSourcesRoot);
        if (!Files.exists(root)) {
            log.debug("StaticTestAnalyzer: test sources root not found at '{}' — skipping scan",
                root.toAbsolutePath());
            return affected;
        }

        // Build a set of search tokens derived from the normalised path
        // e.g. /api/users/{id}  → ["api", "users"]
        List<String> tokens = buildSearchTokens(endpointPath);

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        List<String> hits = scanFile(file, tokens);
                        affected.addAll(hits);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("StaticTestAnalyzer: error scanning test sources: {}", e.getMessage(), e);
        }

        return affected;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<String> buildSearchTokens(String endpointPath) {
        List<String> tokens = new ArrayList<>();
        for (String segment : endpointPath.split("/")) {
            // Skip empty segments and generic placeholders
            if (!segment.isBlank() && !segment.startsWith("{")) {
                tokens.add(segment);
            }
        }
        return tokens;
    }

    private List<String> scanFile(Path file, List<String> tokens) {
        List<String> results = new ArrayList<>();
        try {
            String content   = Files.readString(file);
            String className = file.getFileName().toString().replace(".java", "");

            boolean fileMatches = tokens.stream().anyMatch(content::contains);
            if (fileMatches) {
                // Placeholder: flag the whole class; a real impl would resolve method names
                results.add(className + " (path reference found — review required)");
                log.debug("StaticTestAnalyzer: flagged {}", file.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("StaticTestAnalyzer: could not read file {}: {}", file, e.getMessage());
        }
        return results;
    }
}
