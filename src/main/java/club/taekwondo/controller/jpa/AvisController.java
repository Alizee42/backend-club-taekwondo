package club.taekwondo.controller.jpa;

import club.taekwondo.dto.AvisDTO;
import club.taekwondo.entity.jpa.Avis;
import club.taekwondo.service.jpa.AvisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/avis")
public class AvisController {

    @Autowired
    private AvisService avisService;

    // 🔹 Récupérer tous les avis
    @GetMapping
    public ResponseEntity<List<AvisDTO>> getAllAvis() {
        List<AvisDTO> avisList = avisService.getAllAvis();
        return ResponseEntity.ok(avisList);
    }

    // 🔹 Ajouter un nouvel avis avec upload d'image
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createAvisAvecFichier(
            @RequestParam("contenu") String contenu,
            @RequestParam("note") Integer note,
            @RequestParam("pseudoVisiteur") String pseudoVisiteur,
            @RequestParam(value = "typeAvis", required = false) String typeAvis,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile
    ) {
        try {
            String nomFichier = null;
            if (photoFile != null && !photoFile.isEmpty()) {
                String dossier = "uploads/avis/";
                Files.createDirectories(Paths.get(dossier));
                nomFichier = UUID.randomUUID() + "_" + photoFile.getOriginalFilename();
                Path chemin = Paths.get(dossier + nomFichier);
                photoFile.transferTo(chemin);
            }

            Avis avis = new Avis();
            avis.setContenu(contenu);
            avis.setNote(note);
            avis.setPseudoVisiteur(pseudoVisiteur);
            avis.setTypeAvis(typeAvis);
            avis.setDatePub(LocalDate.now());
            avis.setApprouve(false);
            avis.setPhoto(nomFichier); // nom du fichier

            avisService.ajouterAvis(avis);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'envoi de l'avis : " + e.getMessage());
        }
    }

    // 🔹 Mettre à jour un avis existant
    @PutMapping("/{id}")
    public ResponseEntity<AvisDTO> updateAvis(@PathVariable Integer id, @RequestBody AvisDTO avisDTO) {
        try {
            AvisDTO updatedAvis = avisService.updateAvis(id, avisDTO);
            return ResponseEntity.ok(updatedAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 🔹 Approuver un avis
    @PutMapping("/{id}/approuver")
    public ResponseEntity<AvisDTO> approuverAvis(@PathVariable Integer id) {
        try {
            AvisDTO approuveAvis = avisService.approuverAvis(id);
            return ResponseEntity.ok(approuveAvis);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // 🔹 Supprimer un avis
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvis(@PathVariable Integer id) {
        avisService.deleteAvis(id);
        return ResponseEntity.noContent().build();
    }
}

