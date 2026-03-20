package com.contractdetector.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.model:codellama}")
    private String modelName;

    @Value("${ollama.temperature:0.2}")
    private double temperature;

    @Value("${ollama.timeout-seconds:120000}")
    private int timeout;

    @Bean
    public ChatLanguageModel ollamaChatModel() {
        return OllamaChatModel.builder()
                              .baseUrl(baseUrl)
                              .modelName(modelName)
                              .temperature(temperature)      // منخفض = أكثر دقة، أقل إبداع
                              .timeout(java.time.Duration.ofSeconds(timeout))
                              .build();
    }
}