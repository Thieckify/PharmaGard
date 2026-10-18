package com.pharmagarde.backend.application.service;

import com.pharmagarde.backend.application.dto.PharmacieDeGardeResponse;
import com.pharmagarde.backend.domain.exception.ServiceIndisponibleException;
import com.pharmagarde.backend.domain.port.out.ModeleDeLangagePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Fait répondre l'assistant conversationnel à une question. La liste des pharmacies de garde
 * du jour est insérée dans la consigne à chaque appel (génération augmentée minimale : pas de
 * base vectorielle, les données du jour suffisent). Réutilise GardeService, la même méthode que
 * l'endpoint public : l'assistant ne voit jamais une pharmacie que le site ne montre pas.
 * Le modèle est derrière le port ModeleDeLangagePort : ce service ignore qu'il s'agit d'Ollama.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    // Consigne système : règle métier de l'assistant (rôle, langue, brièveté, garde-fous). Le %s
    // est remplacé par la liste des pharmacies de garde du jour. La règle du SAMU est répétée en
    // fin de consigne : un petit modèle la suit plus fiablement lorsqu'elle est énoncée deux fois.
    static final String CONSIGNE = """
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

    private final ModeleDeLangagePort modele;
    private final GardeService gardeService;

    public AssistantService(ModeleDeLangagePort modele, GardeService gardeService) {
        this.modele = modele;
        this.gardeService = gardeService;
    }

    public String repondre(String question) {
        String consigne = CONSIGNE.formatted(listeDesGardesDuJour());

        try {
            return modele.repondre(consigne, question);
        } catch (RuntimeException ex) {
            // Modèle arrêté, injoignable ou trop lent : message clair pour l'appelant (503),
            // trace complète dans les logs pour le diagnostic.
            log.error("Échec de l'appel à l'assistant (modèle de langage)", ex);
            throw new ServiceIndisponibleException("L'assistant est momentanément indisponible, réessayez dans un instant");
        }
    }

    // Liste lisible des pharmacies de garde aujourd'hui : nom, adresse, zone et quartier, puis
    // téléphone et horaires seulement s'ils sont renseignés (une fiche peut en manquer).
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
