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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/avis")
public class AvisController {

    @Autowired
    private AvisService avisService;

    // Liste blanche des types autorisés
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "cours", "entraineurs", "evenements", "organisation", "competitions"
    );

    /** 🔧 Normalisation du type : lower-case, trim, renvoie null si invalide/vide */
    private String normalizeType(String typeAvis) {
        if (typeAvis == null) return null;
        String t = typeAvis.trim().toLowerCase();
        return t.isBlank() ? null : (ALLOWED_TYPES.contains(t) ? t : null);
    }

    /** 🔹 Récupérer les avis (avec filtres optionnels) :
     *  /api/avis?approuve=false
     *  /api/avis?typeAvis=cours
     *  /api/avis?approuve=true&typeAvis=evenements
     */
    @GetMapping
    public ResponseEntity<List<AvisDTO>> getAllAvis(
            @RequestParam(value = "approuve", required = false) Boolean approuve,
            @RequestParam(value = "typeAvis", required = false) String typeAvis
    ) {
        List<AvisDTO> avisList = avisService.getAllAvis();

        if (approuve != null) {
            avisList = avisList.stream()
                    .filter(a -> {
                        Boolean val = (a.getApprouve() != null) ? a.getApprouve() : Boolean.FALSE;
                        return val.equals(approuve);
                    })
                    .collect(Collectors.toList());
        }

        String normalizedType = normalizeType(typeAvis);
        if (normalizedType != null) {
            avisList = avisList.stream()
                    .filter(a -> normalizedType.equals(
                            Optional.ofNullable(a.getTypeAvis()).map(String::toLowerCase).orElse(null)
                    ))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(avisList);
    }

    /** 🔹 Compteur (utile pour les badges) :
     *  /api/avis/count?approuve=false
     *  /api/avis/count?typeAvis=cours
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAvis(
            @RequestParam(value = "approuve", required = false) Boolean approuve,
            @RequestParam(value = "typeAvis", required = false) String typeAvis
    ) {
        List<AvisDTO> avisList = getAllAvis(approuve, typeAvis).getBody();
        long count = (avisList == null) ? 0L : avisList.size();
        return ResponseEntity.ok(count);
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
            // ——— Validation simple ———
            if (contenu == null || contenu.trim().length() < 3) {
                return ResponseEntity.badRequest().body("Le contenu de l'avis est trop court.");
            }
            if (pseudoVisiteur == null || pseudoVisiteur.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Le nom/pseudo est requis.");
            }
            int safeNote = Math.max(1, Math.min(5, note == null ? 5 : note)); // clamp 1..5

            // ——— Upload éventuel de la photo ———
            String nomFichier = null;
            if (photoFile != null && !photoFile.isEmpty()) {
                // Optionnel: contrôle type/poids
                if (photoFile.getContentType() != null && !photoFile.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest().body("Le fichier doit être une image.");
                }
                if (photoFile.getSize() > 3_000_000) { // 3 Mo
                    return ResponseEntity.badRequest().body("Image trop volumineuse (max 3 Mo).");
                }

                String dossier = "uploads/avis/";
                Files.createDirectories(Paths.get(dossier));

                String ext = Optional.ofNullable(photoFile.getOriginalFilename())
                        .filter(fn -> fn.contains("."))
                        .map(fn -> fn.substring(fn.lastIndexOf('.')))
                        .orElse("");
                nomFichier = UUID.randomUUID() + ext;

                Path chemin = Paths.get(dossier).resolve(nomFichier);
                photoFile.transferTo(chemin.toFile());
            }

            // ——— Construction de l'entité ———
            Avis avis = new Avis();
            avis.setContenu(contenu.trim());
            avis.setNote(safeNote);
            avis.setPseudoVisiteur(pseudoVisiteur.trim());

            // ✅ type optionnel + normalisé ; jamais "cours" par défaut
            avis.setTypeAvis(normalizeType(typeAvis));

            avis.setDatePub(LocalDate.now());
            avis.setApprouve(false);         // en attente de validation par défaut
            avis.setPhoto(nomFichier);       // peut être null

            avisService.ajouterAvis(avis);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de l'envoi de l'avis : " + e.getMessage());
        }
    }

    // 🔹 Mettre à jour un avis existant
    @PutMapping("/{id}")
    public ResponseEntity<AvisDTO> updateAvis(@PathVariable Integer id, @RequestBody AvisDTO avisDTO) {
        try {
            // Normalise type si présent
            if (avisDTO.getTypeAvis() != null) {
                avisDTO.setTypeAvis(normalizeType(avisDTO.getTypeAvis()));
            }
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