package club.taekwondo.service.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.entity.jpa.ReinitialisationMotDePasse;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.ReinitialisationMotDePasseRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReinitialisationMotDePasseService {

    private final ReinitialisationMotDePasseRepository repository;
    private final UtilisateurRepository utilisateurRepository;

    public ReinitialisationMotDePasseService(ReinitialisationMotDePasseRepository repository,
                                             UtilisateurRepository utilisateurRepository) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * 🔹 Crée une demande de réinitialisation par ID utilisateur
     */
    public ReinitialisationMotDePasseDTO creerDemande(Long utilisateurId) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateurId);
        if (utilisateurOpt.isEmpty()) {
            throw new IllegalArgumentException("Utilisateur introuvable.");
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken(UUID.randomUUID().toString());
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);

        return toDTO(repository.save(demande));
    }

    /**
     * 🔹 Crée une demande à partir d’un email (appelé depuis le controller)
     */
    public void demanderReinitialisation(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l’email : " + email));

        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken(UUID.randomUUID().toString());
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);

        repository.save(demande);
        // 🔔 Ici, on pourrait ajouter l'envoi d'un mail avec le lien contenant le token
    }

    /**
     * 🔹 Récupère une demande par token
     */
    public Optional<ReinitialisationMotDePasseDTO> getByToken(String token) {
        return repository.findByToken(token).map(this::toDTO);
    }

    /**
     * 🔹 Valide un token (marque comme utilisé s’il est valide)
     */
    @Transactional
    public boolean validerToken(String token) {
        Optional<ReinitialisationMotDePasse> opt = repository.findByToken(token);
        if (opt.isPresent()) {
            ReinitialisationMotDePasse demande = opt.get();
            if (!demande.isUtilise() && demande.getDateExpiration().isAfter(LocalDateTime.now())) {
                demande.setUtilise(true);
                return true;
            }
        }
        return false;
    }

    /**
     * 🔹 Conversion vers DTO
     */
    public ReinitialisationMotDePasseDTO toDTO(ReinitialisationMotDePasse entity) {
        ReinitialisationMotDePasseDTO dto = new ReinitialisationMotDePasseDTO();
        dto.setId(entity.getId());
        dto.setToken(entity.getToken());
        dto.setDateExpiration(entity.getDateExpiration());
        dto.setUtilise(entity.isUtilise());
        dto.setUtilisateurId(entity.getUtilisateur().getId());
        return dto;
    }

    /**
     * 🔹 Conversion vers Entity
     */
    public ReinitialisationMotDePasse toEntity(ReinitialisationMotDePasseDTO dto) {
        ReinitialisationMotDePasse entity = new ReinitialisationMotDePasse();
        entity.setId(dto.getId());
        entity.setToken(dto.getToken());
        entity.setDateExpiration(dto.getDateExpiration());
        entity.setUtilise(dto.isUtilise());
        utilisateurRepository.findById(dto.getUtilisateurId()).ifPresent(entity::setUtilisateur);
        return entity;
    }
}
