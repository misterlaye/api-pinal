package com.dairy.apipinal.shared.security;

/**
 * Moyen par lequel une session a été ouverte.
 * <ul>
 *     <li>{@link #OTP} : code à usage unique reçu par SMS, ouvre tous les accès de l'utilisateur ;</li>
 *     <li>{@link #PIN} : code permanent remis par un propriétaire à un ouvrier, limité aux adhésions
 *     ouvrières (EMPLOYE, GERANT). Une session PIN ne donne jamais accès à une exploitation dont
 *     l'utilisateur est propriétaire, car le PIN est connu de celui qui l'a remis.</li>
 * </ul>
 */
public enum AuthenticationMethod {
    OTP,
    PIN
}
