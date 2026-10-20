package com.dairy.apipinal.identity.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Simulation d'envoi de SMS : le message (donc le code OTP) est écrit dans les logs.
 * Active uniquement avec les profils {@code dev}, {@code test} ou {@code demo}.
 * Dans tout autre profil, aucun {@link SmsProvider} n'est déclaré et l'application refuse
 * de démarrer tant qu'un fournisseur réel n'est pas branché.
 */
@Component
@Profile({"dev", "test", "demo"})
public class SimulatedSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSmsProvider.class);

    public SimulatedSmsProvider() {
        log.warn("SMS SIMULÉS : les codes OTP sont écrits dans les logs. Ne jamais utiliser en production.");
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("[SMS simulé] vers {} : {}", phoneNumber, message);
    }
}
