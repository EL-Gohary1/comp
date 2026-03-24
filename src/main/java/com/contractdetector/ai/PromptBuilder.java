package com.contractdetector.ai;

import com.contractdetector.change.SchemaChange;
import com.contractdetector.change.SchemaDiff;
import com.contractdetector.impact.POJOClass;
import com.contractdetector.impact.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PromptBuilder {

    public String buildPrompt(SchemaDiff diff,
                              List<TestCase> affectedTests,
                              List<POJOClass> affectedPojos,
                              List<String> similarCases) {

        StringBuilder prompt = new StringBuilder();

        // ====== System Context ======
        prompt.append("You are an expert Java API testing assistant.\n");
        prompt.append("Your task is to analyze API contract changes and suggest precise fixes.\n\n");

        // ====== Schema Changes ======
        prompt.append("## SCHEMA CHANGES DETECTED\n\n");
        prompt.append("Endpoint: ").append(diff.getHttpMethod())
              .append(" ").append(diff.getEndpointPath()).append("\n\n");

        prompt.append("Changes:\n");
        for (SchemaChange change : diff.getChanges()) {
            prompt.append(formatChange(change)).append("\n");
        }
        prompt.append("\n");

        // ====== Affected Tests ======
        prompt.append("## AFFECTED TEST CASES\n\n");
        if (affectedTests.isEmpty()) {
            prompt.append("CRITICAL: [NO TEST CLASSES PROVIDED IN CONTEXT. DO NOT INVENT ANY.]\n");
        } else {
            for (TestCase test : affectedTests) {
                prompt.append(formatTest(test)).append("\n");
            }
        }
        prompt.append("\n");

        // ====== Affected POJOs ======
        prompt.append("## AFFECTED POJO CLASSES\n\n");
        boolean hasPojos = (affectedPojos != null && !affectedPojos.isEmpty());
        if (!hasPojos) {
            prompt.append("### NOTICE: The codebase for POJOs is currently unavailable. Your response MUST NOT include any suggested fixes for POJO files. Focus ONLY on Tests if provided.\n\n");
        } else {
            for (POJOClass pojo : affectedPojos) {
                prompt.append(formatPojo(pojo)).append("\n");
            }
        }
        prompt.append("\n");

        // ====== Historical Cases ======
        if (!similarCases.isEmpty()) {
            prompt.append("## SIMILAR PAST CASES\n\n");
            for (String caseDesc : similarCases) {
                prompt.append("- ").append(caseDesc).append("\n");
            }
            prompt.append("\n");
        }

        // ====== Instructions ======
        prompt.append("## IMPORTANT INSTRUCTIONS\n\n");
        prompt.append("STRICTLY ONLY RETURN JSON. NO CONVERSATIONAL TEXT, NO EXPLANATION, NO MARKDOWN TAGS. START THE RESPONSE WITH '{' AND END WITH '}'.\n");
        prompt.append("1. ONLY use the provided 'AFFECTED TEST CASES' and 'AFFECTED POJO CLASSES' listed above. Do NOT invent files.\n"); // منع التأليف
        prompt.append("2. Classify risk level (HIGH/MEDIUM/LOW/INFO) for each change.\n");
        prompt.append("3. Provide the exact Java code changes needed for POJOs and Tests.\n");
        prompt.append("4. CRITICAL: If no real POJOs or Tests are provided in the sections above, return an empty list for 'suggestedFixes'.\n\n");
        prompt.append("5. STRICT RULE: If the 'AFFECTED POJO CLASSES' section says 'No POJOs found', the 'affectedPojoFiles' array MUST be [] and 'suggestedFixes' MUST NOT contain any POJO_FIELD types.\n");
        prompt.append("6. DO NOT use your imagination. If I didn't provide the class code, it doesn't exist for you.\n");
        prompt.append("7. INDEPENDENT CONTEXT RULE: If 'AFFECTED POJO CLASSES' is empty, you MUST NOT suggest any fix of type 'POJO_FIELD', even if the 'AFFECTED TEST CASES' suggest that a POJO should exist. No context = No fix.\n");

        // ====== Output Format ======
        prompt.append("## OUTPUT FORMAT (JSON)\n\n");
        prompt.append("Return ONLY a JSON object following this schema. Do NOT include any conversational text.\n");
        prompt.append("### EMPTY STATE RULE:\n");
        prompt.append("IF 'AFFECTED TEST CASES' is empty AND 'AFFECTED POJO CLASSES' is empty, YOU MUST RETURN:\n");
        prompt.append("{\n");
        prompt.append("  \"riskClassification\": {},\n");
        prompt.append("  \"suggestedFixes\": [],\n");
        prompt.append("  \"affectedTestFiles\": [],\n");
        prompt.append("  \"affectedPojoFiles\": [],\n");
        prompt.append("  \"explanation\": \"No local code context was provided to suggest fixes.\",\n");
        prompt.append("  \"summary\": \"No changes suggested due to missing context.\"\n");
        prompt.append("}\n\n");
        prompt.append("### GENERAL SCHEMA (When context is provided):\n");
        prompt.append("{\n");
        prompt.append("  \"riskClassification\": {\n");
        prompt.append("    \"[affected_field_name]\": \"[HIGH/MEDIUM/LOW]\"\n");
        prompt.append("  },\n");
        prompt.append("  \"suggestedFixes\": [\n");
        prompt.append("    {\n");
        String allowedTypes = hasPojos ? "POJO_FIELD | TEST_ASSERTION | JSONPATH_UPDATE" : "TEST_ASSERTION | JSONPATH_UPDATE";
        prompt.append("      \"type\": \"[").append(allowedTypes).append("]\",\n");
        prompt.append("      \"file\": \"[Actual file path from the provided context]\",\n");
        prompt.append("      \"lineNumber\": [Approximate line number],\n");
        prompt.append("      \"action\": \"[RENAME_FIELD | ADD_FIELD | DELETE_FIELD | UPDATE_ASSERTION]\",\n");
        prompt.append("      \"description\": \"[Brief explanation of the fix]\",\n");
        prompt.append("      \"originalCode\": \"[The exact current code snippet]\",\n");
        prompt.append("      \"suggestedCode\": \"[The corrected code snippet]\",\n");
        prompt.append("      \"confidence\": 0.95\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"affectedTestFiles\": [\"[Actual Test Class Name]\"],\n");
        prompt.append("  \"affectedPojoFiles\": [\"[Actual POJO Class Name]\"],\n");
        prompt.append("  \"explanation\": \"Summarize why these changes were suggested based on the diff.\",\n");
        prompt.append("  \"summary\": \"Short summary of the impact (e.g., 'Modified 2 files')\"\n");
        prompt.append("}\n\n");

        prompt.append("CRITICAL: Use the EXACT string provided in 'File Path' for the 'file' field in your JSON response. Do not modify it.\n");
        prompt.append("STRICTLY ONLY RETURN JSON. NO CONVERSATIONAL TEXT, NO EXPLANATION, NO MARKDOWN TAGS. START THE RESPONSE WITH '{' AND END WITH '}'.\n");

        return prompt.toString();
    }

    private String formatChange(SchemaChange change) {
        return String.format("- %s: %s (Risk: %s) - %s",
                change.getChangeType(),
                change.getFieldName(),
                change.getRiskLevel(),
                change.toSummary()
        );
    }

    private String formatTest(TestCase test) {
        return String.format("- Class: %s\n" +
                        "  Method: %s()\n" +
                        "  File Path: %s\n" +
                        "  Line: %d\n" +
                        "  Endpoint: %s %s\n" +
                        "  Assertions: %s",
                test.getClassName(),
                test.getMethodName(),
                test.getFilePath(),
                test.getLineNumber(),
                test.getHttpMethod(),
                test.getPath(),
                String.join(", ", test.getAssertions())
        );
    }

    private String formatPojo(POJOClass pojo) {
        String fields = pojo.getFields().stream()
                            .map(f -> f.getName() + (f.getJsonPropertyName() != null ? " (JSON: " + f.getJsonPropertyName() + ")" : ""))
                            .collect(Collectors.joining(", "));

        return String.format("- Class: %s\n" +
                        "  Package: %s\n" +
                        "  File Path: %s\n" +
                        "  Fields: %s\n" +
                        "  Has JSON annotations: %s",
                pojo.getClassName(),
                pojo.getPackageName(),
                pojo.getFilePath(),
                fields,
                pojo.isHasJsonAnnotations()
        );
    }
}