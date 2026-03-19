package com.contractdetector.change;

import com.contractdetector.impact.ImpactAnalysisService;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a single field-level difference between two JSON Schema versions.
 *
 * <p>Instances are created by {@link ChangeDetectionService} and aggregated into a
 * {@link SchemaDiff}. They are then consumed by
 * {@link ImpactAnalysisService} to determine which POJOs and test classes need
 * to be updated.
 *
 * <p>Example — a field {@code email} changed from {@code string} to {@code null}:
 * <pre>{@code
 * SchemaChange.builder()
 *     .path("$.properties.email")
 *     .changeType(ChangeType.TYPE_CHANGED)
 *     .riskLevel(RiskLevel.HIGH)
 *     .oldValue("string")
 *     .newValue("null")
 *     .build()
 * }</pre>
 */
@Getter
@Builder
@ToString
public class SchemaChange {

    /**
     * JSONPath-style location of the changed field within the schema document.
     * Example: {@code $.properties.address.properties.street}
     */
    private final String path;

    /** The nature of this change. */
    private final ChangeType changeType;

    /** Assessed risk of this change on existing consumers and tests. */
    private final RiskLevel riskLevel;

    /**
     * String representation of the old value (type name, constraint value, etc.).
     * {@code null} for {@link ChangeType#ADDED} changes.
     */
    private final String oldValue;

    /**
     * String representation of the new value.
     * {@code null} for {@link ChangeType#REMOVED} changes.
     */
    private final String newValue;

    /**
     * Human-readable summary combining all fields — useful for log output and reports.
     *
     * @return formatted change description
     */
    public String toSummary() {
        return String.format("[%s][%s] %s | %s → %s",
            riskLevel, changeType, path,
            oldValue != null ? oldValue : "∅",
            newValue != null ? newValue : "∅"
        );
    }
}
