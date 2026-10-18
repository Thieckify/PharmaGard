package com.pharmagarde.backend.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration technique de l'assistant (Spring AI + Ollama, modèle local) : fournit le client
 * de chat utilisé par OllamaModeleDeLangageAdapter. La consigne, elle, est une règle métier :
 * elle vit dans AssistantService (couche application).
 */
@Configuration
public class AssistantConfig {

    @Bean
    public ChatClient assistantChatClient(Builder builder) {
        return builder.build();
    }
}
