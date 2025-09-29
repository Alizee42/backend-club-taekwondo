package club.taekwondo.service.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EvenementService {

    private static final String UPLOAD_DIR = "uploads/evenements/";

    @Autowired
    private EvenementRepository evenementRepository;
    
    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    // 🔹 Nouvelle méthode adaptée à l'envoi multipart
    public EvenementDTO ajouterEvenement(String titre, String dateDebut, String dateFin, String lieu,
                                         int capacite, String description, MultipartFile imageFile) {
        String imageFilename = saveImage(imageFile);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        Evenement evenement = new Evenement();
        evenement.setTitre(titre);
        evenement.setDateDebut(LocalDateTime.parse(dateDebut, formatter));
        evenement.setDateFin(LocalDateTime.parse(dateFin, formatter));
        evenement.setLieu(lieu);
        evenement.setCapacite(capacite);
        evenement.setDescription(description);
        evenement.setImageFilename(imageFilename);
        evenement.setActif(true); // Par défaut actif

        Evenement saved = evenementRepository.save(evenement);
        return convertToDTO(saved);
    }

    // 🔹 Récupérer tous les événements
    public List<EvenementDTO> getAllEvenements() {
        return evenementRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer seulement les événements actifs
    public List<EvenementDTO> getEvenementsActifs() {
        return evenementRepository.findAll().stream()
                .filter(e -> e.getActif() == null || e.getActif()) // Inclure les événements sans statut défini
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un événement par ID
    public Optional<EvenementDTO> getEvenementById(Long id) {
        return evenementRepository.findById(id).map(this::convertToDTO);
    }

    // 🔹 Créer un événement via DTO
    public EvenementDTO createEvenement(EvenementDTO dto) {
        Evenement evenement = convertToEntity(dto);
        Evenement saved = evenementRepository.save(evenement);
        return convertToDTO(saved);
    }

    // 🔹 Mettre à jour un événement
    public EvenementDTO updateEvenement(Long id, EvenementDTO dto) {
        Evenement existing = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID : " + id));

        existing.setTitre(dto.getTitre());
        existing.setDateDebut(dto.getDateDebut());
        existing.setDateFin(dto.getDateFin());
        existing.setLieu(dto.getLieu());
        existing.setCapacite(dto.getCapacite());
        existing.setDescription(dto.getDescription());

        if (dto.getImageFilename() != null) {
            existing.setImageFilename(dto.getImageFilename());
        }

        Evenement updated = evenementRepository.save(existing);
        return convertToDTO(updated);
    }

    // 🔹 Changer le statut actif/inactif d'un événement
    public EvenementDTO changerStatutEvenement(Long id, Boolean actif) {
        Evenement existing = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID : " + id));
        
        existing.setActif(actif);
        Evenement updated = evenementRepository.save(existing);
        return convertToDTO(updated);
    }

    // 🔹 Supprimer un événement avec ses inscriptions
    public void deleteEvenement(Long id) {
        // ✅ 1. Récupérer toutes les inscriptions de cet événement
        List<InscriptionEvenement> inscriptions = inscriptionRepository.findByEvenementId(id);
        
        // ✅ 2. Supprimer toutes les inscriptions une par une
        for (InscriptionEvenement inscription : inscriptions) {
            inscriptionRepository.delete(inscription);
        }
        
        // ✅ 3. Ensuite supprimer l'événement
        evenementRepository.deleteById(id);
    }

    // 🔁 Convertisseur : Entity -> DTO
    private EvenementDTO convertToDTO(Evenement evenement) {
        EvenementDTO dto = new EvenementDTO();
        dto.setId(evenement.getId());
        dto.setTitre(evenement.getTitre());
        dto.setDateDebut(evenement.getDateDebut());
        dto.setDateFin(evenement.getDateFin());
        dto.setLieu(evenement.getLieu());
        dto.setCapacite(evenement.getCapacite());
        dto.setDescription(evenement.getDescription());
        dto.setImageFilename(evenement.getImageFilename());
        dto.setActif(evenement.getActif());

        // 🔹 Calculer le nombre d'inscrits confirmés
        long nbInscrits = inscriptionRepository.countByEvenementId(evenement.getId());
        dto.setNbInscrits((int) nbInscrits);

        if (evenement.getImageFilename() != null) {
            // 🔄 URL absolue pour que Angular accède à l'image
            String baseUrl = "http://localhost:8080"; 
            dto.setImageUrl(baseUrl + "/uploads/evenements/" + evenement.getImageFilename());
        }

        return dto;
    }

    // 🔁 Convertisseur : DTO -> Entity
    private Evenement convertToEntity(EvenementDTO dto) {
        Evenement evenement = new Evenement();
        evenement.setTitre(dto.getTitre());
        evenement.setDateDebut(dto.getDateDebut());
        evenement.setDateFin(dto.getDateFin());
        evenement.setLieu(dto.getLieu());
        evenement.setCapacite(dto.getCapacite());
        evenement.setDescription(dto.getDescription());
        evenement.setImageFilename(dto.getImageFilename());
        evenement.setActif(dto.getActif());
        return evenement;
    }

    // 📦 Enregistrement du fichier image
    public String saveImage(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + filename);
            Files.write(path, file.getBytes());
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement de l'image", e);
        }
    }
}