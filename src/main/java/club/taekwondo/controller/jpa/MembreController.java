package club.taekwondo.controller.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.dto.UtilisateurDTO;
import club.taekwondo.security.JwtUtil;
import club.taekwondo.service.jpa.MembreService;
import club.taekwondo.service.jpa.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/membres")
@CrossOrigin(origins = "*")
public class MembreController {

    @Autowired
    private MembreService membreService;

    @Autowired
    private UtilisateurService utilisateurService;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔹 Récupérer tous les membres
    @GetMapping
    public ResponseEntity<List<MembreDTO>> getAllMembres() {
        return ResponseEntity.ok(membreService.getAllMembres());
    }

    // 🔹 Récupérer un membre par ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMembreById(@PathVariable Long id) {
        return membreService.getMembreById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'ID : " + id));
    }

    // 🔹 Créer un nouveau membre
    @PostMapping
    public ResponseEntity<?> createMembre(@RequestBody MembreDTO membreDTO) {
        MembreDTO nouveauMembre = membreService.createMembre(membreDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nouveauMembre);
    }

    // 🔹 Mettre à jour un membre
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMembre(@PathVariable Long id, @RequestBody MembreDTO membreDTO) {
        if (membreService.getMembreById(id).isEmpty()) {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
        MembreDTO membreMisAJour = membreService.updateMembre(id, membreDTO);
        return ResponseEntity.ok(membreMisAJour);
    }

    // 🔹 Supprimer un membre
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMembre(@PathVariable Long id) {
        if (membreService.getMembreById(id).isEmpty()) {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
        membreService.deleteMembre(id);
        return ResponseEntity.ok(Map.of("message", "Membre supprimé avec succès."));
    }

    // 🔹 Récupérer le membre ou utilisateur connecté
    @GetMapping("/me")
    public ResponseEntity<?> getMembreConnecte(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("En-tête Authorization manquant ou invalide.");
        }
        String jwt = token.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(jwt);

        Optional<MembreDTO> membreOpt = membreService.getMembreByEmail(email);
        if (membreOpt.isPresent()) {
            return ResponseEntity.ok(membreOpt.get());
        }

        Optional<UtilisateurDTO> utilisateurOpt = utilisateurService.getUtilisateurByEmail(email);
        return utilisateurOpt.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseThrow(() -> new RuntimeException("Aucun membre ou utilisateur trouvé avec l'email : " + email));
    }
}