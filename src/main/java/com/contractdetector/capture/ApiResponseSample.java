package com.contractdetector.capture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_response_sample")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseSample {

    @Id
    private String id;

    private String method;
    private String url;
    private String path;

    @Column(columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private int statusCode;
    private String contentType;
    private LocalDateTime timestamp;
    private String testClassName;
    private String testMethodName;
}
