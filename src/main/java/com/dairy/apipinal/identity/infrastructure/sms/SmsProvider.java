package com.dairy.apipinal.identity.infrastructure.sms;

/**
 * Interface d'envoi de SMS.
 * Permet de brancher un fournisseur réel (Twilio, Vonage, Orange API)
 * sans modifier le code métier.
 */
public interface SmsProvider {

    void sendSms(String phoneNumber, String message);
}
