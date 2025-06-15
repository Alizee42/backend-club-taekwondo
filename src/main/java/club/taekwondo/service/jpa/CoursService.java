package club.taekwondo.service.jpa;

import club.taekwondo.dto.CoursDTO;
import club.taekwondo.entity.jpa.Cours;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.CoursRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CoursService {

    @Autowired
    private CoursRepository coursRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public List<CoursDTO> getAllCours() {
        return coursRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<CoursDTO> getCoursById(Long id) {
        return coursRepository.findById(id).map(this::toDTO);
    }

    public CoursDTO createCours(CoursDTO coursDTO) {
        Cours saved = coursRepository.save(toEntity(coursDTO));
        return toDTO(saved);
    }

    public CoursDTO updateCours(Long id, CoursDTO coursDTO) {
        if (coursRepository.existsById(id)) {
            Cours cours = toEntity(coursDTO);
            cours.setId(id);
            return toDTO(coursRepository.save(cours));
        }
        return null;
    }

    public void deleteCours(Long id) {
        coursRepository.deleteById(id);
    }

    // Conversion Entity -> DTO
    private CoursDTO toDTO(Cours cours) {
        CoursDTO coursDTO = new CoursDTO();
        coursDTO.setId(cours.getId());
        coursDTO.setNomCours(cours.getNomCours());
        coursDTO.setDescription(cours.getDescription());
        coursDTO.setDate(cours.getDate());
        coursDTO.setDuree(cours.getDuree());
        coursDTO.setHoraire(cours.getHoraire());
        coursDTO.setNiveau(cours.getNiveau());
        coursDTO.setCapacite(cours.getCapacite());
        if (cours.getProfesseur() != null) {
        	coursDTO.setProfesseurId(cours.getProfesseur().getId());
        }
        return coursDTO;
    }

    // Conversion DTO -> Entity
    private Cours toEntity(CoursDTO coursDTO) {
        Cours cours = new Cours();
        cours.setId(coursDTO.getId());
        cours.setNomCours(coursDTO.getNomCours());
        cours.setDescription(coursDTO.getDescription());
        cours.setDate(coursDTO.getDate());
        cours.setDuree(coursDTO.getDuree());
        cours.setHoraire(coursDTO.getHoraire());
        cours.setNiveau(coursDTO.getNiveau());
        cours.setCapacite(coursDTO.getCapacite());
        if (coursDTO.getProfesseurId() != null) {
            Optional<Utilisateur> profOpt = utilisateurRepository.findById(coursDTO.getProfesseurId());
            profOpt.ifPresent(cours::setProfesseur);
        }
        return cours;
    }
}

