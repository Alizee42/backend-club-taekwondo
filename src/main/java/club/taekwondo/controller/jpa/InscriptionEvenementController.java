package club.taekwondo.controller.jpa;

import club.taekwondo.entity.jpa.InscriptionEvenement;
import club.taekwondo.service.jpa.InscriptionEvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inscriptions")
public class InscriptionEvenementController {

    @Autowired
    private InscriptionEvenementService inscriptionEvenementService;

    // Endpoint pour récupérer toutes les inscriptions
    @GetMapping
    public List<InscriptionEvenement> getAllInscriptions() {
        return inscriptionEvenementService.getAllInscriptions();
    }

    // Endpoint pour récupérer une inscription par son ID
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionEvenement> getInscriptionById(@PathVariable Long id) {
        Optional<InscriptionEvenement> inscriptionEvenement = inscriptionEvenementService.getInscriptionById(id);
        return inscriptionEvenement.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint pour inscrire un membre à un événement
    @PostMapping
    public ResponseEntity<InscriptionEvenement> inscrireMembre(@RequestBody InscriptionEvenement inscriptionEvenement) {
        InscriptionEvenement newInscription = inscriptionEvenementService.inscrireMembre(inscriptionEvenement);
        return new ResponseEntity<>(newInscription, HttpStatus.CREATED);
    }

    // Endpoint pour mettre à jour une inscription
    @PutMapping("/{id}")
    public ResponseEntity<InscriptionEvenement> updateInscription(@PathVariable Long id, @RequestBody InscriptionEvenement inscriptionEvenement) {
        InscriptionEvenement updatedInscription = inscriptionEvenementService.updateInscription(id, inscriptionEvenement);
        return updatedInscription != null ? ResponseEntity.ok(updatedInscription) : ResponseEntity.notFound().build();
    }

    // Endpoint pour annuler une inscription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerInscription(@PathVariable Long id) {
        inscriptionEvenementService.annulerInscription(id);
        return ResponseEntity.noContent().build();
    }
}
