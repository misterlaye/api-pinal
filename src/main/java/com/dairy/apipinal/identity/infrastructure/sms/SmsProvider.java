package com.dairy.apipinal.identity.infrastructure.sms;

/**
 * Interface d'envoi de SMS.
 * <p>
 * En développement, {@link SimulatedSmsProvider} écrit les messages dans les logs.
 * Un fournisseur réel (Orange API, Twilio, Vonage…) doit fournir sa propre implémentation,
 * active hors des profils {@code dev}, {@code test} et {@code demo}, sans modifier le code métier.
 */
public interface SmsProvider {

    void sendSms(String phoneNumber, String message);
}
