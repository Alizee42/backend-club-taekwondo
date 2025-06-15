package club.taekwondo.service.jpa;

import club.taekwondo.dto.EcheanceDTO;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.repository.jpa.EcheanceRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    // 🔁 Entity → DTO
    private EcheanceDTO toDTO(Echeance echeance) {
        EcheanceDTO echeanceDTO = new EcheanceDTO();
        echeanceDTO.setId(echeance.getId());
        echeanceDTO.setDateEcheance(echeance.getDateEcheance());
        echeanceDTO.setMontant(echeance.getMontant());
        echeanceDTO.setStatut(echeance.getStatut());
        echeanceDTO.setNumero(echeance.getNumero());
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


