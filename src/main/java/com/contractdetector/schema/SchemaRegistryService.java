package com.contractdetector.schema;

import com.contractdetector.change.ChangeDetectionService;
import com.contractdetector.change.SchemaDiff;
import com.contractdetector.impact.SchemaChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaRegistryService {

    private final SchemaRepository schemaRepository;
    private final ChangeDetectionService changeDetectionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void storeSchema(ApiSchema newSchema) {
        Optional<ApiSchema> existingSchemaOpt = schemaRepository
                .findFirstByEndpointPathAndHttpMethodOrderByVersionDesc(
                        newSchema.getEndpointPath(), 
                        newSchema.getHttpMethod()
                );

        if (existingSchemaOpt.isPresent()) {
            ApiSchema existingSchema = existingSchemaOpt.get();

            SchemaDiff diff = changeDetectionService.diff(existingSchema.getSchemaJson(), newSchema.getSchemaJson());

            if (diff.hasChanges()) {
                log.info("Schema changes detected for {} {}. Creating new version.", 
                        newSchema.getHttpMethod(), newSchema.getEndpointPath());
                
                newSchema.setVersion(existingSchema.getVersion() + 1);
                newSchema.setBaselineSchemaId(existingSchema.getId());
                
                diff.setEndpointPath(newSchema.getEndpointPath());
                diff.setHttpMethod(newSchema.getHttpMethod());
                diff.setOldSchemaId(existingSchema.getId());

                schemaRepository.save(newSchema);
                diff.setNewSchemaId(newSchema.getId());

                eventPublisher.publishEvent(new SchemaChangedEvent(this, diff, newSchema));
            } else {
                log.debug("No changes detected for {} {}", newSchema.getHttpMethod(), newSchema.getEndpointPath());
            }
        } else {
            log.info("Recording initial schema version 1 for {} {}", newSchema.getHttpMethod(), newSchema.getEndpointPath());
            newSchema.setVersion(1);
            schemaRepository.save(newSchema);
        }
    }

    public Optional<ApiSchema> getLatestSchema(String endpointPath, String httpMethod) {
        return schemaRepository.findFirstByEndpointPathAndHttpMethodOrderByVersionDesc(endpointPath, httpMethod);
    }

    public List<ApiSchema> getSchemaHistory(String endpointPath, String httpMethod) {
        return schemaRepository.findByEndpointPathAndHttpMethodOrderByVersionDesc(endpointPath, httpMethod);
    }
}
