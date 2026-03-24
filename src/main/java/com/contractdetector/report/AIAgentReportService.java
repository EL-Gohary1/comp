package com.contractdetector.report;

import com.contractdetector.ai.AIAgentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAgentReportService {

    private final AIAgentReportGenerator reportGenerator;

    // يمكن تغييره من application.properties
    private String reportsDirectory = "./reports";

    /**
     * Generate and save report to file
     */
    public String generateAndSaveReport(AIAgentResult result) {
        String html = reportGenerator.generateReport(result);
        String fileName = "analysis-" + result.getAnalysisId() + "-" +
                System.currentTimeMillis() + ".html";

        try {
            Path reportsPath = Paths.get(reportsDirectory);
            if (!Files.exists(reportsPath)) {
                Files.createDirectories(reportsPath);
            }

            Path filePath = reportsPath.resolve(fileName);
            Files.writeString(filePath, html);

            log.info("📄 Report saved to: {}", filePath.toAbsolutePath());
            return filePath.toString();

        } catch (IOException e) {
            log.error("Failed to save report", e);
            return null;
        }
    }

    /**
     * Generate report and return as HTML string (for web display)
     */
    public String generateHtmlReport(AIAgentResult result) {
        return reportGenerator.generateReport(result);
    }

    public void setReportsDirectory(String directory) {
        this.reportsDirectory = directory;
    }
}