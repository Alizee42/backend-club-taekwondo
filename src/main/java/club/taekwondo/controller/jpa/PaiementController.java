package club.taekwondo.controller.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.service.jpa.PaiementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/paiements")
@CrossOrigin(origins = "*")
public class PaiementController {

    private final PaiementService paiementService;
    private final ObjectMapper objectMapper;

    public PaiementController(PaiementService paiementService, ObjectMapper objectMapper) {
        this.paiementService = paiementService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<PaiementDTO> getAll() {
        return paiementService.getAllWithEcheances();
    }

    @PostMapping("/{id}/payer-echeance")
    public ResponseEntity<PaiementDTO> payerEcheance(
            @PathVariable Long id,
            @RequestBody List<Map<String, Object>> echeancesPayees) {

        Optional<Paiement> optPaiement = paiementService.getById(id);
        if (optPaiement.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        Paiement paiement = optPaiement.get();
        double montantTotalAPayer = 0.0;
        int nombreEcheancesPayees = 0;

        for (Map<String, Object> echeanceData : echeancesPayees) {
            Long echeanceId = Long.parseLong(echeanceData.get("id").toString());
            Optional<Echeance> optEcheance = paiement.getEcheances().stream()
                    .filter(e -> e.getId().equals(echeanceId))
                    .findFirst();

            if (optEcheance.isPresent()) {
                Echeance echeance = optEcheance.get();
                if (!"payé".equalsIgnoreCase(echeance.getStatut())) {
                    echeance.setStatut("payé");
                    montantTotalAPayer += echeance.getMontant();
                    nombreEcheancesPayees++;
                }
            } else {
                return ResponseEntity.badRequest().body(null);
            }
        }

        paiement.setMontantRestant(Math.max(0, paiement.getMontantRestant() - montantTotalAPayer));
        paiement.setEcheancesRestantes(Math.max(0, paiement.getEcheancesRestantes() - nombreEcheancesPayees));

        if (paiement.getMontantRestant() <= 0 || paiement.getEcheancesRestantes() <= 0) {
            paiement.setStatut("payé");
            paiement.setMontantRestant(0.0);
            paiement.setEcheancesRestantes(0);
        }

        Paiement saved = paiementService.save(paiement);
        return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<PaiementDTO>> filterPaiements(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String modePaiement) {
        List<Paiement> paiements = paiementService.filterPaiements(statut, modePaiement);
        List<PaiementDTO> dtos = paiements.stream().map(paiementService::toPaiementDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<PaiementDTO> validerPaiement(@PathVariable Long id) {
        return paiementService.getById(id)
                .map(p -> {
                    p.setStatut("payé");
                    p.setEcheancesRestantes(0);
                    p.setMontantRestant(0.0);
                    Paiement saved = paiementService.save(p);
                    return ResponseEntity.ok(paiementService.toPaiementDTO(saved));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ✅ Nouvelle méthode d’annulation avec AnnulationRequestDTO
    @PutMapping("/{id}/annuler")
    public ResponseEntity<PaiementDTO> annulerPaiement(
            @PathVariable Long id,
            @RequestBody AnnulationRequestDTO request) {
        try {
            PaiementDTO updated = paiementService.annulerPaiement(id, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/dashboard")
    public DashboardStatsDTO getDashboardStats() {
        return paiementService.buildDashboardStats();
    }

    @PostMapping("/ajouter-manuel")
    public ResponseEntity<PaiementDTO> ajouterPaiementManuel(@RequestBody PaiementDTO dto) {
        try {
            Paiement paiement = paiementService.ajouterPaiementManuel(dto);
            return ResponseEntity.ok(paiementService.toPaiementDTO(paiement));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/ajouter-complet")
    public ResponseEntity<PaiementDTO> ajouterPaiementComplet(
            @RequestParam String utilisateurNom,
            @RequestParam String utilisateurPrenom,
            @RequestParam(required = false) String utilisateurEmail,
            @RequestParam String type,
            @RequestParam Double montantTotal,
            @RequestParam String modePaiement,
            @RequestParam String datePaiement,
            @RequestParam(required = false) String echeances,
            @RequestParam(required = false) MultipartFile justificatif
    ) {
        try {
            PaiementDTO dto = new PaiementDTO();
            dto.setUtilisateurNom(utilisateurNom);
            dto.setUtilisateurPrenom(utilisateurPrenom);
            dto.setUtilisateurEmail(utilisateurEmail);
            dto.setType(type);
            dto.setMontantTotal(montantTotal);
            dto.setModePaiement(modePaiement);
            dto.setDatePaiement(LocalDate.parse(datePaiement));

            if (echeances != null && !echeances.isEmpty()) {
                List<Map<String, Object>> parsed = objectMapper.readValue(
                        echeances,
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                List<Echeance> echeanceList = new ArrayList<>();
                for (int i = 0; i < parsed.size(); i++) {
                    Map<String, Object> e = parsed.get(i);
                    Echeance echeance = new Echeance();
                    echeance.setDateEcheance(LocalDate.parse((String) e.get("dateEcheance")));
                    echeance.setMontant(Double.parseDouble(e.get("montant").toString()));
                    String statutStr = (String) e.get("statut");
                    echeance.setStatut(statutStr != null ? statutStr : "en attente");
                    echeance.setNumero(i + 1);
                    echeanceList.add(echeance);
                }

                dto.setEcheances(echeanceList.stream().map(e -> {
                    EcheanceDTO edto = new EcheanceDTO();
                    edto.setDateEcheance(e.getDateEcheance());
                    edto.setMontant(e.getMontant());
                    edto.setStatut(e.getStatut());
                    edto.setNumero(e.getNumero());
                    return edto;
                }).toList());
            }

            Paiement paiement = paiementService.ajouterPaiementManuel(dto);
            return ResponseEntity.ok(paiementService.toPaiementDTO(paiement));

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout complet : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(@PathVariable Long id) {
        try {
            paiementService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


