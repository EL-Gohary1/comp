package com.contractdetector.ai;

import com.contractdetector.change.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAgentResult {

    private String id;
    private String analysisId;

    private Map<String, RiskLevel> riskClassification;

    private List<AIFixSuggestion> suggestedFixes;

    private List<String> affectedTestFiles;

    private List<String> affectedPojoFiles;

    private String explanation;
    private String summary;

    private long processingTimeMs;
}