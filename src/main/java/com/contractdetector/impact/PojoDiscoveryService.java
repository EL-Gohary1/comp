package com.contractdetector.impact;

import com.contractdetector.change.ChangeType;
import com.contractdetector.change.SchemaDiff;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Placeholder implementation that discovers Java POJO classes whose field
 * names overlap with schema properties that were removed or renamed.
 *
 * <h3>Current heuristic</h3>
 * Scans all {@code .java} files under {@code src/main/java}, then checks whether
 * any of the REMOVED field names from the {@link SchemaDiff} appear as word-boundary
 * tokens inside the file (e.g. as a field declaration or getter name).
 *
 * <h3>Extending this service</h3>
 * Replace the text-scan with a JavaParser-based field-declaration visitor to achieve
 * precise field-level matching and avoid false positives from comments or string literals.
 */
@Slf4j
@Service
public class PojoDiscoveryService {

    @Value("${contract-detector.scan.main-sources-root:src/main/java}")
    private String mainSourcesRoot;

    /**
     * Finds main-source Java classes that likely need updating due to the given diff.
     *
     * <p>Only {@link ChangeType#REMOVED} and {@link ChangeType#TYPE_CHANGED} changes
     * are considered, as these represent fields that consumers must actively handle.
     *
     * @param diff the detected schema diff
     * @return list of simple class names (filename-based) that are potentially affected
     */
    public List<String> findAffectedPojos(SchemaDiff diff) {
        List<String> affected = new ArrayList<>();

        // Collect field names from high-risk changes only
        Set<String> removedOrChangedFields = diff.getChanges().stream()
            .filter(c -> c.getChangeType() == ChangeType.REMOVED
                      || c.getChangeType() == ChangeType.TYPE_CHANGED)
            .map(c -> extractFieldName(c.getPath()))
            .filter(name -> name != null && !name.isBlank())
            .collect(Collectors.toSet());

        if (removedOrChangedFields.isEmpty()) {
            log.debug("PojoDiscoveryService: no removed/changed fields — skipping POJO scan");
            return affected;
        }

        Path root = Paths.get(mainSourcesRoot);
        if (!Files.exists(root)) {
            log.debug("PojoDiscoveryService: main sources root not found at '{}' — skipping scan",
                root.toAbsolutePath());
            return affected;
        }

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        List<String> hits = scanFile(file, removedOrChangedFields);
                        affected.addAll(hits);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("PojoDiscoveryService: error scanning main sources: {}", e.getMessage(), e);
        }

        return affected;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the leaf field name from a JSONPath-style schema path.
     * e.g. {@code $.properties.user.properties.email} → {@code email}
     */
    private String extractFieldName(String schemaPath) {
        if (schemaPath == null) return null;
        String[] parts = schemaPath.split("\\.");
        return parts[parts.length - 1];
    }

    private List<String> scanFile(Path file, Set<String> fieldNames) {
        List<String> results = new ArrayList<>();
        try {
            String content   = Files.readString(file);
            String className = file.getFileName().toString().replace(".java", "");

            for (String field : fieldNames) {
                // Word-boundary match — avoids matching "id" inside "valid"
                if (content.matches("(?s).*\\b" + field + "\\b.*")) {
                    results.add(className + "#" + field
                        + " (field reference — verify POJO and getter/setter)");
                    log.debug("PojoDiscoveryService: flagged {} for field '{}'",
                        file.toAbsolutePath(), field);
                    break;  // flag file once even if multiple fields match
                }
            }
        } catch (IOException e) {
            log.warn("PojoDiscoveryService: could not read file {}: {}", file, e.getMessage());
        }
        return results;
    }
}
