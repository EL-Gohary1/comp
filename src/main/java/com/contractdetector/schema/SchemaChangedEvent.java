package com.contractdetector.schema;

import com.contractdetector.change.SchemaDiff;
import com.contractdetector.impact.ImpactAnalysisService;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published by {@link SchemaRegistryService} whenever a new
 * schema version is persisted for an API endpoint.
 *
 * <p>{@link ImpactAnalysisService} listens for this event
 * and triggers downstream impact analysis asynchronously.
 *
 * <h3>Event data</h3>
 * <ul>
 *   <li>{@link #oldSchema} — the previously stored schema (may be {@code null} for
 *       the very first version of an endpoint).</li>
 *   <li>{@link #newSchema} — the newly persisted schema version.</li>
 *   <li>{@link #diff}      — the computed differences; not null when
 *       {@code oldSchema != null}.</li>
 * </ul>
 */
@Getter
public class SchemaChangedEvent extends ApplicationEvent {

    /** The schema version that was superseded ; {@code null} for brand-new endpoints. */
    private final ApiSchema oldSchema;

    /** The newly persisted schema version. */
    private final ApiSchema newSchema;

    /**
     * Computed diff between old and new schemas.
     * {@code null} only when this is the first version of the endpoint (no prior schema).
     */
    private final SchemaDiff diff;

    /**
     * @param source    the bean that published this event (usually {@link SchemaRegistryService})
     * @param oldSchema previous schema version, or {@code null}
     * @param newSchema newly saved schema version
     * @param diff      computed diff; {@code null} when there is no prior schema
     */
    public SchemaChangedEvent(Object source, ApiSchema oldSchema, ApiSchema newSchema, SchemaDiff diff) {
        super(source);
        this.oldSchema = oldSchema;
        this.newSchema = newSchema;
        this.diff      = diff;
    }
}
