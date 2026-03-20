package com.contractdetector.ai;

import com.contractdetector.impact.AnalysisContext;
import com.contractdetector.impact.ImpactAnalysisReadyEvent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAIAgent {

    private final ChatLanguageModel ollamaModel;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;


   // @Async("aiAgentExecutor")
    @EventListener
    public void onAnalysisReady(ImpactAnalysisReadyEvent event) {
        AnalysisContext context = event.getContext();

        log.info("🚀 AI Agent starting processing for: {} {}",
                context.getDiff().getHttpMethod(),
                context.getDiff().getEndpointPath());

        process(context);
    }

    public AIAgentResult process(AnalysisContext context) {
        long startTime = System.currentTimeMillis();

        List<String> draft = new ArrayList<>();
        draft.add("be careful");

        try {
            String prompt = promptBuilder.buildPrompt(
                    context.getDiff(),
                    context.getAffectedTests(),
                    context.getAffectedPojos(),
                    draft
            );

            log.debug("Sending prompt to AI (Length: {})", prompt.length());

            String aiResponse = ollamaModel.generate(prompt);

            log.debug("Received AI response (Length: {})", aiResponse.length());

            AIAgentResult result = responseParser.parse(aiResponse);

            result.setAnalysisId(context.getDiff().getId());
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            log.info("✅ AI Agent completed. Suggestions: {}, Risk Level: {}",
                    result.getSuggestedFixes().size(),
                    context.getDiff().getHighRiskChanges().isEmpty() ? "LOW" : "HIGH");

            System.out.println("********************************");
            System.out.println(result.getSuggestedFixes());
            System.out.println("********************************");
            return result;

        } catch (Exception e) {
            log.error("❌ AI Agent failed to process impact analysis", e);
            return null;
        }
    }


}