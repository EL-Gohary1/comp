package com.contractdetector.change;

/**
 * Risk classification for a detected schema change.
 *
 * <p>Risk is determined by {@link ChangeDetectionService} based on the
 * {@link ChangeType} and the schema position of the changed field:
 *
 * <table border="1">
 *   <tr><th>ChangeType</th><th>Default Risk</th><th>Rationale</th></tr>
 *   <tr><td>REMOVED</td><td>HIGH</td>
 *       <td>Consumers expecting the field will break at runtime.</td></tr>
 *   <tr><td>TYPE_CHANGED</td><td>HIGH</td>
 *       <td>Deserialisation will throw or silently corrupt data.</td></tr>
 *   <tr><td>REQUIRED_CHANGED</td><td>MEDIUM</td>
 *       <td>Optional→required may cause validation failures in POST/PUT tests.</td></tr>
 *   <tr><td>STRUCTURE_CHANGED</td><td>MEDIUM</td>
 *       <td>Nested POJOs and JsonPath assertions may need updating.</td></tr>
 *   <tr><td>ADDED</td><td>LOW</td>
 *       <td>Backward-compatible in most consumer scenarios.</td></tr>
 *   <tr><td>CONSTRAINT_CHANGED</td><td>LOW</td>
 *       <td>Only impacts boundary-value tests.</td></tr>
 * </table>
 */
public enum RiskLevel {

    /** Breaking change — immediate investigation required. */
    HIGH,

    /** Non-breaking but potentially impactful — review recommended. */
    MEDIUM,

    /** Backward-compatible change — informational only. */
    LOW
}
