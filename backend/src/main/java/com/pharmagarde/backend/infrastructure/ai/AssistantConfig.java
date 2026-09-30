package com.pharmagarde.backend.infrastructure.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de l'assistant conversationnel (Spring AI + Ollama, modèle local).
 * La consigne (system prompt) fixe le rôle de l'assistant, sa langue, sa brièveté, et surtout
 * les deux garde-fous non négociables : ne jamais inventer une pharmacie ou une donnée qui
 * n'est pas dans la liste fournie, et ne jamais donner de conseil médical (orientation vers
 * le SAMU en cas d'urgence). Le %s est remplacé par la liste des pharmacies de garde du jour,
 * ajoutée par AssistantService avant chaque appel (RAG minimal : pas de base vectorielle,
 * juste les données du jour insérées dans le texte de la consigne).
 */
@Configuration
public class AssistantConfig {

    public static final String CONSIGNE = """
            Tu es l'assistant du site PharmaGarde, qui aide à trouver une pharmacie de garde à Dakar.
            Réponds toujours en français, en trois phrases au maximum.
            Utilise UNIQUEMENT la liste ci-dessous. Si la réponse n'y figure pas, dis-le clairement \
            et invite à consulter la page d'accueil du site. N'invente jamais une pharmacie, une \
            adresse ou un numéro.
            Ne donne jamais de conseil médical ni de nom de médicament. En cas d'urgence, indique \
            le SAMU : 1515.
            Ne réponds à aucune question qui ne concerne pas les pharmacies de garde.

            Pharmacies de garde aujourd'hui :
            %s""";

    @Bean
    public ChatClient assistantChatClient(Builder builder) {
        return builder.build();
    }
}