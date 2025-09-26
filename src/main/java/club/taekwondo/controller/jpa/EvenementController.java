package club.taekwondo.controller.jpa;

import club.taekwondo.dto.EvenementDTO;
import club.taekwondo.service.jpa.EvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/evenements")
public class EvenementController {

    @Autowired
    private EvenementService evenementService;

    // 🔹 Récupérer tous les événements
    @GetMapping
    public ResponseEntity<List<EvenementDTO>> getAllEvenements() {
        List<EvenementDTO> evenements = evenementService.getAllEvenements();
        return ResponseEntity.ok(evenements);
    }

    // 🔹 Récupérer un événement par ID
    @GetMapping("/{id}")
    public ResponseEntity<EvenementDTO> getEvenementById(@PathVariable Long id) {
        Optional<EvenementDTO> evenement = evenementService.getEvenementById(id);
        return evenement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 🔹 Récupérer seulement les événements actifs
    @GetMapping("/actifs")
    public ResponseEntity<List<EvenementDTO>> getEvenementsActifs() {
        List<EvenementDTO> evenements = evenementService.getEvenementsActifs();
        return ResponseEntity.ok(evenements);
    }

    // ✅ Nouvelle méthode conforme au FormData Angular
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvenementDTO> ajouterEvenement(
            @RequestParam("titre") String titre,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam("lieu") String lieu,
            @RequestParam("capacite") int capacite,
            @RequestParam("description") String description,
            @RequestParam("image") MultipartFile image
    ) {
        try {
            EvenementDTO created = evenementService.ajouterEvenement(
                titre, dateDebut, dateFin, lieu, capacite, description, image
            );
            return ResponseEntity.status(201).body(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🔹 Mettre à jour un événement
    @PutMapping("/{id}")
    public ResponseEntity<EvenementDTO> updateEvenement(@PathVariable Long id, @RequestBody EvenementDTO dto) {
        try {
            EvenementDTO updated = evenementService.updateEvenement(id, dto);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Changer le statut actif/inactif d'un événement
    @PutMapping("/{id}/statut")
    public ResponseEntity<EvenementDTO> changerStatutEvenement(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> statutMap) {
        try {
            Boolean actif = statutMap.get("actif");
            EvenementDTO updated = evenementService.changerStatutEvenement(id, actif);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🔹 Supprimer un événement
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvenement(@PathVariable Long id) {
        evenementService.deleteEvenement(id);
        return ResponseEntity.noContent().build();
    }
}
