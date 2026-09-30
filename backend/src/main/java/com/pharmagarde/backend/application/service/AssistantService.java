package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmacieDeGardeResponse;
import com.pharmagarde.backend.infrastructure.ai.AssistantConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Fait répondre l'assistant conversationnel à une question. La liste des pharmacies de garde
 * du jour est insérée dans la consigne à chaque appel (aucun historique conservé entre deux
 * questions, aucune base vectorielle : à ce volume de données, les recopier dans la consigne
 * suffit et reste vérifiable). Réutilise GardeService.calculerPharmaciesDeGarde, la même
 * méthode que l'endpoint public GET /api/public/gardes : l'assistant ne peut donc jamais voir
 * une pharmacie que le site lui-même ne montre pas.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;
    private final GardeService gardeService;

    public AssistantService(ChatClient assistantChatClient, GardeService gardeService) {
        this.chatClient = assistantChatClient;
        this.gardeService = gardeService;
    }

    public String repondre(String question) {
        String consigne = AssistantConfig.CONSIGNE.formatted(listeDesGardesDuJour());

        try {
            return chatClient.prompt()
                    .system(consigne)
                    .user(question)
                    .call()
                    .content();
        } catch (Exception ex) {
            // Ollama non démarré, injoignable, ou trop lent (au-delà du read-timeout configuré) :
            // message clair pour l'appelant, et trace complète dans les logs pour le diagnostic.
            log.error("Échec de l'appel à l'assistant (Ollama)", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "L'assistant est momentanément indisponible, réessayez dans un instant");
        }
    }

    // Construit la liste en texte lisible des pharmacies de garde aujourd'hui, dans le même
    // format qu'une ligne de fiche : nom, quartier/zone, puis téléphone et horaires seulement
    // s'ils sont renseignés (une fiche peut en manquer).
    private String listeDesGardesDuJour() {
        List<PharmacieDeGardeResponse> gardes = gardeService.calculerPharmaciesDeGarde(LocalDate.now());

        if (gardes.isEmpty()) {
            return "(aucune pharmacie de garde recensée aujourd'hui)";
        }

        StringBuilder texte = new StringBuilder();
        for (PharmacieDeGardeResponse garde : gardes) {
            texte.append("- ").append(garde.nom())
                    .append(", ").append(garde.adresse())
                    .append(" (zone ").append(garde.nomZone()).append(", quartier ").append(garde.nomQuartier()).append(")");

            if (garde.telephone() != null && !garde.telephone().isBlank()) {
                texte.append(", téléphone ").append(garde.telephone());
            }
            if (garde.horaireOuverture() != null && garde.horaireFermeture() != null) {
                texte.append(", ouverte de ").append(garde.horaireOuverture())
                        .append(" à ").append(garde.horaireFermeture());
            }
            texte.append("\n");
        }
        return texte.toString();
    }
}