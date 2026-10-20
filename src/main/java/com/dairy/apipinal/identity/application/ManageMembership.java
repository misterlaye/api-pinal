package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.Membership;
import com.dairy.apipinal.identity.domain.RoleExploitation;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.MembershipRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion de l'équipe d'une exploitation.
 * <p>
 * Seul un propriétaire de l'exploitation peut inviter ou lister les membres, et il ne peut
 * inviter que des ouvriers (EMPLOYE, GERANT). Le PIN d'un ouvrier n'est communiqué qu'une
 * seule fois, dans la réponse de l'invitation qui l'a généré ; il n'est jamais relisible.
 */
@Service
@Transactional
public class ManageMembership {

    private static final Set<RoleExploitation> INVITABLE_ROLES =
            EnumSet.of(RoleExploitation.EMPLOYE, RoleExploitation.GERANT);

    private final MembershipRepository membershipRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PinCodeService pinCodeService;

    public ManageMembership(
            MembershipRepository membershipRepository,
            UtilisateurRepository utilisateurRepository,
            PinCodeService pinCodeService
    ) {
        this.membershipRepository = membershipRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.pinCodeService = pinCodeService;
    }

    public record AddMemberCommand(
            UUID requesterId,
            UUID exploitationId,
            String telephone,
            String nomComplet,
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

    /**
     * Résultat d'une invitation. {@code codePin} n'est renseigné que si un PIN vient d'être généré ;
     * il vaut {@code null} lorsque l'utilisateur possédait déjà un PIN ({@code pinExistant}).
     */
    public record InvitationResult(
            UUID membershipId,
            UUID utilisateurId,
            String nom,
            String prenom,
            String telephone,
            RoleExploitation role,
            String codePin,
            boolean pinExistant
    ) {}

    public InvitationResult addMember(AddMemberCommand command) {
        if (command.exploitationId() == null) {
            throw new IllegalArgumentException("L'identifiant de l'exploitation est obligatoire.");
        }
        if (command.telephone() == null || command.telephone().isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone du membre est obligatoire.");
        }
        if (command.role() == null || !INVITABLE_ROLES.contains(command.role())) {
            throw new IllegalArgumentException("Seuls les rôles Ouvrier (EMPLOYE) et Gérant (GERANT) peuvent être attribués.");
        }

        requireOwner(command.requesterId(), command.exploitationId());

        Utilisateur targetUser = utilisateurRepository.findByTelephone(command.telephone())
                .orElseGet(() -> {
                    String nomComplet = command.nomComplet() == null ? "" : command.nomComplet().trim();
                    String[] parts = nomComplet.split(" ", 2);
                    String prenom = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "Ouvrier";
                    String nom = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "Inconnu";
                    return utilisateurRepository.save(new Utilisateur(
                            UUID.randomUUID(),
                            command.telephone(),
                            nom,
                            prenom
                    ));
                });

        if (membershipRepository.findByUtilisateurIdAndExploitationId(targetUser.getId(), command.exploitationId()).isPresent()) {
            throw new IllegalStateException("Cet utilisateur est déjà membre de cette exploitation.");
        }

        String generatedPin = null;
        boolean pinExistant = targetUser.hasPin();
        if (!pinExistant) {
            generatedPin = pinCodeService.generatePin();
            targetUser.definirPinHash(pinCodeService.hash(generatedPin));
            utilisateurRepository.save(targetUser);
        }

        Membership newMembership = membershipRepository.save(
                new Membership(targetUser.getId(), command.exploitationId(), command.role())
        );

        return new InvitationResult(
                newMembership.getId(),
                targetUser.getId(),
                targetUser.getNom(),
                targetUser.getPrenom(),
                targetUser.getTelephone(),
                newMembership.getRole(),
                generatedPin,
                pinExistant
        );
    }

    @Transactional(readOnly = true)
    public List<MemberView> listMembers(UUID requesterId, UUID exploitationId) {
        requireOwner(requesterId, exploitationId);

        List<Membership> memberships = membershipRepository.findByExploitationId(exploitationId);
        Map<UUID, Utilisateur> users = utilisateurRepository
                .findAllById(memberships.stream().map(Membership::getUtilisateurId).toList())
                .stream()
                .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));

        return memberships.stream()
                .map(m -> {
                    Utilisateur user = users.get(m.getUtilisateurId());
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

    private void requireOwner(UUID requesterId, UUID exploitationId) {
        Membership requester = membershipRepository
                .findByUtilisateurIdAndExploitationId(requesterId, exploitationId)
                .orElseThrow(() -> new AccessDeniedException("Accès refusé à cette exploitation."));
        if (requester.getRole() != RoleExploitation.PROPRIETAIRE) {
            throw new AccessDeniedException("Seul le propriétaire peut gérer l'équipe de l'exploitation.");
        }
    }
}
