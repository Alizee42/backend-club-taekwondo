package club.taekwondo.service.jpa;

import club.taekwondo.dto.MembreDTO;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.entity.jpa.Club;
import club.taekwondo.repository.jpa.MembreRepository;
import club.taekwondo.repository.jpa.UtilisateurRepository;
import club.taekwondo.repository.jpa.ClubRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MembreService {

    private final MembreRepository membreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ClubRepository clubRepository;

    public MembreService(MembreRepository membreRepository, UtilisateurRepository utilisateurRepository, ClubRepository clubRepository) {
        this.membreRepository = membreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.clubRepository = clubRepository;
    }

    // 🔹 Récupérer tous les membres (DTO)
    public List<MembreDTO> getAllMembres() {
        return membreRepository.findAll()
                .stream()
                .map(this::toMembreDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Récupérer un membre par ID (DTO)
    public Optional<MembreDTO> getMembreById(Long id) {
        return membreRepository.findById(id)
                .map(this::toMembreDTO);
    }

    // 🔹 Récupérer un membre par ID (Entity)
    public Optional<Membre> getMembreEntityById(Long id) {
        return membreRepository.findById(id);
    }

    // ✅ Alias simple attendu par d'autres services/contrôleurs
    public Optional<Membre> findById(Long id) {
        return membreRepository.findById(id);
    }

    // 🔹 Récupérer un membre (DTO) par email utilisateur (compte utilisateur adulte)
    public Optional<MembreDTO> getMembreByEmail(String email) {
        return membreRepository.findByCompteUtilisateur_Email(email)
                .map(this::toMembreDTO);
    }

    public Optional<MembreDTO> getMembreByUtilisateurEmail(String email) {
        return getMembreByEmail(email);
    }

    // 🔹 Récupérer tous les membres d'un club (DTO)
    public List<MembreDTO> getMembresByClubId(Long clubId) {
        return membreRepository.findByClub_Id(clubId)
                .stream()
                .map(this::toMembreDTO)
                .collect(Collectors.toList());
    }

    // 🔹 Créer un membre sans rattachement explicite
    public MembreDTO createMembre(MembreDTO membreDTO) {
        return createMembre(membreDTO, membreDTO.getUtilisateurId());
    }

    // 🔹 Créer un membre avec rattachement explicite (adulte ↔ compteUtilisateur / enfant ↔ parent)
    public MembreDTO createMembre(MembreDTO membreDTO, Long utilisateurId) {
        Membre membre = fromMembreDTO(membreDTO);

        if (membreDTO.isEstAdulte()) {
            // Membre adulte → rattachement au compte utilisateur
            if (utilisateurId != null) {
                Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + utilisateurId));
                membre.setCompteUtilisateur(utilisateur);
            } else {
                throw new RuntimeException("Impossible de créer un membre adulte sans utilisateur associé.");
            }
        } else {
            // Membre enfant → rattachement au parent
            if (utilisateurId != null) {
                Utilisateur parent = utilisateurRepository.findById(utilisateurId)
                        .orElseThrow(() -> new RuntimeException("Parent non trouvé avec l'ID : " + utilisateurId));
                membre.setParent(parent);
            } else {
                throw new RuntimeException("Impossible de créer un membre enfant sans parent associé.");
            }
        }

        // 🔹 Rattacher le club si clubId présent dans le DTO
        if (membreDTO.getClubId() != null) {
            Club club = clubRepository.findById(membreDTO.getClubId())
                    .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID : " + membreDTO.getClubId()));
            membre.setClub(club);
        }

        return toMembreDTO(membreRepository.save(membre));
    }

    // 🔹 Enfants d’un parent (Entity)
    public List<Membre> getEnfantsDuParent(Long parentId) {
        return membreRepository.findByParentId(parentId);
    }

    // 🔹 Membre rattaché à un utilisateur (adulte) — Entity
    public Optional<Membre> getMembreEntityByIdUtilisateur(Long utilisateurId) {
        return membreRepository.findByCompteUtilisateur_Id(utilisateurId);
    }

    // ✅ Alias attendu ailleurs (ex. StripeController)
    public Optional<Membre> findByCompteUtilisateurId(Long utilisateurId) {
        return membreRepository.findByCompteUtilisateur_Id(utilisateurId);
    }

    // 🔹 Mettre à jour un membre
    public MembreDTO updateMembre(Long id, MembreDTO membreDTO) {
        return membreRepository.findById(id).map(membre -> {
            membre.setNom(membreDTO.getNom());
            membre.setPrenom(membreDTO.getPrenom());
            membre.setDateNaissance(membreDTO.getDateNaissance());
            membre.setNumeroLicence(membreDTO.getNumeroLicence());
            membre.setCeinture(membreDTO.getCeinture());
            membre.setEstAdulte(membreDTO.isEstAdulte());
            // MAJ du club si besoin
            if (membreDTO.getClubId() != null) {
                Club club = clubRepository.findById(membreDTO.getClubId())
                        .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID : " + membreDTO.getClubId()));
                membre.setClub(club);
            }
            return toMembreDTO(membreRepository.save(membre));
        }).orElseThrow(() -> new RuntimeException("Membre non trouvé avec l'ID : " + id));
    }

    // 🔹 Supprimer un membre
    public void deleteMembre(Long id) {
        if (!membreRepository.existsById(id)) {
            throw new RuntimeException("Membre non trouvé avec l'ID : " + id);
        }
        membreRepository.deleteById(id);
    }

    // 🔹 Récupérer les membres liés à un utilisateur (parent) — DTO
    public List<MembreDTO> getMembresByUtilisateurId(Long utilisateurId) {
        List<Membre> membres = membreRepository.findByParentId(utilisateurId);
        return membres.stream()
                .map(this::toMembreDTO)
                .collect(Collectors.toList());
    }

    // ✅ NOUVEAU : récupérer les enfants du parent via l'email (extrait du JWT)
    public List<MembreDTO> getMembresByParentEmail(String email) {
        Optional<Utilisateur> parentOpt = utilisateurRepository.findByEmailIgnoreCase(email);
        if (parentOpt.isEmpty()) {
            return List.of();
        }
        Long parentId = parentOpt.get().getId();
        return getMembresByUtilisateurId(parentId);
    }

    // ✅ Utilitaire simple : utilisé par d'autres services
    public Membre save(Membre membre) {
        return membreRepository.save(membre);
    }

    // 🔁 Membre → DTO
    public MembreDTO toMembreDTO(Membre membre) {
        MembreDTO dto = new MembreDTO();
        dto.setId(membre.getId());
        dto.setNom(membre.getNom());
        dto.setPrenom(membre.getPrenom());
        dto.setDateNaissance(membre.getDateNaissance());
        dto.setNumeroLicence(membre.getNumeroLicence());
        dto.setCeinture(membre.getCeinture());
        dto.setEstAdulte(membre.isEstAdulte());

        if (membre.getCompteUtilisateur() != null) {
            dto.setUtilisateurId(membre.getCompteUtilisateur().getId());
        } else if (membre.getParent() != null) {
            dto.setUtilisateurId(membre.getParent().getId());
        }
        if (membre.getClub() != null) {
            dto.setClubId(membre.getClub().getId());
        }
        return dto;
    }

    // 🔁 DTO → Membre
    public Membre fromMembreDTO(MembreDTO dto) {
        Membre membre = new Membre();
        membre.setNom(dto.getNom());
        membre.setPrenom(dto.getPrenom());
        membre.setDateNaissance(dto.getDateNaissance());
        membre.setNumeroLicence(dto.getNumeroLicence());
        membre.setCeinture(dto.getCeinture());
        membre.setEstAdulte(dto.isEstAdulte());
        // Ajout du club si présent dans le DTO
        if (dto.getClubId() != null) {
            Club club = clubRepository.findById(dto.getClubId())
                    .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID : " + dto.getClubId()));
            membre.setClub(club);
        }
        return membre;
    }
}