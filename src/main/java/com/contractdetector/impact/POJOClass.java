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
public class POJOClass {

    private String id;
    private String filePath;
    private String className;
    private String packageName;
    private List<POJOField> fields;
    private List<String> matchingChanges;
    private boolean hasJsonAnnotations;
    private List<String> jsonAnnotations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class POJOField {
        private String name;
        private String type;
        private String jsonPropertyName;
        private List<String> annotations;
    }
}
