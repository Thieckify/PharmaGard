package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.infrastructure.ai.AssistantConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Fait répondre l'assistant conversationnel à une question. La liste des pharmacies de garde
 * du jour est insérée dans la consigne à chaque appel (aucun historique conservé entre deux
 * questions, aucune base vectorielle : à ce volume de données, les recopier dans la consigne
 * suffit et reste vérifiable). Pour l'instant la liste est vide ; elle sera alimentée par
 * PharmacieRechercheService (brique suivante).
 */
@Service
public class AssistantService {

    private final ChatClient chatClient;

    public AssistantService(ChatClient assistantChatClient) {
        this.chatClient = assistantChatClient;
    }

    public String repondre(String question) {
        String consigne = AssistantConfig.CONSIGNE.formatted(listeDesGardesDuJour());

        return chatClient.prompt()
                .system(consigne)
                .user(question)
                .call()
                .content();
    }

    // Provisoire : sera remplacé par la vraie liste des pharmacies de garde aujourd'hui (brique 3)
    private String listeDesGardesDuJour() {
        return "(aucune donnée pour l'instant)";
    }
}