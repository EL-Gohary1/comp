package com.contractdetector.ai;

import com.contractdetector.change.RiskLevel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseParser {

    private final ObjectMapper objectMapper;


    public AIAgentResult parse(String aiResponse) {
        try {
            String cleanJson = extractJson(aiResponse);
            JsonNode root = objectMapper.readTree(cleanJson);

            if (root == null) return null;

            AIAgentResult result = new AIAgentResult();
            result.setId(java.util.UUID.randomUUID().toString());

            result.setRiskClassification(parseRiskMap(root.get("riskClassification")));
            result.setSuggestedFixes(parseFixes(root.get("suggestedFixes")));
            result.setAffectedTestFiles(parseStringList(root.get("affectedTestFiles")));
            result.setAffectedPojoFiles(parseStringList(root.get("affectedPojoFiles")));
            result.setExplanation(getText(root, "explanation"));
            result.setSummary(getText(root, "summary"));

            return result;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            return null;
        }
    }

    private String extractJson(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.lastIndexOf("```");
            return response.substring(start, end).trim();
        }

        if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            return response.substring(start, end).trim();
        }

        return response.trim();
    }
    private Map<String, RiskLevel> parseRiskMap(JsonNode node) {
        Map<String, RiskLevel> map = new HashMap<>();
        if (node == null || !node.isObject()) return map;

        node.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            String riskStr = entry.getValue().asText().trim().toUpperCase();
            try {
                map.put(field, RiskLevel.valueOf(riskStr));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown risk level '{}' for field '{}', defaulting to LOW", riskStr, field);
                map.put(field, RiskLevel.LOW);
            }
        });

        return map;
    }

    private List<AIFixSuggestion> parseFixes(JsonNode node) {
        List<AIFixSuggestion> fixes = new ArrayList<>();
        if (node == null || !node.isArray()) return fixes;

        for (JsonNode fixNode : node) {
            fixes.add(AIFixSuggestion.builder()
                                     .id(java.util.UUID.randomUUID().toString())
                                     .type(parseFixType(fixNode.get("type")))
                                     .file(getText(fixNode, "file"))
                                     .lineNumber(getInt(fixNode, "lineNumber"))
                                     .action(getText(fixNode, "action"))
                                     .description(getText(fixNode, "description"))
                                     .originalCode(getText(fixNode, "originalCode"))
                                     .suggestedCode(getText(fixNode, "suggestedCode"))
                                     .confidence(getDouble(fixNode, "confidence"))
                                     .build());
        }

        return fixes;
    }

    private AIFixSuggestion.FixType parseFixType(JsonNode node) {
        if (node == null) return null;
        try {
            return AIFixSuggestion.FixType.valueOf(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) return list;

        node.forEach(n -> list.add(n.asText()));
        return list;
    }

    private String getText(JsonNode parent, String field) {
        if (parent == null) return null;
        JsonNode node = parent.get(field);
        return node != null ? node.asText() : null;
    }

    private int getInt(JsonNode parent, String field) {
        if (parent == null) return 0;
        JsonNode node = parent.get(field);
        return node != null ? node.asInt() : 0;
    }

    private double getDouble(JsonNode parent, String field) {
        if (parent == null) return 0.0;
        JsonNode node = parent.get(field);
        return node != null ? node.asDouble() : 0.0;
    }
}