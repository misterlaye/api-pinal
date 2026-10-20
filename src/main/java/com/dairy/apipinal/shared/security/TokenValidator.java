package com.dairy.apipinal.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface de validation des tokens JWT.
 * Permet à shared.security de valider les tokens sans dépendre du module identity.
 */
public interface TokenValidator {

    /**
     * Valide un token JWT et retourne l'utilisateur et le moyen d'authentification.
     * @return vide si le token est invalide ou expiré.
     */
    Optional<ValidatedToken> validate(String token);

    /**
     * Valide un token JWT et retourne le userId (subject).
     * @return le UUID du userId, ou null si le token est invalide.
     */
    default UUID validateTokenAndGetUserId(String token) {
        return validate(token).map(ValidatedToken::userId).orElse(null);
    }

    record ValidatedToken(UUID userId, AuthenticationMethod method) {
    }
}
