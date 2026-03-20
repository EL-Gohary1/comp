package com.contractdetector.impact;

import com.contractdetector.change.SchemaDiff;
import com.contractdetector.schema.ApiSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisContext {

    private ApiSchema oldSchema;
    private ApiSchema newSchema;
    private SchemaDiff diff;

    @Builder.Default
    private List<TestCase> affectedTests = new ArrayList<>();

    @Builder.Default
    private Set<String> runtimeCapturedTests = new java.util.HashSet<>();

    @Builder.Default
    private List<POJOClass> affectedPojos = new ArrayList<>();


    public int getTotalImpactedUniqueTests() {
        return affectedTests.size() + runtimeCapturedTests.size();
    }
}