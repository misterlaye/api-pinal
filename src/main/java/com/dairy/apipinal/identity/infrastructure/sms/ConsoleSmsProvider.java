package com.dairy.apipinal.identity.infrastructure.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Implémentation de développement qui affiche le SMS dans la console.
 * Sera remplacée par un fournisseur réel en production.
 */
@Component
@ConditionalOnMissingBean(name = "productionSmsProvider")
public class ConsoleSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsProvider.class);

    @Override
    public void sendSms(String phoneNumber, String message) {
        log.info("==============================");
        log.info("SMS vers {} : {}", phoneNumber, message);
        log.info("==============================");
    }
}
