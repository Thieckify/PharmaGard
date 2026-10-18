package com.pharmagarde.backend.domain.port.out;

/**
 * Port de sortie : transforme un mot de passe en empreinte non réversible avant stockage.
 */
public interface EncodeurMotDePassePort {

    String encoder(String motDePasse);
}
