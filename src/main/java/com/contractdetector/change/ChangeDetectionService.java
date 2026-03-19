package com.contractdetector.change;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Computes a structural diff between two JSON Schema documents and produces a
 * {@link SchemaDiff} containing one {@link SchemaChange} per detected difference.
 *
 * <h3>Algorithm</h3>
 * The diff walks the {@code properties} nodes of both schemas recursively.
 * For each field it detects:
 * <ul>
 *   <li>Fields only in the new schema   → {@link ChangeType#ADDED}</li>
 *   <li>Fields only in the old schema   → {@link ChangeType#REMOVED}</li>
 *   <li>Same field, different type      → {@link ChangeType#TYPE_CHANGED}</li>
 *   <li>Required-ness flipped           → {@link ChangeType#REQUIRED_CHANGED}</li>
 *   <li>Nested-object structural change → {@link ChangeType#STRUCTURE_CHANGED}</li>
 * </ul>
 *
 * <h3>Risk assignment</h3>
 * <ul>
 *   <li>REMOVED, TYPE_CHANGED          → {@link RiskLevel#HIGH}</li>
 *   <li>REQUIRED_CHANGED, STRUCTURE_CHANGED → {@link RiskLevel#MEDIUM}</li>
 *   <li>ADDED, CONSTRAINT_CHANGED      → {@link RiskLevel#LOW}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeDetectionService {

    private final ObjectMapper objectMapper;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Computes the diff between two JSON Schema strings.
     *
     * @param oldSchemaJson JSON Schema of the previous version
     * @param newSchemaJson JSON Schema of the current version
     * @return a {@link SchemaDiff} (may be empty if schemas are semantically equal)
     */
    public SchemaDiff diff(String oldSchemaJson, String newSchemaJson) {
        List<SchemaChange> changes = new ArrayList<>();

        try {
            JsonNode oldRoot = objectMapper.readTree(oldSchemaJson);
            JsonNode newRoot = objectMapper.readTree(newSchemaJson);

            diffNodes(oldRoot, newRoot, "$", changes);

        } catch (Exception e) {
            log.error("Failed to parse schemas for diff: {}", e.getMessage(), e);
        }

        SchemaDiff result = SchemaDiff.builder().changes(changes).build();

        if (result.hasChanges()) {
            log.info("Diff complete — {} change(s) found ({} HIGH, {} MEDIUM, {} LOW)",
                changes.size(),
                result.getHighRiskChanges().size(),
                result.getMediumRiskChanges().size(),
                result.getLowRiskChanges().size()
            );
        }

        return result;
    }

    // ── Recursive diff logic ─────────────────────────────────────────────────

    private void diffNodes(JsonNode oldNode, JsonNode newNode,
                           String   path,   List<SchemaChange> changes) {

        // ── Handle type-level diffs ──────────────────────────────────────────
        String oldType = getType(oldNode);
        String newType = getType(newNode);

        if (!oldType.equals(newType)) {
            changes.add(SchemaChange.builder()
                .path(path)
                .changeType(ChangeType.TYPE_CHANGED)
                .riskLevel(RiskLevel.HIGH)
                .oldValue(oldType)
                .newValue(newType)
                .build());
            return;   // no point diving into properties of incompatible types
        }

        // ── Properties diff (objects only) ───────────────────────────────────
        JsonNode oldProps = oldNode.path("properties");
        JsonNode newProps = newNode.path("properties");

        if (!oldProps.isMissingNode() || !newProps.isMissingNode()) {

            // Fields present in old but removed in new → REMOVED (HIGH)
            diffFieldsRemoved(oldProps, newProps, path, changes);

            // Fields present in new but added in old → ADDED (LOW)
            diffFieldsAdded(oldProps, newProps, path, changes);

            // Fields present in both → recurse + check type changes
            diffFieldsCommon(oldProps, newProps, path, changes);
        }

        // ── Required array diff ──────────────────────────────────────────────
        diffRequired(oldNode, newNode, path, changes);
    }

    private void diffFieldsRemoved(JsonNode oldProps, JsonNode newProps,
                                   String   basePath, List<SchemaChange> changes) {
        if (oldProps.isMissingNode()) return;

        Iterator<Map.Entry<String, JsonNode>> it = oldProps.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldName = entry.getKey();
            if (newProps.isMissingNode() || !newProps.has(fieldName)) {
                changes.add(SchemaChange.builder()
                    .path(basePath + ".properties." + fieldName)
                    .changeType(ChangeType.REMOVED)
                    .riskLevel(RiskLevel.HIGH)
                    .oldValue(getType(entry.getValue()))
                    .newValue(null)
                    .build());
            }
        }
    }

    private void diffFieldsAdded(JsonNode oldProps, JsonNode newProps,
                                  String   basePath, List<SchemaChange> changes) {
        if (newProps.isMissingNode()) return;

        Iterator<Map.Entry<String, JsonNode>> it = newProps.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldName = entry.getKey();
            if (oldProps.isMissingNode() || !oldProps.has(fieldName)) {
                changes.add(SchemaChange.builder()
                    .path(basePath + ".properties." + fieldName)
                    .changeType(ChangeType.ADDED)
                    .riskLevel(RiskLevel.LOW)
                    .oldValue(null)
                    .newValue(getType(entry.getValue()))
                    .build());
            }
        }
    }

    private void diffFieldsCommon(JsonNode oldProps, JsonNode newProps,
                                   String   basePath, List<SchemaChange> changes) {
        if (oldProps.isMissingNode() || newProps.isMissingNode()) return;

        Iterator<Map.Entry<String, JsonNode>> it = oldProps.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String fieldName = entry.getKey();
            if (newProps.has(fieldName)) {
                JsonNode oldField = entry.getValue();
                JsonNode newField = newProps.get(fieldName);

                // Recurse into nested objects
                if ("object".equals(getType(oldField))) {
                    diffNodes(oldField, newField,
                        basePath + ".properties." + fieldName, changes);
                } else {
                    // Leaf-level type check
                    String oldT = getType(oldField);
                    String newT = getType(newField);
                    if (!oldT.equals(newT)) {
                        changes.add(SchemaChange.builder()
                            .path(basePath + ".properties." + fieldName)
                            .changeType(ChangeType.TYPE_CHANGED)
                            .riskLevel(RiskLevel.HIGH)
                            .oldValue(oldT)
                            .newValue(newT)
                            .build());
                    }
                }
            }
        }
    }

    private void diffRequired(JsonNode oldNode, JsonNode newNode,
                               String   basePath, List<SchemaChange> changes) {
        JsonNode oldReq = oldNode.path("required");
        JsonNode newReq = newNode.path("required");

        if (oldReq.isMissingNode() && newReq.isMissingNode()) return;

        List<String> oldRequired = toList(oldReq);
        List<String> newRequired = toList(newReq);

        // Fields promoted to required
        for (String field : newRequired) {
            if (!oldRequired.contains(field)) {
                changes.add(SchemaChange.builder()
                    .path(basePath + ".required." + field)
                    .changeType(ChangeType.REQUIRED_CHANGED)
                    .riskLevel(RiskLevel.MEDIUM)
                    .oldValue("optional")
                    .newValue("required")
                    .build());
            }
        }

        // Fields demoted from required
        for (String field : oldRequired) {
            if (!newRequired.contains(field)) {
                changes.add(SchemaChange.builder()
                    .path(basePath + ".required." + field)
                    .changeType(ChangeType.REQUIRED_CHANGED)
                    .riskLevel(RiskLevel.MEDIUM)
                    .oldValue("required")
                    .newValue("optional")
                    .build());
            }
        }
    }

    // ── Utility helpers ───────────────────────────────────────────────────────

    private String getType(JsonNode node) {
        if (node == null || node.isMissingNode()) return "unknown";
        JsonNode typeNode = node.get("type");
        return typeNode != null ? typeNode.asText("unknown") : "unknown";
    }

    private List<String> toList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            arrayNode.forEach(n -> result.add(n.asText()));
        }
        return result;
    }
}
