package club.taekwondo.controller;

import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.EvenementRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardAdminController {

    private final MembreRepository membreRepository;
    private final PaiementRepository paiementRepository;
    private final EvenementRepository evenementRepository;

    public DashboardAdminController(MembreRepository membreRepository,
                                    PaiementRepository paiementRepository,
                                    EvenementRepository evenementRepository) {
        this.membreRepository = membreRepository;
        this.paiementRepository = paiementRepository;
        this.evenementRepository = evenementRepository;
    }

    @PreAuthorize("hasRole('ADMIN')") // 🔒 Ajout sécurité
    @GetMapping("/admin")
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        long nbMembres = membreRepository.count();

        Double totalPaiements = paiementRepository.sumMontantByStatut("payé");
        if (totalPaiements == null) totalPaiements = 0.0;

        long paiementsAttente = paiementRepository.countByStatutIgnoreCase("en attente");

        long evenementsAVenir = evenementRepository.countByDateDebutAfter(LocalDateTime.now());

        stats.put("nbMembres", nbMembres);
        stats.put("totalPaiements", totalPaiements);
        stats.put("paiementsAttente", paiementsAttente);
        stats.put("evenementsAVenir", evenementsAVenir);

        return stats;
    }
}