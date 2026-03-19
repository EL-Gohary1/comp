package com.contractdetector.impact;

import com.contractdetector.change.SchemaDiff;
import com.contractdetector.schema.SchemaChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Listens for {@link SchemaChangedEvent} and performs impact analysis on the
 * local codebase to identify which Java POJOs and test classes are affected by
 * the detected schema drift.
 *
 * <h3>Analysis pipeline</h3>
 * <ol>
 *   <li>Receive the event (runs async on the shared task executor).</li>
 *   <li>Skip no-change events (new baseline registrations).</li>
 *   <li>Delegate POJO discovery to {@link PojoDiscoveryService}.</li>
 *   <li>Delegate test-class analysis to {@link StaticTestAnalyzer}.</li>
 *   <li>Log a consolidated impact report per risk level.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

    private final StaticTestAnalyzer staticTestAnalyzer;
    private final PojoDiscoveryService pojoDiscoveryService;

    // ── Event handling ───────────────────────────────────────────────────────

    /**
     * Entry point triggered by Spring's event bus whenever a schema change is detected.
     *
     * <p>Intentionally async so that event publishing in {@link com.contractdetector.schema.SchemaRegistryService}
     * completes without blocking the capture pipeline.
     *
     * @param event the schema-changed event carrying old/new schemas and the diff
     */
    @Async("taskExecutor")
    @EventListener
    public void onSchemaChanged(SchemaChangedEvent event) {

        SchemaDiff diff = event.getDiff();

        // ── Skip baseline registrations (first schema for an endpoint) ────────
        if (diff == null || !diff.hasChanges()) {
            log.debug("ImpactAnalysisService: no-change event for [{}] — skipping",
                event.getNewSchema().getEndpointPath());
            return;
        }

        String endpoint = event.getNewSchema().getEndpointPath();
        String method   = event.getNewSchema().getHttpMethod();

        log.info("=== Impact Analysis triggered for [{} {}] v{} → v{} ===",
            method, endpoint,
            event.getOldSchema() != null ? event.getOldSchema().getVersion() : "–",
            event.getNewSchema().getVersion());

        // ── POJO discovery ────────────────────────────────────────────────────
        List<String> affectedPojos = pojoDiscoveryService.findAffectedPojos(diff);
        if (affectedPojos.isEmpty()) {
            log.info("  [POJO] No POJO fields directly affected.");
        } else {
            log.warn("  [POJO] {} POJO(s) potentially affected:", affectedPojos.size());
            affectedPojos.forEach(p -> log.warn("    → {}", p));
        }

        // ── Test analysis ─────────────────────────────────────────────────────
        List<String> affectedTests = staticTestAnalyzer.findAffectedTests(endpoint, diff);
        if (affectedTests.isEmpty()) {
            log.info("  [TEST] No test methods found referencing this endpoint.");
        } else {
            log.warn("  [TEST] {} test method(s) may require update:", affectedTests.size());
            affectedTests.forEach(t -> log.warn("    → {}", t));
        }

        // ── Risk summary ──────────────────────────────────────────────────────
        logRiskSummary(diff);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void logRiskSummary(SchemaDiff diff) {
        log.info("  Risk Summary: {} HIGH | {} MEDIUM | {} LOW",
            diff.getHighRiskChanges().size(),
            diff.getMediumRiskChanges().size(),
            diff.getLowRiskChanges().size());

        if (!diff.getHighRiskChanges().isEmpty()) {
            log.error("  ⚠ HIGH-RISK CHANGES DETECTED — manual review required:");
            diff.getHighRiskChanges().forEach(c -> log.error("    {}", c.toSummary()));
        }
    }
}
