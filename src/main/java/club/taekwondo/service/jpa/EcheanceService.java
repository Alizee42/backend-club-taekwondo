package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.MembreRetardDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EcheanceService {
    // Échéances filtrées par club
    @Transactional(readOnly = true)
    public List<EcheanceDTO> getEcheancesByClubId(Long clubId) {
        return echeanceRepository.findByPaiement_Membre_Club_Id(clubId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Autowired
    private EcheanceRepository echeanceRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    /* =========================================
     * ==============  QUERIES  ================
     * ========================================= */

    // ✅ Ajout: exposer l'entité par ID (utilisé par le webhook/service)
    public Optional<Echeance> getEcheanceEntityById(Long id) {
        return echeanceRepository.findById(id);
    }

    // ✅ Ajout: persister une échéance (create/update) – utilisé par le webhook/service
    @Transactional
    public Echeance save(Echeance e) {
        return echeanceRepository.save(e);
    }

    // 🔹 Récupérer toutes les échéances sous forme de DTO
    @Transactional(readOnly = true)
    public List<EcheanceDTO> getAllEcheanceDTOs() {
        return echeanceRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    /* =========================================
     * ==============  COMMANDES  ==============
     * ========================================= */

    // 🔹 Créer une échéance (DTO → Entity) avec lien à un paiement
    @Transactional
    public EcheanceDTO createEcheance(EcheanceDTO echeanceDTO, Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec id : " + paiementId));

        Echeance echeance = toEntity(echeanceDTO);
        echeance.setPaiement(paiement);

        return toDTO(echeanceRepository.save(echeance));
    }

    // 🔹 Mettre à jour une échéance existante (y compris mode / ref / date réelle si fournis)
    @Transactional
    public EcheanceDTO updateEcheance(Long id, EcheanceDTO echeanceDTO) {
        Echeance echeance = echeanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Échéance non trouvée avec id: " + id));

        echeance.setDateEcheance(echeanceDTO.getDateEcheance());
        if (echeanceDTO.getMontant() != null) {
            echeance.setMontant(echeanceDTO.getMontant());
        }
        echeance.setStatut(echeanceDTO.getStatut());
        echeance.setNumero(echeanceDTO.getNumero());

        // Champs de paiement au niveau de l’échéance
        if (echeanceDTO.getModePaiement() != null) {
            echeance.setModePaiement(normalizeMode(echeanceDTO.getModePaiement()));
        }
        if (echeanceDTO.getReference() != null) {
            echeance.setReference(echeanceDTO.getReference());
        }
        if (echeanceDTO.getDatePaiementReel() != null) {
            echeance.setDatePaiementReel(echeanceDTO.getDatePaiementReel());
        }

        return toDTO(echeanceRepository.save(echeance));
    }

    // 🔹 Payer une échéance spécifique (enregistré par l'admin)
    @Transactional
    public EcheanceDTO payerEcheance(Long echeanceId, String modePaiement, String reference, LocalDate datePaiementReel) {
        Echeance echeance = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new RuntimeException("Échéance non trouvée avec id: " + echeanceId));

        if ("payé".equalsIgnoreCase(echeance.getStatut())) {
            throw new IllegalStateException("Cette échéance est déjà payée.");
        }

        // Enregistre le paiement sur l'échéance
        echeance.setStatut("payé");
        echeance.setModePaiement(normalizeMode(modePaiement));
        echeance.setReference(reference);
        echeance.setDatePaiementReel(datePaiementReel != null ? datePaiementReel : LocalDate.now());
        echeanceRepository.save(echeance);

        // Mets à jour le paiement parent (logique existante conservée)
        Paiement paiement = echeance.getPaiement();
        if (paiement != null) {
            if (paiement.getMontantRestant() != null) {
                paiement.setMontantRestant(Math.max(0, paiement.getMontantRestant() - echeance.getMontant()));
            }
            if (paiement.getEcheancesRestantes() != null) {
                paiement.setEcheancesRestantes(Math.max(0, paiement.getEcheancesRestantes() - 1));
                if (paiement.getEcheancesRestantes() <= 0) {
                    paiement.setStatut("payé");
                }
            } else {
                // Si pas géré par compteur, dérive le statut depuis les échéances
                boolean allPayees = paiement.getEcheances() != null
                        && paiement.getEcheances().stream().allMatch(e -> "payé".equalsIgnoreCase(e.getStatut()));
                if (allPayees) paiement.setStatut("payé");
            }
            paiementRepository.save(paiement);
        }

        return toDTO(echeance);
    }

    /* =========================================
     * ==============  REPORTING  ==============
     * ========================================= */

    public List<MembreRetardDTO> getMembresEnRetard() {
        // Récupère toutes les échéances en retard (statut = "en attente" et date antérieure à aujourd'hui)
        List<Echeance> echeancesEnRetard = echeanceRepository.findByStatutAndDateEcheanceBefore("en attente", LocalDate.now());

        // Regroupe les échéances par utilisateur (par membre payeur)
        Map<Long, List<Echeance>> echeancesParMembre = echeancesEnRetard.stream()
                .collect(Collectors.groupingBy(e -> e.getPaiement().getUtilisateur().getId()));

        List<MembreRetardDTO> retards = new ArrayList<>();
        for (Map.Entry<Long, List<Echeance>> entry : echeancesParMembre.entrySet()) {
            List<Echeance> echeances = entry.getValue();

            // Total restant (uniquement "en attente")
            double totalRestant = echeances.stream()
                    .filter(e -> "en attente".equalsIgnoreCase(e.getStatut()))
                    .mapToDouble(Echeance::getMontant)
                    .sum();

            // Nom du payeur (parent)
            String nom = echeances.get(0).getPaiement().getUtilisateur().getNom();

            // Première échéance en retard
            Echeance echeanceEnRetard = echeances.stream()
                    .filter(e -> e.getDateEcheance().isBefore(LocalDate.now()) && "en attente".equalsIgnoreCase(e.getStatut()))
                    .findFirst().orElse(null);

            if (echeanceEnRetard != null) {
                MembreRetardDTO dto = new MembreRetardDTO(
                        nom,
                        totalRestant,
                        echeanceEnRetard.getDateEcheance(),
                        echeanceEnRetard.getMontant()
                );
                dto.setUtilisateurId(entry.getKey());
                retards.add(dto);
            }
        }

        return retards;
    }

    /* =========================================
     * ==============  DELETE  =================
     * ========================================= */

    @Transactional
    public void delete(Long id) {
        Optional<Echeance> echeanceOpt = echeanceRepository.findById(id);
        if (echeanceOpt.isEmpty()) {
            throw new RuntimeException("Échéance introuvable avec l'ID : " + id);
        }
        echeanceRepository.deleteById(id);
    }

    /* =========================================
     * ==============  MAPPINGS  ===============
     * ========================================= */

    // 🔁 Entity → DTO
    private EcheanceDTO toDTO(Echeance echeance) {
        EcheanceDTO dto = new EcheanceDTO();
        dto.setId(echeance.getId());
        dto.setDateEcheance(echeance.getDateEcheance());
        dto.setMontant(echeance.getMontant());
        dto.setStatut(echeance.getStatut());
        dto.setNumero(echeance.getNumero());

        // Champs paiement au niveau de l’échéance
        dto.setModePaiement(echeance.getModePaiement());
        dto.setDatePaiementReel(echeance.getDatePaiementReel());
        dto.setReference(echeance.getReference());

        // ------- Parent (utilisateur payeur) -------
        if (echeance.getPaiement() != null && echeance.getPaiement().getUtilisateur() != null) {
            dto.setPrenom(echeance.getPaiement().getUtilisateur().getPrenom());
            dto.setNom(echeance.getPaiement().getUtilisateur().getNom());
        }

        // ------- Enfant (membre concerné) -------
        if (echeance.getPaiement() != null && echeance.getPaiement().getMembre() != null) {
            dto.setEnfantPrenom(echeance.getPaiement().getMembre().getPrenom());
            dto.setEnfantNom(echeance.getPaiement().getMembre().getNom());
        }

        return dto;
    }

    // 🔁 DTO → Entity (⚠️ Paiement à associer manuellement dans le service appelant)
    private Echeance toEntity(EcheanceDTO dto) {
        Echeance e = new Echeance();
        e.setId(dto.getId());
        e.setDateEcheance(dto.getDateEcheance());
        if (dto.getMontant() != null) e.setMontant(dto.getMontant());
        e.setStatut(dto.getStatut());
        e.setNumero(dto.getNumero());

        // Champs paiement au niveau de l’échéance
        e.setModePaiement(dto.getModePaiement() != null ? normalizeMode(dto.getModePaiement()) : null);
        e.setDatePaiementReel(dto.getDatePaiementReel());
        e.setReference(dto.getReference());

        // ⚠️ Le Paiement est associé ailleurs (createEcheance)
        return e;
    }

    /* =========================================
     * ==============  HELPERS  ================
     * ========================================= */

    /** Normalise le mode pour rester simple côté front : cb / Virement / Espèces */
    private String normalizeMode(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return "";

        // tolérer "stripe" comme "cb"
        if (v.equals("stripe") || v.equals("carte") || v.equals("carte bancaire") || v.equals("cb") || v.equals("cartebancaire")) {
            return "cb";
        }
        if (v.equals("virement")) return "Virement";
        if (v.equals("especes") || v.equals("espèces")) return "Espèces";

        // valeur libre telle quelle
        return raw;
    }
}
