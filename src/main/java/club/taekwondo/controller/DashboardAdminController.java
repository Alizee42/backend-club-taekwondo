package club.taekwondo.controller;

import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.EvenementRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
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

    @GetMapping("/admin")
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1️⃣ Total membres
        long nbMembres = membreRepository.count();

        // 3️⃣ Total paiements reçus (uniquement ceux "payé")
        Double totalPaiements = paiementRepository.sumMontantByStatut("payé");
        if (totalPaiements == null) totalPaiements = 0.0;

        // 4️⃣ Paiements en attente
        long paiementsAttente = paiementRepository.countByStatutIgnoreCase("en attente");

        // 5️⃣ Événements à venir (date de début dans le futur)
        long evenementsAVenir = evenementRepository.countByDateDebutAfter(LocalDateTime.now());

        // 🔹 Remplissage du Map
        stats.put("nbMembres", nbMembres);
        stats.put("totalPaiements", totalPaiements);
        stats.put("paiementsAttente", paiementsAttente);
        stats.put("evenementsAVenir", evenementsAVenir);

        return stats;
    }
}

