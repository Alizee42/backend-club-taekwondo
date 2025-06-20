package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.dto.MembreRetardDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EcheanceService {

    @Autowired
    private EcheanceRepository echeanceRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    // 🔹 Récupérer toutes les échéances sous forme de DTO
    public List<EcheanceDTO> getAllEcheanceDTOs() {
        return echeanceRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    // 🔹 Créer une échéance (DTO → Entity) avec lien à un paiement
    public EcheanceDTO createEcheance(EcheanceDTO echeanceDTO, Long paiementId) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé avec id : " + paiementId));

        Echeance echeance = toEntity(echeanceDTO);
        echeance.setPaiement(paiement);

        return toDTO(echeanceRepository.save(echeance));
    }

    // 🔹 Mettre à jour une échéance existante
    public EcheanceDTO updateEcheance(Long id, EcheanceDTO echeanceDTO) {
        Echeance echeance = echeanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Échéance non trouvée avec id: " + id));

        echeance.setDateEcheance(echeanceDTO.getDateEcheance());
        echeance.setMontant(echeanceDTO.getMontant());
        echeance.setStatut(echeanceDTO.getStatut());
        echeance.setNumero(echeanceDTO.getNumero());

        return toDTO(echeanceRepository.save(echeance));
    }

    // 🔹 Payer une échéance spécifique
    public EcheanceDTO payerEcheance(Long echeanceId) {
        Echeance echeance = echeanceRepository.findById(echeanceId)
                .orElseThrow(() -> new RuntimeException("Échéance non trouvée avec id: " + echeanceId));

        if ("payé".equalsIgnoreCase(echeance.getStatut())) {
            throw new IllegalStateException("Cette échéance est déjà payée.");
        }

        echeance.setStatut("payé");
        echeanceRepository.save(echeance);

        Paiement paiement = echeance.getPaiement();
        paiement.setMontantRestant(Math.max(0, paiement.getMontantRestant() - echeance.getMontant()));
        paiement.setEcheancesRestantes(Math.max(0, paiement.getEcheancesRestantes() - 1));

        if (paiement.getEcheancesRestantes() <= 0) {
            paiement.setStatut("payé");
        }

        paiementRepository.save(paiement);

        return toDTO(echeance);
    }
    public List<MembreRetardDTO> getMembresEnRetard() {
        // Récupère toutes les échéances en retard (statut = "en attente" et date antérieure à aujourd'hui)
        List<Echeance> echeancesEnRetard = echeanceRepository.findByStatutAndDateEcheanceBefore("en attente", LocalDate.now());

        System.out.println("🔍 Nombre d'échéances en retard : " + echeancesEnRetard.size());
        
        // Affiche des informations sur les échéances en retard pour debugging
        echeancesEnRetard.forEach(e -> {
            System.out.println("  - ID: " + e.getId() +
                    ", Date: " + e.getDateEcheance() +
                    ", Montant: " + e.getMontant() +
                    ", Statut: " + e.getStatut());
        });

        // Regroupe les échéances par utilisateur (par membre)
        Map<Long, List<Echeance>> echeancesParMembre = echeancesEnRetard.stream()
            .collect(Collectors.groupingBy(e -> e.getPaiement().getUtilisateur().getId()));

        List<MembreRetardDTO> retards = new ArrayList<>();
        for (Map.Entry<Long, List<Echeance>> entry : echeancesParMembre.entrySet()) {
            List<Echeance> echeances = entry.getValue();

            // Filtrer pour ne prendre en compte que les échéances non payées (statut = "en attente")
            double totalRestant = echeances.stream()
                .filter(e -> "en attente".equalsIgnoreCase(e.getStatut()))
                .mapToDouble(Echeance::getMontant)
                .sum();

            // Récupère le nom du membre
            String nom = echeances.get(0).getPaiement().getUtilisateur().getNom(); 

            // Récupère la première échéance en retard (la plus proche)
            Echeance echeanceEnRetard = echeances.stream()
                .filter(e -> e.getDateEcheance().isBefore(LocalDate.now()) && "en attente".equalsIgnoreCase(e.getStatut()))
                .findFirst().orElse(null);

            if (echeanceEnRetard != null) {
                // Crée un MembreRetardDTO avec les informations de l'échéance en retard
                retards.add(new MembreRetardDTO(nom, totalRestant, echeanceEnRetard.getDateEcheance(), echeanceEnRetard.getMontant()));
            }
        }

        System.out.println("📊 Membres en retard générés : " + retards.size());
        return retards;
    }


    // 🔁 Entity → DTO
    private EcheanceDTO toDTO(Echeance echeance) {
        System.out.println("Conversion de l'entité Echeance en DTO : " + echeance);
        EcheanceDTO echeanceDTO = new EcheanceDTO();
        echeanceDTO.setId(echeance.getId());
        echeanceDTO.setDateEcheance(echeance.getDateEcheance());
        echeanceDTO.setMontant(echeance.getMontant());
        echeanceDTO.setStatut(echeance.getStatut());
        echeanceDTO.setNumero(echeance.getNumero());
        System.out.println("DTO généré : " + echeanceDTO);
        return echeanceDTO;
    }

    // 🔁 DTO → Entity (paiement à associer manuellement)
    private Echeance toEntity(EcheanceDTO echeanceDTO) {
        Echeance echeance = new Echeance();
        echeance.setId(echeanceDTO.getId());
        echeance.setDateEcheance(echeanceDTO.getDateEcheance());
        echeance.setMontant(echeanceDTO.getMontant());
        echeance.setStatut(echeanceDTO.getStatut());
        echeance.setNumero(echeanceDTO.getNumero());
        return echeance;
    }
}


