package com.contractdetector.change;

/**
 * Classifies the nature of a single field-level schema change.
 */
public enum ChangeType {

    /** A field present in the new schema that did not exist in the old schema. */
    ADDED,

    /** A field present in the old schema that no longer exists in the new schema. */
    REMOVED,

    /**
     * A field that exists in both schemas but whose declared JSON type has changed
     * (e.g. {@code "integer"} → {@code "string"}).
     */
    TYPE_CHANGED,

    /**
     * A field's presence in the {@code required} array has changed
     * (either promoted to required or made optional).
     */
    REQUIRED_CHANGED,

    /**
     * A validation constraint on an existing field has changed
     * (e.g. {@code maxLength}, {@code minimum}, {@code pattern}).
     */
    CONSTRAINT_CHANGED,

    /**
     * The structure of a nested object or array element has changed beyond a
     * simple type change (recursive structural drift).
     */
    STRUCTURE_CHANGED
}
