package com.contractdetector.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIFixSuggestion {

    private String id;
    private FixType type;
    private String file;
    private int lineNumber;
    private String action;
    private String description;
    private String originalCode;
    private String suggestedCode;
    private double confidence;

    public enum FixType {
        POJO_FIELD,
        TEST_ASSERTION,
        JSON_PATH,
        IMPORT_STATEMENT
    }
}