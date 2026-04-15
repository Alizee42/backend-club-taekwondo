package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.PaiementDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conversion Paiement (entity) → PaiementDTO.
 * Extrait de PaiementService pour isoler la responsabilité de mapping.
 */
@Component
public class PaiementMapper {

    public PaiementDTO toDTO(Paiement paiement) {
        PaiementDTO dto = new PaiementDTO();
        dto.setId(paiement.getId());
        dto.setType(paiement.getType());
        dto.setDatePaiement(paiement.getDatePaiement() != null ? paiement.getDatePaiement().toString() : null);
        dto.setStatut(paiement.getStatut());
        dto.setModePaiement(paiement.getModePaiement());
        dto.setMontantTotal(paiement.getMontantTotal());
        dto.setMotifAnnulation(paiement.getMotifAnnulation());
        dto.setDateAnnulation(paiement.getDateAnnulation());
        dto.setAdminResponsable(paiement.getAdminResponsable());

        // Club : priorité commande.club > membre.club > utilisateur.club
        Long clubId = null;
        if (paiement.getCommande() != null && paiement.getCommande().getClub() != null) {
            clubId = paiement.getCommande().getClub().getId();
        } else if (paiement.getMembre() != null && paiement.getMembre().getClub() != null) {
            clubId = paiement.getMembre().getClub().getId();
        } else if (paiement.getUtilisateur() != null && paiement.getUtilisateur().getClub() != null) {
            clubId = paiement.getUtilisateur().getClub().getId();
        }
        dto.setClubId(clubId);

        if (paiement.getUtilisateur() != null) {
            dto.setUtilisateurId(paiement.getUtilisateur().getId());
            dto.setUtilisateurNom(paiement.getUtilisateur().getNom());
            dto.setUtilisateurPrenom(paiement.getUtilisateur().getPrenom());
            dto.setUtilisateurEmail(paiement.getUtilisateur().getEmail());
        }

        if (paiement.getMembre() != null) {
            dto.setMembreId(paiement.getMembre().getId());
            dto.setMembreNom(paiement.getMembre().getNom());
            dto.setMembrePrenom(paiement.getMembre().getPrenom());
            dto.setEnfantNomComplet(
                    (Optional.ofNullable(paiement.getMembre().getPrenom()).orElse("") + " " +
                     Optional.ofNullable(paiement.getMembre().getNom()).orElse("")).trim()
            );
        }

        double montantPaye = 0.0;

        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            List<EcheanceDTO> liste = new ArrayList<>();
            for (Echeance e : paiement.getEcheances()) {
                EcheanceDTO edto = new EcheanceDTO();
                edto.setId(e.getId());
                edto.setNumero(e.getNumero());
                edto.setDateEcheance(e.getDateEcheance());
                edto.setMontant(e.getMontant());
                edto.setStatut(e.getStatut());
                edto.setModePaiement(e.getModePaiement());
                edto.setDatePaiementReel(e.getDatePaiementReel());
                edto.setReference(e.getReference());
                if ("payé".equalsIgnoreCase(e.getStatut())) {
                    montantPaye += safe(e.getMontant());
                }
                liste.add(edto);
            }
            dto.setEcheances(liste);
        } else {
            if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                montantPaye = safe(paiement.getMontantTotal());
            }
        }

        double total = safe(paiement.getMontantTotal());
        dto.setMontantPaye(montantPaye);
        dto.setMontantRestant(Math.max(0.0, total - montantPaye));

        return dto;
    }

    private double safe(Double v) {
        return v != null ? v : 0.0;
    }
}
