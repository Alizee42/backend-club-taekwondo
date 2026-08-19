package club.taekwondo.controller;

import club.taekwondo.entity.jpa.Club;
import club.taekwondo.entity.jpa.Evenement;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.EvenementRepository;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardAdminController {

    private final MembreRepository membreRepository;
    private final PaiementRepository paiementRepository;
    private final EvenementRepository evenementRepository;
    private final UtilisateurService utilisateurService;

    public DashboardAdminController(MembreRepository membreRepository,
                                    PaiementRepository paiementRepository,
                                    EvenementRepository evenementRepository,
                                    UtilisateurService utilisateurService) {
        this.membreRepository = membreRepository;
        this.paiementRepository = paiementRepository;
        this.evenementRepository = evenementRepository;
        this.utilisateurService = utilisateurService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAdminStats(Authentication authentication) {
        Utilisateur admin = utilisateurService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifie introuvable"));

        Long clubId = getClubId(admin);
        if (clubId == null) {
            return ResponseEntity.ok(emptyStats());
        }

        Map<String, Object> stats = new HashMap<>();
        List<Paiement> paiementsClub = paiementRepository.findByClubIdAny(clubId);
        List<Evenement> evenementsClub = evenementRepository.findByClub_Id(clubId);

        long nbMembres = membreRepository.findByClub_Id(clubId).size();
        double totalPaiements = paiementsClub.stream()
                .filter(paiement -> "payé".equalsIgnoreCase(paiement.getStatut()))
                .map(Paiement::getMontantTotal)
                .filter(montant -> montant != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        long paiementsAttente = paiementsClub.stream()
                .filter(paiement -> "en attente".equalsIgnoreCase(paiement.getStatut()))
                .count();
        long evenementsAVenir = evenementsClub.stream()
                .map(Evenement::getDateDebut)
                .filter(date -> date != null && date.isAfter(LocalDateTime.now()))
                .count();

        stats.put("nbMembres", nbMembres);
        stats.put("totalPaiements", totalPaiements);
        stats.put("paiementsAttente", paiementsAttente);
        stats.put("evenementsAVenir", evenementsAVenir);

        return ResponseEntity.ok(stats);
    }

    private Long getClubId(Utilisateur utilisateur) {
        Club club = utilisateur.getClub();
        return club != null ? club.getId() : null;
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("nbMembres", 0L);
        stats.put("totalPaiements", 0.0d);
        stats.put("paiementsAttente", 0L);
        stats.put("evenementsAVenir", 0L);
        return stats;
    }
}
