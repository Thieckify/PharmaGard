package com.pharmagarde.backend.infrastructure.ai;

import com.pharmagarde.backend.domain.port.out.ModeleDeLangagePort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Adaptateur Spring AI du port ModeleDeLangagePort : envoie la consigne et la question au
 * modèle servi par Ollama. Changer de modèle ou de fournisseur ne touche que cette classe.
 */
@Component
public class OllamaModeleDeLangageAdapter implements ModeleDeLangagePort {

    private final ChatClient chatClient;

    public OllamaModeleDeLangageAdapter(ChatClient assistantChatClient) {
        this.chatClient = assistantChatClient;
    }

    @Override
    public String repondre(String consigne, String question) {
        return chatClient.prompt()
                .system(consigne)
                .user(question)
                .call()
                .content();
    }
}
