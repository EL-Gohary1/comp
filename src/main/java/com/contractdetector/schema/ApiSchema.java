package com.contractdetector.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "api_schemas",
    indexes = {
        @Index(name = "idx_endpoint_method", columnList = "endpointPath, httpMethod"),
        @Index(name = "idx_version", columnList = "version")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiSchema {

    @Id
    private String id;

    private String endpointPath;
    private String httpMethod;
    private int version;

    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String schemaJson;

    private String baselineSchemaId;
    private String sampleId;
    
    private LocalDateTime inferredAt;
    private LocalDateTime createdAt;
}
