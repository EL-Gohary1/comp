package com.contractdetector.impact;

import com.contractdetector.change.SchemaDiff;
import com.contractdetector.schema.ApiSchema;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SchemaChangedEvent extends ApplicationEvent {

    private final SchemaDiff schemaDiff;
    private final ApiSchema newSchema;

    public SchemaChangedEvent(Object source, SchemaDiff schemaDiff, ApiSchema newSchema) {
        super(source);
        this.schemaDiff = schemaDiff;
        this.newSchema = newSchema;
    }
}
