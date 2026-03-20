package com.contractdetector.impact;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ImpactAnalysisReadyEvent extends ApplicationEvent {
    private final AnalysisContext context;

    public ImpactAnalysisReadyEvent(Object source, AnalysisContext context) {
        super(source);
        this.context = context;
    }
}