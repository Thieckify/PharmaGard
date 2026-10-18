package com.pharmagarde.backend.domain.port.out;

/**
 * Port de sortie vers le modèle de langage de l'assistant. La couche application fournit la
 * consigne et la question ; elle ne sait pas quel modèle répond (aujourd'hui Ollama, en local).
 */
public interface ModeleDeLangagePort {

    String repondre(String consigne, String question);
}
