package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.SexeAnimal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.api.NaissancesDeclareesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VelageEnregistreAnimalListener {

    private static final Logger log = LoggerFactory.getLogger(VelageEnregistreAnimalListener.class);

    private final AnimalRepository animalRepository;

    public VelageEnregistreAnimalListener(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    /**
     * Synchrone, dans la transaction de l'opération d'origine : un échec annule l'ensemble,
     * aucun état intermédiaire incohérent entre modules n'est conservé.
     */
    @EventListener
    public void onNaissancesDeclarees(NaissancesDeclareesEvent event) {
        log.info("Réception événement NaissancesDeclareesEvent pour le cycle {} (Mère: {})", event.cycleId(), event.animalId());

        if (event.veaux() == null || event.veaux().isEmpty()) {
            return; // Aucun veau à créer
        }

        // Vérification stricte d'idempotence : Si on a déjà des veaux pour ce cycle, on arrête !
        if (animalRepository.countByCycleReproductionId(event.cycleId()) > 0) {
            log.info("Les veaux pour le cycle {} ont déjà été créés. Idempotence appliquée.", event.cycleId());
            return;
        }

        // Récupération de la mère pour obtenir sa race par défaut pour le veau
        Animal mere = animalRepository
                .findByIdAndTenantIdAndExploitationId(event.animalId(), event.tenantId(), event.exploitationId())
                .orElseThrow(() -> new IllegalStateException("Mère introuvable lors de la création du veau."));

        // Validation de tous les veaux avant toute création : une erreur annule le vêlage entier.
        java.util.Set<String> identifiants = new java.util.HashSet<>();
        for (NaissancesDeclareesEvent.VeauPayload veauPayload : event.veaux()) {
            String identifiant = veauPayload.identifiant() == null ? "" : veauPayload.identifiant().trim();
            if (identifiant.isEmpty()) {
                throw new IllegalArgumentException("L'identifiant de chaque veau est obligatoire.");
            }
            if (!identifiants.add(identifiant)
                    || animalRepository.existsByExploitationIdAndIdentifiant(event.exploitationId(), identifiant)) {
                throw new IllegalStateException("L'identifiant de veau '" + identifiant + "' existe déjà dans l'exploitation.");
            }
            sexeVeau(veauPayload.sexe());
        }

        // Création des veaux
        for (NaissancesDeclareesEvent.VeauPayload veauPayload : event.veaux()) {
            SexeAnimal sexe = sexeVeau(veauPayload.sexe());

            // Le pere_identifiant prend soit le UUID (en string) du taureau interne, soit le code paillette externe
            String pereIdentifiant = event.codePaillette() != null ? event.codePaillette() : 
                                     (event.taureauId() != null ? event.taureauId().toString() : null);

            Animal veau = new Animal(
                    event.tenantId(),
                    event.exploitationId(),
                    mere.getRaceId(),
                    veauPayload.identifiant().trim(),
                    veauPayload.nom() != null && !veauPayload.nom().isBlank() ? veauPayload.nom() : veauPayload.identifiant().trim(),
                    event.dateVelage(), // Date de naissance = date de vêlage
                    sexe,
                    StatutAnimal.ACTIF,
                    null // actorId, puisque créé par le système
            );

            veau.definirMere(event.animalId());
            if (pereIdentifiant != null) {
                veau.definirPere(pereIdentifiant);
            }
            veau.definirCycleReproductionId(event.cycleId());

            animalRepository.save(veau);
            log.info("Veau créé avec succès (Identifiant: {})", veau.getIdentifiant());
        }
    }

    private static SexeAnimal sexeVeau(String sexe) {
        if (sexe == null || sexe.isBlank()) {
            return SexeAnimal.INCONNU;
        }
        try {
            return SexeAnimal.valueOf(sexe.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Sexe de veau inconnu : " + sexe + " (MALE, FEMELLE ou INCONNU).");
        }
    }
}
