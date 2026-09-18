package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RegisterUser {

    private final UtilisateurRepository utilisateurRepository;

    public RegisterUser(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public record Command(
            UUID userId,
            String telephone,
            String nom,
            String prenom,
            String email
    ) {}

    public Utilisateur execute(Command command) {
        if (command.userId() == null) {
            throw new IllegalArgumentException("L'identifiant utilisateur (userId) est obligatoire.");
        }

        return utilisateurRepository.findById(command.userId())
                .map(user -> {
                    user.updateProfil(command.nom(), command.prenom(), command.email());
                    return utilisateurRepository.save(user);
                })
                .orElseGet(() -> {
                    Utilisateur newUser = new Utilisateur(
                            command.userId(),
                            command.telephone(),
                            command.nom() != null ? command.nom() : "Nom",
                            command.prenom() != null ? command.prenom() : "Prénom"
                    );
                    if (command.email() != null) {
                        newUser.updateProfil(command.nom(), command.prenom(), command.email());
                    }
                    return utilisateurRepository.save(newUser);
                });
    }
}
