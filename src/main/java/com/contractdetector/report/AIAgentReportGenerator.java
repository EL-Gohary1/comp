package com.contractdetector.report;

import com.contractdetector.ai.AIAgentResult;
import com.contractdetector.ai.AIFixSuggestion;
import com.contractdetector.change.RiskLevel;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Component
public class AIAgentReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generateReport(AIAgentResult result) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>ContractDetector AI Report - ").append(escapeHtml(result.getAnalysisId())).append("</title>\n")
            .append("    <style>\n")
            .append(getCSSStyles())
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"container\">\n");

        html.append(generateHeader(result))
            .append("        <div class=\"content\">\n")
            .append(generateSummarySection(result))
            .append(generateRiskSection(result))
            .append(generateFixesSection(result))
            .append(generateFilesSection(result))
            .append(generateExplanationSection(result))
            .append(generateStatsSection(result))
            .append("        </div>\n")
            .append(generateFooter(result))
            .append("    </div>\n")
            .append("</body>\n")
            .append("</html>");

        return html.toString();
    }

    private String generateHeader(AIAgentResult result) {
        StringBuilder header = new StringBuilder();
        header.append("        <div class=\"header\">\n")
              .append("            <div class=\"header-badge\">🔍 ContractDetector</div>\n")
              .append("            <h1>AI Agent Analysis Report</h1>\n")
              .append("            <div class=\"header-meta\">\n")
              .append("                <div class=\"meta-item\">\n")
              .append("                    <div class=\"meta-label\">Analysis ID</div>\n")
              .append("                    <div class=\"meta-value\">").append(escapeHtml(result.getAnalysisId())).append("</div>\n")
              .append("                </div>\n")
              .append("                <div class=\"meta-item\">\n")
              .append("                    <div class=\"meta-label\">Processing Time</div>\n")
              .append("                    <div class=\"meta-value\">").append(result.getProcessingTimeMs()).append(" ms</div>\n")
              .append("                </div>\n")
              .append("                <div class=\"meta-item\">\n")
              .append("                    <div class=\"meta-label\">Generated</div>\n")
              .append("                    <div class=\"meta-value\">").append(LocalDateTime.now().format(DATE_FORMATTER)).append("</div>\n")
              .append("                </div>\n")
              .append("            </div>\n")
              .append("        </div>\n");
        return header.toString();
    }

    private String generateSummarySection(AIAgentResult result) {
        if (isEmpty(result.getSummary())) return "";

        return "            <div class=\"section highlight\">\n" +
                "                <div class=\"section-title\">\n" +
                "                    <div class=\"section-icon\">📋</div>\n" +
                "                    Executive Summary\n" +
                "                </div>\n" +
                "                <div class=\"summary-box\">" + escapeHtml(result.getSummary()) + "</div>\n" +
                "            </div>\n";
    }

    private String generateRiskSection(AIAgentResult result) {
        if (result.getRiskClassification() == null || result.getRiskClassification().isEmpty()) {
            return "";
        }

        StringBuilder riskSection = new StringBuilder();
        riskSection.append("            <div class=\"section\">\n")
                   .append("                <div class=\"section-title\">\n")
                   .append("                    <div class=\"section-icon\">⚠️</div>\n")
                   .append("                    Risk Classification\n")
                   .append("                </div>\n")
                   .append("                <div class=\"risk-grid\">\n");

        for (Map.Entry<String, RiskLevel> entry : result.getRiskClassification().entrySet()) {
            String riskClass = entry.getValue().toString().toLowerCase();
            riskSection.append("                    <div class=\"risk-item ").append(riskClass).append("\">\n")
                       .append("                        <div class=\"risk-info\">\n")
                       .append("                            <span class=\"risk-name\">").append(escapeHtml(entry.getKey())).append("</span>\n")
                       .append("                        </div>\n")
                       .append("                        <span class=\"risk-badge ").append(entry.getValue()).append("\">")
                       .append(entry.getValue()).append("</span>\n")
                       .append("                    </div>\n");
        }

        riskSection.append("                </div>\n")
                   .append("            </div>\n");

        return riskSection.toString();
    }

    private String generateFixesSection(AIAgentResult result) {
        if (result.getSuggestedFixes() == null || result.getSuggestedFixes().isEmpty()) {
            return "            <div class=\"section\">\n" +
                    "                <div class=\"section-title\">\n" +
                    "                    <div class=\"section-icon\">💡</div>\n" +
                    "                    AI Fix Suggestions\n" +
                    "                </div>\n" +
                    "                <div class=\"empty-state\">No fix suggestions generated for this analysis.</div>\n" +
                    "            </div>\n";
        }

        StringBuilder fixesSection = new StringBuilder();
        fixesSection.append("            <div class=\"section\">\n")
                    .append("                <div class=\"section-title\">\n")
                    .append("                    <div class=\"section-icon\">💡</div>\n")
                    .append("                    AI Fix Suggestions (").append(result.getSuggestedFixes().size()).append(")\n")
                    .append("                </div>\n");

        for (int i = 0; i < result.getSuggestedFixes().size(); i++) {
            AIFixSuggestion fix = result.getSuggestedFixes().get(i);
            fixesSection.append(generateFixCard(fix, i + 1));
        }

        fixesSection.append("            </div>\n");
        return fixesSection.toString();
    }

    private String generateFixCard(AIFixSuggestion fix, int index) {
        StringBuilder card = new StringBuilder();
        String typeClass = fix.getType() != null ? fix.getType().toString().toLowerCase() : "general";
        double confidence = fix.getConfidence() > 0 ? fix.getConfidence() : 0.0;

        card.append("                <div class=\"fix-card\">\n")
            .append("                    <div class=\"fix-header\">\n")
            .append("                        <div class=\"fix-title-group\">\n")
            .append("                            <span class=\"fix-number\">#").append(index).append("</span>\n")
            .append("                            <span class=\"fix-title\">").append(escapeHtml(fix.getAction())).append("</span>\n")
            .append("                            <span class=\"fix-type-badge ").append(typeClass).append("\">")
            .append(fix.getType() != null ? formatFixType(fix.getType()) : "GENERAL").append("</span>\n")
            .append("                        </div>\n")
            .append("                        <div class=\"fix-meta\">\n")
            .append("                            <span class=\"confidence-badge\" style=\"--confidence: ").append((int)(confidence * 100)).append("%\">\n")
            .append("                                Confidence: ").append(String.format("%.1f", confidence * 100)).append("%\n")
            .append("                            </span>\n")
            .append("                        </div>\n")
            .append("                    </div>\n");

        // File info
        if (!isEmpty(fix.getFile())) {
            card.append("                    <div class=\"fix-file\">\n")
                .append("                        <span class=\"file-label\">📄 File:</span>\n")
                .append("                        <code class=\"file-path\">").append(escapeHtml(fix.getFile()));
            if (fix.getLineNumber() > 0) {
                card.append(":").append(fix.getLineNumber());
            }
            card.append("</code>\n")
                .append("                    </div>\n");
        }

        // Description
        if (!isEmpty(fix.getDescription())) {
            card.append("                    <div class=\"fix-description\">")
                .append(escapeHtml(fix.getDescription()))
                .append("</div>\n");
        }

        // Code comparison
        if (!isEmpty(fix.getOriginalCode()) || !isEmpty(fix.getSuggestedCode())) {
            card.append("                    <div class=\"code-comparison\">\n");

            if (!isEmpty(fix.getOriginalCode())) {
                card.append("                        <div class=\"code-block removed\">\n")
                    .append("                            <div class=\"code-header\">❌ Original</div>\n")
                    .append("                            <pre><code>").append(escapeHtml(fix.getOriginalCode())).append("</code></pre>\n")
                    .append("                        </div>\n");
            }

            if (!isEmpty(fix.getSuggestedCode())) {
                card.append("                        <div class=\"code-block added\">\n")
                    .append("                            <div class=\"code-header\">✅ Suggested</div>\n")
                    .append("                            <pre><code>").append(escapeHtml(fix.getSuggestedCode())).append("</code></pre>\n")
                    .append("                        </div>\n");
            }

            card.append("                    </div>\n");
        }

        card.append("                </div>\n");
        return card.toString();
    }

    private String generateFilesSection(AIAgentResult result) {
        boolean hasPojos = result.getAffectedPojoFiles() != null && !result.getAffectedPojoFiles().isEmpty();
        boolean hasTests = result.getAffectedTestFiles() != null && !result.getAffectedTestFiles().isEmpty();

        if (!hasPojos && !hasTests) return "";

        StringBuilder filesSection = new StringBuilder();
        filesSection.append("            <div class=\"section\">\n")
                    .append("                <div class=\"section-title\">\n")
                    .append("                    <div class=\"section-icon\">📁</div>\n")
                    .append("                    Affected Files\n")
                    .append("                </div>\n");

        if (hasPojos) {
            filesSection.append("                <div class=\"file-category\">\n")
                        .append("                    <h4>🔷 POJO Files (").append(result.getAffectedPojoFiles().size()).append(")</h4>\n")
                        .append("                    <div class=\"file-list\">\n");
            for (String file : result.getAffectedPojoFiles()) {
                filesSection.append("                        <div class=\"file-tag pojo\">\n")
                            .append("                            <span class=\"file-icon\">📄</span>\n")
                            .append("                            <span class=\"file-name\">").append(escapeHtml(file)).append("</span>\n")
                            .append("                        </div>\n");
            }
            filesSection.append("                    </div>\n")
                        .append("                </div>\n");
        }

        if (hasTests) {
            filesSection.append("                <div class=\"file-category\">\n")
                        .append("                    <h4>🧪 Test Files (").append(result.getAffectedTestFiles().size()).append(")</h4>\n")
                        .append("                    <div class=\"file-list\">\n");
            for (String file : result.getAffectedTestFiles()) {
                filesSection.append("                        <div class=\"file-tag test\">\n")
                            .append("                            <span class=\"file-icon\">🧪</span>\n")
                            .append("                            <span class=\"file-name\">").append(escapeHtml(file)).append("</span>\n")
                            .append("                        </div>\n");
            }
            filesSection.append("                    </div>\n")
                        .append("                </div>\n");
        }

        filesSection.append("            </div>\n");
        return filesSection.toString();
    }

    private String generateExplanationSection(AIAgentResult result) {
        if (isEmpty(result.getExplanation())) return "";

        // Convert markdown-like formatting to HTML
        String formatted = escapeHtml(result.getExplanation())
                .replace("\n\n", "</p><p>")
                .replace("\n", "<br>");

        return "            <div class=\"section\">\n" +
                "                <div class=\"section-title\">\n" +
                "                    <div class=\"section-icon\">🔍</div>\n" +
                "                    Detailed Analysis\n" +
                "                </div>\n" +
                "                <div class=\"explanation-content\">\n" +
                "                    <p>" + formatted + "</p>\n" +
                "                </div>\n" +
                "            </div>\n";
    }

    private String generateStatsSection(AIAgentResult result) {
        int riskCount = result.getRiskClassification() != null ? result.getRiskClassification().size() : 0;
        int fixCount = result.getSuggestedFixes() != null ? result.getSuggestedFixes().size() : 0;
        int pojoCount = result.getAffectedPojoFiles() != null ? result.getAffectedPojoFiles().size() : 0;
        int testCount = result.getAffectedTestFiles() != null ? result.getAffectedTestFiles().size() : 0;

        return "            <div class=\"section stats-section\">\n" +
                "                <div class=\"section-title\">\n" +
                "                    <div class=\"section-icon\">📊</div>\n" +
                "                    Analysis Statistics\n" +
                "                </div>\n" +
                "                <div class=\"stats-grid\">\n" +
                "                    <div class=\"stat-card " + (riskCount > 0 ? "has-data" : "") + "\">\n" +
                "                        <div class=\"stat-icon\">⚠️</div>\n" +
                "                        <div class=\"stat-value\">" + riskCount + "</div>\n" +
                "                        <div class=\"stat-label\">Risk Items</div>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-card " + (fixCount > 0 ? "has-data" : "") + "\">\n" +
                "                        <div class=\"stat-icon\">💡</div>\n" +
                "                        <div class=\"stat-value\">" + fixCount + "</div>\n" +
                "                        <div class=\"stat-label\">Fix Suggestions</div>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-card " + (pojoCount > 0 ? "has-data" : "") + "\">\n" +
                "                        <div class=\"stat-icon\">📄</div>\n" +
                "                        <div class=\"stat-value\">" + pojoCount + "</div>\n" +
                "                        <div class=\"stat-label\">POJO Files</div>\n" +
                "                    </div>\n" +
                "                    <div class=\"stat-card " + (testCount > 0 ? "has-data" : "") + "\">\n" +
                "                        <div class=\"stat-icon\">🧪</div>\n" +
                "                        <div class=\"stat-value\">" + testCount + "</div>\n" +
                "                        <div class=\"stat-label\">Test Files</div>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n";
    }

    private String generateFooter(AIAgentResult result) {
        return "        <div class=\"footer\">\n" +
                "            <div class=\"footer-content\">\n" +
                "                <p>Generated by <strong>ContractDetector AI Agent</strong></p>\n" +
                "                <p class=\"footer-meta\">Report ID: " + UUID.randomUUID().toString().substring(0, 8) +
                " | Analysis: " + escapeHtml(result.getAnalysisId()) + "</p>\n" +
                "            </div>\n" +
                "        </div>\n";
    }

    // Helper methods
    private String formatFixType(AIFixSuggestion.FixType type) {
        return type.toString().replace("_", " ");
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    private String getCSSStyles() {
        return """
            :root {
                --primary: #2563eb;
                --primary-dark: #1d4ed8;
                --success: #10b981;
                --warning: #f59e0b;
                --danger: #ef4444;
                --info: #3b82f6;
                --bg: #f1f5f9;
                --card: #ffffff;
                --text: #0f172a;
                --text-muted: #64748b;
                --border: #e2e8f0;
                --radius: 12px;
                --shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -1px rgba(0,0,0,0.06);
                --shadow-lg: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
            }

            * { margin: 0; padding: 0; box-sizing: border-box; }

            body {
                font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                padding: 20px;
                color: var(--text);
                line-height: 1.6;
            }

            .container {
                max-width: 1000px;
                margin: 0 auto;
                background: var(--card);
                border-radius: var(--radius);
                box-shadow: var(--shadow-lg);
                overflow: hidden;
            }

            .header {
                background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                color: white;
                padding: 40px;
                text-align: center;
                position: relative;
            }

            .header-badge {
                display: inline-block;
                background: rgba(255,255,255,0.1);
                padding: 8px 16px;
                border-radius: 20px;
                font-size: 0.9rem;
                margin-bottom: 15px;
                backdrop-filter: blur(10px);
            }

            .header h1 {
                font-size: 2.2rem;
                margin-bottom: 25px;
                font-weight: 700;
            }

            .header-meta {
                display: flex;
                justify-content: center;
                gap: 30px;
                flex-wrap: wrap;
            }

            .meta-item {
                background: rgba(255,255,255,0.1);
                padding: 15px 25px;
                border-radius: var(--radius);
                min-width: 150px;
            }

            .meta-label {
                font-size: 0.8rem;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                opacity: 0.8;
                margin-bottom: 5px;
            }

            .meta-value {
                font-size: 1.1rem;
                font-weight: 600;
            }

            .content { padding: 30px; }

            .section {
                margin-bottom: 30px;
                background: var(--bg);
                border-radius: var(--radius);
                padding: 25px;
                border-left: 4px solid var(--primary);
                transition: transform 0.2s, box-shadow 0.2s;
            }

            .section:hover {
                transform: translateY(-2px);
                box-shadow: var(--shadow);
            }

            .section.highlight {
                background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                border-left-color: var(--info);
            }

            .section-title {
                font-size: 1.3rem;
                font-weight: 600;
                margin-bottom: 20px;
                display: flex;
                align-items: center;
                gap: 12px;
                color: var(--text);
            }

            .section-icon {
                width: 36px;
                height: 36px;
                background: var(--primary);
                border-radius: 8px;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 1.2rem;
            }

            .summary-box {
                background: white;
                padding: 20px;
                border-radius: var(--radius);
                font-size: 1.1rem;
                line-height: 1.7;
                box-shadow: var(--shadow);
            }

            .empty-state {
                text-align: center;
                padding: 40px;
                color: var(--text-muted);
                font-style: italic;
            }

            /* Risk Grid */
            .risk-grid {
                display: grid;
                gap: 12px;
            }

            .risk-item {
                background: white;
                padding: 16px 20px;
                border-radius: var(--radius);
                display: flex;
                justify-content: space-between;
                align-items: center;
                box-shadow: var(--shadow);
                border-left: 4px solid;
            }

            .risk-item.high { border-left-color: var(--danger); }
            .risk-item.medium { border-left-color: var(--warning); }
            .risk-item.low { border-left-color: var(--success); }

            .risk-name {
                font-weight: 500;
                font-size: 1rem;
            }

            .risk-badge {
                padding: 6px 14px;
                border-radius: 20px;
                font-size: 0.8rem;
                font-weight: 700;
                text-transform: uppercase;
                letter-spacing: 0.5px;
                color: white;
            }

            .risk-badge.HIGH { background: var(--danger); }
            .risk-badge.MEDIUM { background: var(--warning); }
            .risk-badge.LOW { background: var(--success); }

            /* Fix Cards */
            .fix-card {
                background: white;
                border-radius: var(--radius);
                padding: 20px;
                margin-bottom: 20px;
                box-shadow: var(--shadow);
                border-left: 4px solid var(--success);
            }

            .fix-header {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                margin-bottom: 15px;
                flex-wrap: wrap;
                gap: 10px;
            }

            .fix-title-group {
                display: flex;
                align-items: center;
                gap: 10px;
                flex-wrap: wrap;
            }

            .fix-number {
                background: var(--primary);
                color: white;
                width: 28px;
                height: 28px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 0.9rem;
                font-weight: 700;
            }

            .fix-title {
                font-size: 1.1rem;
                font-weight: 600;
                color: var(--text);
            }

            .fix-type-badge {
                padding: 4px 10px;
                border-radius: 15px;
                font-size: 0.75rem;
                font-weight: 600;
                text-transform: uppercase;
                background: #e0e7ff;
                color: #3730a3;
            }

            .fix-type-badge.pojo_field { background: #dbeafe; color: #1e40af; }
            .fix-type-badge.test_assertion { background: #d1fae5; color: #065f46; }
            .fix-type-badge.json_path { background: #fef3c7; color: #92400e; }
            .fix-type-badge.import_statement { background: #fce7f3; color: #9d174d; }

            .confidence-badge {
                background: linear-gradient(90deg, var(--success) var(--confidence), var(--bg) var(--confidence));
                padding: 6px 12px;
                border-radius: 20px;
                font-size: 0.85rem;
                font-weight: 600;
                border: 2px solid var(--border);
            }

            .fix-file {
                background: #f8fafc;
                padding: 10px 15px;
                border-radius: 6px;
                margin-bottom: 12px;
                font-family: 'Consolas', monospace;
                font-size: 0.9rem;
            }

            .file-label { margin-left: 8px; opacity: 0.7; }
            .file-path { color: var(--primary); font-weight: 600; }

            .fix-description {
                color: var(--text-muted);
                margin-bottom: 15px;
                line-height: 1.6;
            }

            /* Code Comparison */
            .code-comparison {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 15px;
                margin-top: 15px;
            }

            .code-block {
                background: #1e293b;
                border-radius: 8px;
                overflow: hidden;
            }

            .code-block.removed { border: 2px solid var(--danger); }
            .code-block.added { border: 2px solid var(--success); }

            .code-header {
                background: rgba(255,255,255,0.1);
                padding: 8px 12px;
                font-size: 0.85rem;
                font-weight: 600;
                color: white;
            }

            .code-block pre {
                padding: 15px;
                margin: 0;
                overflow-x: auto;
                font-family: 'Consolas', 'Monaco', monospace;
                font-size: 0.9rem;
                line-height: 1.5;
                color: #e2e8f0;
            }

            /* Files Section */
            .file-category { margin-bottom: 15px; }
            .file-category h4 {
                color: var(--text-muted);
                font-size: 0.95rem;
                margin-bottom: 12px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            .file-list {
                display: flex;
                flex-wrap: wrap;
                gap: 10px;
            }

            .file-tag {
                background: white;
                border: 2px solid var(--border);
                padding: 10px 16px;
                border-radius: 25px;
                display: flex;
                align-items: center;
                gap: 8px;
                font-family: 'Consolas', monospace;
                font-size: 0.9rem;
                transition: all 0.2s;
            }

            .file-tag:hover {
                border-color: var(--primary);
                transform: translateY(-2px);
                box-shadow: var(--shadow);
            }

            .file-tag.pojo { border-color: #dbeafe; background: #eff6ff; }
            .file-tag.test { border-color: #d1fae5; background: #ecfdf5; }

            /* Explanation */
            .explanation-content {
                background: white;
                padding: 20px;
                border-radius: var(--radius);
                line-height: 1.8;
            }

            .explanation-content p {
                margin-bottom: 12px;
            }

            /* Stats */
            .stats-section { background: white; }
            
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 20px;
            }

            .stat-card {
                background: var(--bg);
                padding: 25px;
                border-radius: var(--radius);
                text-align: center;
                transition: all 0.3s;
                border: 2px solid transparent;
            }

            .stat-card:hover {
                transform: translateY(-5px);
                border-color: var(--primary);
            }

            .stat-card.has-data {
                background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
            }

            .stat-icon {
                font-size: 1.5rem;
                margin-bottom: 10px;
            }

            .stat-value {
                font-size: 2rem;
                font-weight: 700;
                color: var(--primary);
                margin-bottom: 5px;
            }

            .stat-label {
                font-size: 0.85rem;
                color: var(--text-muted);
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }

            /* Footer */
            .footer {
                background: var(--bg);
                padding: 30px;
                text-align: center;
                color: var(--text-muted);
                border-top: 1px solid var(--border);
            }

            .footer-content p { margin-bottom: 8px; }
            .footer-meta { font-size: 0.85rem; opacity: 0.8; }

            /* Responsive */
            @media (max-width: 768px) {
                .header { padding: 30px 20px; }
                .header h1 { font-size: 1.6rem; }
                .header-meta { flex-direction: column; gap: 10px; }
                .content { padding: 20px; }
                .code-comparison { grid-template-columns: 1fr; }
                .stats-grid { grid-template-columns: repeat(2, 1fr); }
                .fix-header { flex-direction: column; }
            }

            @media print {
                body { background: white; }
                .container { box-shadow: none; }
                .section { break-inside: avoid; }
            }
            """;
    }
}