package club.taekwondo.controller.jpa;

import club.taekwondo.dto.NotificationDTO;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.service.jpa.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    // 🔹 Envoyer une notification à un utilisateur
    @PostMapping("/envoyer")
    public ResponseEntity<?> envoyerNotification(@RequestParam Long utilisateurId, @RequestParam String message) {
        try {
            NotificationDTO dto = notificationService.envoyerNotification(utilisateurId, message);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Récupérer les notifications non lues d'un utilisateur
    @GetMapping("/utilisateur/{id}")
    public ResponseEntity<List<NotificationDTO>> getNotificationsUtilisateur(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            Utilisateur caller = utilisateurRepository.findByEmail(authentication.getName()).orElse(null);
            if (caller == null || !caller.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        List<NotificationDTO> notifications = notificationService.getToutesNotificationsUtilisateur(id);
        return ResponseEntity.ok(notifications);
    }

    // 🔹 Marquer une notification comme lue
    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerCommeLue(@PathVariable Long id, Authentication authentication) {
        try {
            ResponseEntity<?> refus = refuserSiPasProprietaire(id, authentication);
            if (refus != null) return refus;
            notificationService.marquerCommeLue(id);
            return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Supprimer une notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        try {
            ResponseEntity<?> refus = refuserSiPasProprietaire(id, authentication);
            if (refus != null) return refus;
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(Map.of("message", "Notification supprimée avec succès."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Vérifie que l'appelant est propriétaire de la notification (ou admin/super-admin) ;
    // renvoie une réponse 401/403 à retourner tel quel si l'accès doit être refusé, sinon null.
    private ResponseEntity<?> refuserSiPasProprietaire(Long notificationId, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (isAdmin) return null;
        Utilisateur caller = utilisateurRepository.findByEmail(authentication.getName()).orElse(null);
        Long ownerId = notificationService.getOwnerId(notificationId);
        if (caller == null || !caller.getId().equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return null;
    }

    // ========== NOUVEAUX ENDPOINTS POUR LE FRONTEND ==========

    // 🔹 Récupérer TOUTES les notifications pour l'utilisateur connecté (endpoint frontend)
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        // Récupérer l'utilisateur connecté par son email/username
        String username = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        
        List<NotificationDTO> notifications = notificationService.getToutesNotificationsUtilisateur(utilisateur.getId());
        return ResponseEntity.ok(notifications);
    }

    // 🔹 Marquer une notification comme lue (endpoint frontend)
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            ResponseEntity<?> refus = refuserSiPasProprietaire(id, authentication);
            if (refus != null) return refus;
            notificationService.marquerCommeLue(id);
            return ResponseEntity.ok(Map.of("message", "Notification marquée comme lue"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    // 🔹 Marquer toutes les notifications comme lues (endpoint frontend)
    @PutMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        
        String username = authentication.getName();
        Utilisateur utilisateur = utilisateurRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        
        notificationService.marquerToutesCommeLues(utilisateur.getId());
        return ResponseEntity.ok(Map.of("message", "Toutes les notifications marquées comme lues"));
    }
}