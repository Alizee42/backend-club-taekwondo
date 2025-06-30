package club.taekwondo.service.jpa;

import club.taekwondo.dto.AvisDTO;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.repository.jpa.AvisRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AvisService {

    @Autowired
    private AvisRepository avisRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Récupérer tous les avis en DTO
    public List<AvisDTO> getAllAvis() {
        return avisRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un avis par ID
    public Optional<AvisDTO> getAvisById(Integer id) {
        return avisRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Ajouter un nouvel avis via DTO
    public AvisDTO createAvis(AvisDTO avisDTO) {
        Avis avis = convertToEntity(avisDTO);
        Avis saved = avisRepository.save(avis);
        return convertToDTO(saved);
    }

    // 🔹 Ajouter un nouvel avis depuis le Controller (sans DTO)
    public Avis ajouterAvis(Avis avis) {
        return avisRepository.save(avis);
    }

    // 🔹 Mettre à jour un avis
    public AvisDTO updateAvis(Integer id, AvisDTO avisDTO) {
        Avis avis = avisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));

        avis.setContenu(avisDTO.getContenu());
        avis.setPseudoVisiteur(avisDTO.getPseudoVisiteur());
        avis.setNote(avisDTO.getNote());
        avis.setTypeAvis(avisDTO.getTypeAvis());
        avis.setPhoto(avisDTO.getPhoto());

        Avis updated = avisRepository.save(avis);
        return convertToDTO(updated);
    }

    // 🔹 Approuver un avis
    public AvisDTO approuverAvis(Integer id) {
        Avis avis = avisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Avis non trouvé avec l'ID : " + id));

        avis.setApprouve(true);
        Avis updated = avisRepository.save(avis);
        return convertToDTO(updated);
    }

    // 🔹 Supprimer un avis
    public void deleteAvis(Integer id) {
        avisRepository.deleteById(id);
    }

    // 🔁 Convertisseur : Avis → AvisDTO
    private AvisDTO convertToDTO(Avis avis) {
        AvisDTO avisDTO = new AvisDTO();
        avisDTO.setId(avis.getId());
        avisDTO.setDatePub(avis.getDatePub());
        avisDTO.setContenu(avis.getContenu());
        avisDTO.setApprouve(avis.getApprouve());
        avisDTO.setPseudoVisiteur(avis.getPseudoVisiteur());
        avisDTO.setNote(avis.getNote());
        avisDTO.setTypeAvis(avis.getTypeAvis());
        avisDTO.setPhoto(avis.getPhoto());
        if (avis.getUtilisateur() != null) {
            avisDTO.setUtilisateurId(avis.getUtilisateur().getId());
        }
        return avisDTO;
    }

    // 🔁 Convertisseur : AvisDTO → Avis
    private Avis convertToEntity(AvisDTO avisDTO) {
        Avis avis = new Avis();
        avis.setDatePub(avisDTO.getDatePub());
        avis.setContenu(avisDTO.getContenu());
        avis.setApprouve(avisDTO.getApprouve() != null ? avisDTO.getApprouve() : false);
        avis.setPseudoVisiteur(avisDTO.getPseudoVisiteur());
        avis.setNote(avisDTO.getNote());
        avis.setTypeAvis(avisDTO.getTypeAvis());
        avis.setPhoto(avisDTO.getPhoto());
        if (avisDTO.getUtilisateurId() != null) {
            utilisateurRepository.findById(avisDTO.getUtilisateurId())
                .ifPresent(avis::setUtilisateur);
        }
        return avis;
    }
}

