package club.taekwondo.service.jpa;

import club.taekwondo.dto.ReinitialisationMotDePasseDTO;
import club.taekwondo.entity.jpa.ReinitialisationMotDePasse;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.repository.jpa.ReinitialisationMotDePasseRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReinitialisationMotDePasseService {

    private static final Logger log = LoggerFactory.getLogger(ReinitialisationMotDePasseService.class);

    private final ReinitialisationMotDePasseRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ReinitialisationMotDePasseService(ReinitialisationMotDePasseRepository repository,
                                             UtilisateurRepository utilisateurRepository,
                                             PasswordEncoder passwordEncoder,
                                             EmailService emailService) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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
     * 🔹 Crée une demande à partir d'un email (appelé depuis le controller)
     */
    public void demanderReinitialisation(String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'email : " + email));

        ReinitialisationMotDePasse demande = new ReinitialisationMotDePasse();
        demande.setUtilisateur(utilisateur);
        demande.setToken(UUID.randomUUID().toString());
        demande.setDateExpiration(LocalDateTime.now().plusHours(1));
        demande.setUtilise(false);

        repository.save(demande);
        
        // 🔔 Envoi de l'email avec le lien de réinitialisation
        try {
            emailService.envoyerEmailReinitialisationMotDePasse(utilisateur.getClub(), email, demande.getToken());
            log.info("Email de réinitialisation envoyé à : {}", email);
        } catch (Exception e) {
            log.error("Erreur envoi email pour {} : {}", email, e.getMessage());
            // On ne fait pas échouer la demande si l'email ne peut pas être envoyé
            // L'admin peut toujours voir le token dans les logs ou BDD
        }
    }

    /**
     * 🔹 Récupère une demande par token
     */
    public Optional<ReinitialisationMotDePasseDTO> getByToken(String token) {
        return repository.findByToken(token).map(this::toDTO);
    }

    /**
     * 🔹 Valide un token (marque comme utilisé s'il est valide)
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

    /**
     * 🔹 Réinitialise le mot de passe avec un token valide
     */
    @Transactional
    public boolean reinitialiserMotDePasse(String token, String newPassword) {
        log.debug("[SERVICE] Début réinitialisation");

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token manquant.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Nouveau mot de passe manquant.");
        }

        Optional<ReinitialisationMotDePasse> opt = repository.findByToken(token);
        log.debug("[SERVICE] Token trouvé en BDD: {}", opt.isPresent());

        if (opt.isPresent()) {
            ReinitialisationMotDePasse demande = opt.get();
            log.debug("[SERVICE] Token utilisé: {}, expiré: {}", demande.isUtilise(),
                    demande.getDateExpiration().isBefore(LocalDateTime.now()));

            // Vérifier que le token n'est pas expiré et n'a pas été utilisé
            if (!demande.isUtilise() && demande.getDateExpiration().isAfter(LocalDateTime.now())) {
                log.info("[SERVICE] Token valide, mise à jour mot de passe");
                // Récupérer l'utilisateur et mettre à jour son mot de passe
                Utilisateur utilisateur = demande.getUtilisateur();
                utilisateur.setPassword(passwordEncoder.encode(newPassword));
                // Le mot de passe n'est plus temporaire après une réinitialisation réussie
                utilisateur.setPasswordTemporaire(false);
                utilisateurRepository.save(utilisateur);
                
                // Marquer le token comme utilisé
                demande.setUtilise(true);
                repository.save(demande);
                
                return true;
            }
        }
        return false;
    }
}