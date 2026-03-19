package com.contractdetector.impact;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    public enum Confidence {
        HIGH, MEDIUM, LOW
    }

    private String id;
    private String filePath;
    private String className;
    private String methodName;
    private int lineNumber;
    private String endpointPattern;
    private Confidence confidence;
    private String httpMethod;
    private String path;
    
    // Storing representations of assertions and JSON path expressions parsed statically
    private List<String> assertions;
    private List<String> jsonPathExpressions;
}
