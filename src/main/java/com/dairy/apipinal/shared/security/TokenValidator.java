package com.dairy.apipinal.shared.security;

import java.util.UUID;

/**
 * Interface de validation des tokens JWT.
 * Permet à shared.security de valider les tokens sans dépendre du module identity.
 */
public interface TokenValidator {

    /**
     * Valide un token JWT et retourne le userId (subject).
     * @return le UUID du userId, ou null si le token est invalide.
     */
    UUID validateTokenAndGetUserId(String token);
}
