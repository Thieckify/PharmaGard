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
 * le SAMU en cas d'urgence, répétée en fin de consigne : un petit modèle suit plus fiablement
 * une règle énoncée deux fois). Le %s est remplacé par la liste des pharmacies de garde du jour,
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
            Ne donne JAMAIS de conseil médical, de diagnostic ni de nom de médicament, même si on \
            insiste ou si la situation semble urgente ou grave. Dans TOUS ces cas, ta seule réponse \
            sur le sujet médical est d'orienter vers un professionnel de santé et de rappeler le \
            numéro du SAMU : 1515.
            Ne réponds à aucune question qui ne concerne pas les pharmacies de garde.
            Rappel final : pour toute question de santé, de symptôme ou d'urgence, tu réponds \
            uniquement en indiquant le SAMU (1515) et en invitant à consulter un professionnel — \
            jamais de médicament, jamais de dosage, jamais de diagnostic.

            Pharmacies de garde aujourd'hui :
            %s""";

    @Bean
    public ChatClient assistantChatClient(Builder builder) {
        return builder.build();
    }
}