package club.taekwondo.service.jpa;

import club.taekwondo.dto.AvisDTO;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.repository.jpa.AvisRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvisService {

    @Autowired
    private AvisRepository avisRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // ✅ Types autorisés (optionnels)
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "cours", "entraineurs", "evenements", "organisation", "competitions"
    );

    /* ===== Helpers ===== */
    private String normalizeType(String typeAvis) {
        if (typeAvis == null) return null;
        String t = typeAvis.trim().toLowerCase();
        return (t.isBlank() || !ALLOWED_TYPES.contains(t)) ? null : t;
    }

    private int clampNote(Integer note) {
        int n = (note == null) ? 5 : note;
        return Math.max(1, Math.min(5, n));
    }

    /* ===== Read ===== */
    // Tous les avis en DTO
    public List<AvisDTO> getAllAvis() {
        return avisRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Filtré par approuve / typeAvis (optionnels)
    public List<AvisDTO> getAllAvis(Boolean approuve, String typeAvis) {
        String normType = normalizeType(typeAvis);

        List<Avis> list;
        if (approuve != null && normType != null) {
            list = avisRepository.findByApprouveAndTypeAvisIgnoreCase(approuve, normType);
        } else if (approuve != null) {
            list = avisRepository.findByApprouve(approuve);
        } else if (normType != null) {
            list = avisRepository.findByTypeAvisIgnoreCase(normType);
        } else {
            list = avisRepository.findAll();
        }

        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Compteurs (pour les badges)
    public long countAvis(Boolean approuve, String typeAvis) {
        String normType = normalizeType(typeAvis);

        if (approuve != null && normType != null) {
            return avisRepository.countByApprouveAndTypeAvisIgnoreCase(approuve, normType);
        } else if (approuve != null) {
            return avisRepository.countByApprouve(approuve);
        } else if (normType != null) {
            return avisRepository.countByTypeAvisIgnoreCase(normType);
        } else {
            return avisRepository.count();
        }
    }

    public Optional<AvisDTO> getAvisById(Integer id) {
        return avisRepository.findById(id).map(this::convertToDTO);
    }

    /* ===== Create / Update / Delete ===== */
    // via DTO
    public AvisDTO createAvis(AvisDTO avisDTO) {
        Avis avis = convertToEntity(avisDTO);
        // sécurise les champs
        avis.setTypeAvis(normalizeType(avis.getTypeAvis()));
        avis.setNote(clampNote(avis.getNote()));
        if (avis.getApprouve() == null) avis.setApprouve(false);

        Avis saved = avisRepository.save(avis);
        return convertToDTO(saved);
    }

    // depuis Controller (multipart)
    public Avis ajouterAvis(Avis avis) {
        avis.setTypeAvis(normalizeType(avis.getTypeAvis()));
        avis.setNote(clampNote(avis.getNote()));
        if (avis.getApprouve() == null) avis.setApprouve(false);
        return avisRepository.save(avis);
    }

    public AvisDTO updateAvis(Integer id, AvisDTO avisDTO) {
        Avis avis = avisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));

        avis.setContenu(avisDTO.getContenu());
        avis.setPseudoVisiteur(avisDTO.getPseudoVisiteur());
        avis.setNote(clampNote(avisDTO.getNote()));
        avis.setTypeAvis(normalizeType(avisDTO.getTypeAvis()));
        avis.setPhoto(avisDTO.getPhoto());
        // on ne touche pas à approuve ici (sauf si tu veux permettre de le changer via DTO)

        Avis updated = avisRepository.save(avis);
        return convertToDTO(updated);
    }

    public AvisDTO approuverAvis(Integer id) {
        Avis avis = avisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));
        avis.setApprouve(true);
        Avis updated = avisRepository.save(avis);
        return convertToDTO(updated);
    }

    public void deleteAvis(Integer id) {
        avisRepository.deleteById(id);
    }

    /* ===== Mapping ===== */
    private AvisDTO convertToDTO(Avis avis) {
        AvisDTO dto = new AvisDTO();
        dto.setId(avis.getId());
        dto.setDatePub(avis.getDatePub());
        dto.setContenu(avis.getContenu());
        dto.setApprouve(avis.getApprouve());
        dto.setPseudoVisiteur(avis.getPseudoVisiteur());
        dto.setNote(avis.getNote());
        dto.setTypeAvis(avis.getTypeAvis());
        dto.setPhoto(avis.getPhoto());
        if (avis.getUtilisateur() != null) {
            dto.setUtilisateurId(avis.getUtilisateur().getId());
        }
        return dto;
    }

    private Avis convertToEntity(AvisDTO avisDTO) {
        Avis avis = new Avis();
        avis.setDatePub(avisDTO.getDatePub());
        avis.setContenu(avisDTO.getContenu());
        avis.setApprouve(avisDTO.getApprouve() != null ? avisDTO.getApprouve() : false);
        avis.setPseudoVisiteur(avisDTO.getPseudoVisiteur());
        avis.setNote(clampNote(avisDTO.getNote()));
        avis.setTypeAvis(normalizeType(avisDTO.getTypeAvis()));
        avis.setPhoto(avisDTO.getPhoto());
        if (avisDTO.getUtilisateurId() != null) {
            utilisateurRepository.findById(avisDTO.getUtilisateurId())
                    .ifPresent(avis::setUtilisateur);
        }
        return avis;
    }
}