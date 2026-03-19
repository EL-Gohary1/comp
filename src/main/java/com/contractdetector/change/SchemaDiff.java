package com.contractdetector.change;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Aggregate result of comparing two JSON Schema versions for a single endpoint.
 *
 * <p>Created by {@link ChangeDetectionService} and attached to each
 * {@link com.contractdetector.schema.SchemaChangedEvent}.
 */
@Getter
@Builder
public class SchemaDiff {
    @Setter
    private String id;
    
    @Setter
    private String endpointPath;
    
    @Setter
    private String httpMethod;
    
    @Setter
    private String oldSchemaId;
    
    @Setter
    private String newSchemaId;

    /**
     * All detected field-level changes, ordered by risk (highest first).
     * Never {@code null}; may be empty when schemas are semantically equivalent
     * but string-representation differs (e.g. key ordering).
     */
    @Builder.Default
    private final List<SchemaChange> changes = Collections.emptyList();

    // ── Convenience queries ──────────────────────────────────────────────────

    /**
     * @return {@code true} if at least one change was detected
     */
    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    /**
     * @return all changes classified as {@link RiskLevel#HIGH}
     */
    public List<SchemaChange> getHighRiskChanges() {
        return filterByRisk(RiskLevel.HIGH);
    }

    /**
     * @return all changes classified as {@link RiskLevel#MEDIUM}
     */
    public List<SchemaChange> getMediumRiskChanges() {
        return filterByRisk(RiskLevel.MEDIUM);
    }

    /**
     * @return all changes classified as {@link RiskLevel#LOW}
     */
    public List<SchemaChange> getLowRiskChanges() {
        return filterByRisk(RiskLevel.LOW);
    }

    /**
     * Produces a concise human-readable summary of all changes.
     *
     * @return multi-line string (one line per change)
     */
    public String toSummary() {
        if (!hasChanges()) return "No schema changes detected.";
        return changes.stream()
            .map(SchemaChange::toSummary)
            .collect(Collectors.joining("\n"));
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private List<SchemaChange> filterByRisk(RiskLevel level) {
        if (changes == null) return Collections.emptyList();
        return changes.stream()
            .filter(c -> c.getRiskLevel() == level)
            .collect(Collectors.toList());
    }
}
