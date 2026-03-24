package com.contractdetector.impact;


import com.contractdetector.change.SchemaDiff;
import com.contractdetector.schema.ApiSchema;
import com.contractdetector.schema.SchemaChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

    private final StaticTestAnalyzer testAnalyzer;
    private final PojoDiscoveryService pojoDiscoveryService;

    private final ApplicationEventPublisher eventPublisher;

    @Async("captureExecutor")
    @EventListener
    public void onSchemaChanged(SchemaChangedEvent event) {
        SchemaDiff diff = event.getDiff();

        if (diff == null || diff.getChanges().isEmpty()) {
            log.debug("ImpactAnalysisService: No significant changes detected. Skipping.");
            return;
        }

        ApiSchema newSchema = event.getNewSchema();

        log.info("=== Starting Deep Impact Analysis for: {} {} ===",
                newSchema.getHttpMethod(), newSchema.getEndpointPath());

        List<TestCase> affectedTests = testAnalyzer.findAffectedTests(
                newSchema.getEndpointPath(),
                getTestSourceDir()
        );

        System.out.println("********************");
        System.out.println(affectedTests.toString());
        System.out.println("********************");

        List<POJOClass> affectedPojos = pojoDiscoveryService.findAffectedPojos(
                diff.getChanges(),
                getMainSourceDir()
        );

        System.out.println("********************");
        System.out.println(affectedPojos.toString());
        System.out.println("********************");


        AnalysisContext context = AnalysisContext.builder()
                                                 .diff(diff)
                                                 .newSchema(newSchema)
                                                 .affectedTests(affectedTests)
                                                 .affectedPojos(affectedPojos)
                                                 .build();

        System.out.println("******************Impact Analysis Report****************");
        logReport(context);
        System.out.println("********************************************************");
        eventPublisher.publishEvent(new ImpactAnalysisReadyEvent(this, context));
    }

    private void logReport(AnalysisContext context) {
        log.info("--- Impact Analysis Report ---");
        log.warn(">> Affected Tests: {}", context.getAffectedTests().size());
        context.getAffectedTests().forEach(t -> log.warn("   - {} in {}", t.getMethodName(), t.getClassName()));

        log.warn(">> Affected POJOs: {}", context.getAffectedPojos().size());
        context.getAffectedPojos().forEach(t -> log.warn("   - {} in {}", t.getPackageName(), t.getClassName()));

        log.info(">> Total Risk: {} HIGH, {} MEDIUM, {} LOW",
                context.getDiff().getHighRiskChanges().size(),
                context.getDiff().getMediumRiskChanges().size(),
                context.getDiff().getLowRiskChanges().size());

        log.info("--- Analysis Complete ---");
    }

    private Path getTestSourceDir() {
        return Paths.get(System.getProperty("user.dir"), "src", "test", "java");
    }

    private Path getMainSourceDir() {
        return Paths.get(System.getProperty("user.dir"), "src", "main", "java");
    }
}