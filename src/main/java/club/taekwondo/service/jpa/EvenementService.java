package club.taekwondo.service.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.ClubRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.InscriptionEvenementRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
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
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ClubRepository clubRepository;

    // 🔹 Nouvelle méthode adaptée à l'envoi multipart
    public EvenementDTO ajouterEvenement(String titre, String dateDebut, String dateFin, String lieu,
                                         int capacite, String description, MultipartFile imageFile,
                                         Long clubId) {
        String imageFilename = saveImage(imageFile);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new RuntimeException("Club non trouvÃ© avec l'ID : " + clubId));

        Evenement evenement = new Evenement();
        evenement.setTitre(titre);
        evenement.setDateDebut(LocalDateTime.parse(dateDebut, formatter));
        evenement.setDateFin(LocalDateTime.parse(dateFin, formatter));
        evenement.setLieu(lieu);
        evenement.setCapacite(capacite);
        evenement.setDescription(description);
        evenement.setImageFilename(imageFilename);
        evenement.setClub(club);
        evenement.setActif(true); // Par défaut actif

        Evenement saved = evenementRepository.save(evenement);
        
        // 🔔 Envoyer notification à tous les utilisateurs
        System.out.println("🚀 Événement créé, envoi des notifications...");
        envoyerNotificationNouvelEvenement(saved);
        
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

    public Optional<Evenement> getEvenementEntityById(Long id) {
        return evenementRepository.findById(id);
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
        // Sauvegarder l'état avant modification pour détecter un vrai changement
        String oldTitre = existing.getTitre();
        var oldDateDebut = existing.getDateDebut();
        var oldDateFin = existing.getDateFin();
        String oldLieu = existing.getLieu();
        Integer oldCapacite = existing.getCapacite();
        String oldDescription = existing.getDescription();
        String oldImage = existing.getImageFilename();

        existing.setTitre(dto.getTitre());
        existing.setDateDebut(dto.getDateDebut());
        existing.setDateFin(dto.getDateFin());
        existing.setLieu(dto.getLieu());
        existing.setCapacite(dto.getCapacite());
        existing.setDescription(dto.getDescription());

        if (dto.getImageFilename() != null) {
            existing.setImageFilename(dto.getImageFilename());
        }

        boolean modifie = !equalsObj(oldTitre, dto.getTitre())
                || !equalsObj(oldDateDebut, dto.getDateDebut())
                || !equalsObj(oldDateFin, dto.getDateFin())
                || !equalsObj(oldLieu, dto.getLieu())
                || !equalsObj(oldCapacite, dto.getCapacite())
                || !equalsObj(oldDescription, dto.getDescription())
                || (dto.getImageFilename() != null && !equalsObj(oldImage, dto.getImageFilename()));

        Evenement updated = evenementRepository.save(existing);

        // 🔔 Notifier uniquement si au moins un champ a vraiment changé
        if (modifie) {
            envoyerNotificationModificationEvenement(updated);
        }

        return convertToDTO(updated);
    }

    // 🔹 Changer le statut actif/inactif d'un événement
    public EvenementDTO changerStatutEvenement(Long id, Boolean actif) {
        Evenement existing = evenementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé avec l'ID : " + id));
        
        existing.setActif(actif);
        Evenement updated = evenementRepository.save(existing);
        
        // 🔔 Envoyer notification si événement annulé
        if (!actif) {
            envoyerNotificationAnnulationEvenement(updated);
        }
        
        return convertToDTO(updated);
    }

    // 🔹 Supprimer un événement avec ses inscriptions
    public void deleteEvenement(Long id) {
        // ✅ 0. Vérifier que l'événement existe
        if (!evenementRepository.existsById(id)) {
            throw new RuntimeException("Événement avec l'ID " + id + " n'existe pas");
        }
        
        try {
            // ✅ 1. Supprimer directement toutes les inscriptions par requête SQL
            // Cela évite de charger les relations et contourne le problème du membre ID 0
            inscriptionRepository.deleteByEvenementId(id);
            System.out.println("🔍 Toutes les inscriptions de l'événement " + id + " ont été supprimées");
            
            // ✅ 2. Ensuite supprimer l'événement
            evenementRepository.deleteById(id);
            System.out.println("✅ Événement " + id + " supprimé avec succès");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la suppression de l'événement " + id + ": " + e.getMessage());
            throw new RuntimeException("Impossible de supprimer l'événement: " + e.getMessage(), e);
        }
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
        long nbInscrits = inscriptionRepository.countActiveByEvenementId(evenement.getId());
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
        if (file == null || file.isEmpty()) {
            return null;
        }

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
    
    // ========== MÉTHODES DE NOTIFICATION ==========
    
    // 🔔 Notification : Nouvel événement créé
    private void envoyerNotificationNouvelEvenement(Evenement evenement) {
        try {
            List<Utilisateur> tousUtilisateurs = utilisateurRepository.findAll();
            String titre = "Nouvel événement disponible";
            String message = "Un nouvel événement '" + evenement.getTitre() + "' a été créé. Inscriptions ouvertes !";
            
            for (Utilisateur utilisateur : tousUtilisateurs) {
                notificationService.envoyerNotification(
                    utilisateur.getId(), 
                    titre, 
                    message, 
                    "EVENEMENT"
                );
            }
            System.out.println("🔔 Notifications envoyées pour le nouvel événement : " + evenement.getTitre());
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi des notifications : " + e.getMessage());
        }
    }
    
    // 🔔 Notification : Événement modifié
    private void envoyerNotificationModificationEvenement(Evenement evenement) {
        try {
            // Notifier uniquement les personnes inscrites
            List<InscriptionEvenement> inscriptions = inscriptionRepository.findByEvenementId(evenement.getId());
            String titre = "Événement modifié";
            String message = "L'événement '" + evenement.getTitre() + "' a été modifié. Vérifiez les détails.";
            
            for (InscriptionEvenement inscription : inscriptions) {
                if (inscription.getMembre() != null && inscription.getMembre().getCompteUtilisateur() != null) {
                    notificationService.envoyerNotification(
                        inscription.getMembre().getCompteUtilisateur().getId(),
                        titre,
                        message,
                        "EVENEMENT"
                    );
                }
            }
            System.out.println("🔔 Notifications de modification envoyées pour : " + evenement.getTitre());
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi des notifications de modification : " + e.getMessage());
        }
    }
    
    // 🔔 Notification : Événement annulé
    private void envoyerNotificationAnnulationEvenement(Evenement evenement) {
        try {
            // Notifier uniquement les personnes inscrites
            List<InscriptionEvenement> inscriptions = inscriptionRepository.findByEvenementId(evenement.getId());
            String titre = "Événement annulé";
            String message = "L'événement '" + evenement.getTitre() + "' a été annulé. Nous nous excusons pour la gêne occasionnée.";
            
            for (InscriptionEvenement inscription : inscriptions) {
                if (inscription.getMembre() != null && inscription.getMembre().getCompteUtilisateur() != null) {
                    notificationService.envoyerNotification(
                        inscription.getMembre().getCompteUtilisateur().getId(),
                        titre,
                        message,
                        "EVENEMENT"
                    );
                }
            }
            System.out.println("🔔 Notifications d'annulation envoyées pour : " + evenement.getTitre());
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi des notifications d'annulation : " + e.getMessage());
        }
    }

    // 🔹 Récupérer tous les événements d'un club
    public List<EvenementDTO> getEvenementsByClubId(Long clubId) {
        return evenementRepository.findByClub_Id(clubId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ Helper null-safe pour comparer des objets
    private static boolean equalsObj(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
