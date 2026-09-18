package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.Membership;
import com.dairy.apipinal.identity.domain.RoleExploitation;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.MembershipRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ManageMembership {

    private final MembershipRepository membershipRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ManageMembership(
            MembershipRepository membershipRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public record AddMemberCommand(
            UUID requesterId,
            UUID exploitationId,
            String telephone,
            RoleExploitation role
    ) {}

    public record MemberView(
            UUID membershipId,
            UUID utilisateurId,
            String nom,
            String prenom,
            String telephone,
            RoleExploitation role
    ) {}

    public MemberView addMember(AddMemberCommand command) {
        if (command.exploitationId() == null) {
            throw new IllegalArgumentException("L'identifiant de l'exploitation est obligatoire.");
        }
        if (command.telephone() == null || command.telephone().isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone du membre est obligatoire.");
        }
        if (command.role() == null) {
            throw new IllegalArgumentException("Le rôle du membre est obligatoire.");
        }

        // Vérifier l'accès du demandeur
        Membership requesterMembership = membershipRepository
                .findByUtilisateurIdAndExploitationId(command.requesterId(), command.exploitationId())
                .orElseThrow(() -> new IllegalArgumentException("Vous n'êtes pas membre de cette exploitation."));

        if (requesterMembership.getRole() == RoleExploitation.EMPLOYE) {
            throw new IllegalStateException("Seuls les propriétaires et gérants peuvent ajouter des membres.");
        }

        Utilisateur targetUser = utilisateurRepository.findByTelephone(command.telephone())
                .orElseGet(() -> utilisateurRepository.save(new Utilisateur(
                        UUID.randomUUID(),
                        command.telephone(),
                        "Membre",
                        "Invité"
                )));

        membershipRepository.findByUtilisateurIdAndExploitationId(targetUser.getId(), command.exploitationId())
                .ifPresent(m -> {
                    throw new IllegalStateException("Cet utilisateur est déjà membre de cette exploitation.");
                });

        Membership newMembership = membershipRepository.save(
                new Membership(targetUser.getId(), command.exploitationId(), command.role())
        );

        return new MemberView(
                newMembership.getId(),
                targetUser.getId(),
                targetUser.getNom(),
                targetUser.getPrenom(),
                targetUser.getTelephone(),
                newMembership.getRole()
        );
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(UUID requesterId, UUID exploitationId) {
        membershipRepository.findByUtilisateurIdAndExploitationId(requesterId, exploitationId)
                .orElseThrow(() -> new IllegalArgumentException("Accès refusé à cette exploitation."));

        return membershipRepository.findAll().stream()
                .filter(m -> m.getExploitationId().equals(exploitationId))
                .map(m -> {
                    Utilisateur user = utilisateurRepository.findById(m.getUtilisateurId()).orElse(null);
                    return new MemberView(
                            m.getId(),
                            m.getUtilisateurId(),
                            user != null ? user.getNom() : "",
                            user != null ? user.getPrenom() : "",
                            user != null ? user.getTelephone() : "",
                            m.getRole()
                    );
                })
                .toList();
    }
}
