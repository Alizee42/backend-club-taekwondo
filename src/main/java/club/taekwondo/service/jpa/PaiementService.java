package club.taekwondo.service.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.*;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.PaiementRepository;
import club.taekwondo.repository.jpa.CommandeRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Charge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaiementService {
    /**
     * Récupérer tous les paiements d'un club
     */
    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsByClubId(Long clubId) {
        return paiementRepository.findByClubIdAny(clubId)
                .stream()
                .map(this::toPaiementDTO)
                .collect(Collectors.toList());
    }

    private static final Logger log = LoggerFactory.getLogger(PaiementService.class);

    private final PaiementRepository paiementRepository;
    private final EcheanceService echeanceService;
    private final UtilisateurService utilisateurService;
    private final MembreService membreService;
    private final CommandeRepository commandeRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaiementMapper paiementMapper;
    private final PaiementStatsService paiementStatsService;
    private final PaiementStripeService paiementStripeService;

    public PaiementService(
            PaiementRepository paiementRepository,
            EcheanceService echeanceService,
            UtilisateurService utilisateurService,
            MembreService membreService,
            CommandeRepository commandeRepository,
            PasswordEncoder passwordEncoder,
            PaiementMapper paiementMapper,
            PaiementStatsService paiementStatsService,
            PaiementStripeService paiementStripeService
    ) {
        this.paiementRepository = paiementRepository;
        this.echeanceService = echeanceService;
        this.utilisateurService = utilisateurService;
        this.membreService = membreService;
        this.commandeRepository = commandeRepository;
        this.passwordEncoder = passwordEncoder;
        this.paiementMapper = paiementMapper;
        this.paiementStatsService = paiementStatsService;
        this.paiementStripeService = paiementStripeService;
    }

    /* ===========================
     *  Utils
     * =========================== */

    private double safeMontant(Double montant) { return montant != null ? montant : 0.0; }

    private String stripAccents(String input) {
        if (input == null) return "";
        String n = Normalizer.normalize(input, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "");
    }

    private String norm(String v) { return stripAccents(v).toUpperCase(Locale.ROOT).trim(); }

    @SuppressWarnings("unused")
    private boolean isModeCarte(String mode) {
        String m = norm(mode);
        return m.equals("CB") || m.equals("CARTE") || m.equals("CARTE BANCAIRE")
                || m.equals("CARTEBANCAIRE") || m.equals("STRIPE");
    }

    private boolean isTypeUnique(String type) { return norm(type).equals("UNIQUE"); }
    private boolean isTypeEchelonne(String type) { return norm(type).equals("ECHELONNE"); }

    public static String normalizeTypeHuman(String typeHuman) {
        if (typeHuman == null) return "UNIQUE";
        String t = Normalizer.normalize(typeHuman, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
        return (t.startsWith("echel")) ? "ECHELONNE" : "UNIQUE";
    }

    public static String normalizeModeHuman(String modeHuman) {
        if (modeHuman == null) return "ESPECES";
        String m = Normalizer.normalize(modeHuman, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim();
        if (m.equals("stripe") || m.equals("cb") || m.equals("carte") || m.equals("carte bancaire")) return "CB";
        if (m.equals("virement")) return "VIREMENT";
        if (m.equals("cheque")) return "CHEQUE";
        return "ESPECES";
    }

    private static String normalizeType(String t) {
        if (t == null) return "UNIQUE";
        t = t.trim().toUpperCase(Locale.ROOT);
        if ("ECHEANCES".equals(t) || "ECHEANCE".equals(t)) return "ECHELONNE";
        return t;
    }

    private static String normalizeMode(String m) {
        if (m == null) return "ESPECES";
        m = m.trim().toUpperCase(Locale.ROOT);
        if ("STRIPE".equals(m)) return "CB";
        if ("CHEQUE".equals(m)) return "CHEQUE";
        if ("VIREMENT".equals(m)) return "VIREMENT";
        if ("CB".equals(m)) return "CB";
        if ("ESPECE".equals(m)) return "ESPECES";
        return "ESPECES";
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /* ===========================
     *  Queries
     * =========================== */

    @Transactional(readOnly = true)
    public List<PaiementDTO> getAllWithEcheances() {
        List<Paiement> paiements = paiementRepository.findAllWithEcheances();
        List<PaiementDTO> dtos = new ArrayList<>();
        for (Paiement paiement : paiements) {
            dtos.add(toPaiementDTO(paiement));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public Optional<Paiement> findById(Long id) { return paiementRepository.findByIdWithDetails(id); }

    @Transactional(readOnly = true)
    public Optional<Paiement> getById(Long id) { return paiementRepository.findByIdWithDetails(id); }

    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findAll().stream()
                .filter(p -> p.getMembre() != null && Objects.equals(p.getMembre().getId(), membreId))
                .collect(Collectors.toList());
    }

    public Optional<Paiement> findPaiementByUtilisateurAndMontantAndStatut(Long utilisateurId, Double montantTotal, String modePaiement, String statut) {
        return paiementRepository.findPaiementByUtilisateurAndMontantAndStatut(utilisateurId, montantTotal, modePaiement, statut);
    }

    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsParMembres(List<Long> membresIds) {
        return paiementRepository.findByMembreIdIn(membresIds)
                .stream()
                .map(this::toPaiementDTO)
                .toList();
    }

    /* ===========================
     *  Commandes
     * =========================== */

    @Transactional
    public Paiement save(Paiement detached) {
        if (detached.getId() == null) {
            // --- Création ---
            if (detached.getMontantTotal() == null || detached.getMontantTotal() <= 0) {
                throw new IllegalArgumentException("Le montant total ne peut pas être nul ou négatif.");
            }
            if (detached.getMembre() == null || detached.getMembre().getId() == null || detached.getMembre().getId() <= 0) {
                throw new IllegalArgumentException("Le paiement doit être lié à un membre valide.");
            }

            final String type = detached.getType() == null ? "" : detached.getType();
            final String mode = normalizeMode(detached.getModePaiement());
            detached.setModePaiement(mode);

            if (isTypeUnique(type) || (!isTypeUnique(type) && !isTypeEchelonne(type))) {
                detached.setMontantPaye(0.0);
                detached.setMontantRestant(safeMontant(detached.getMontantTotal()));
                detached.setStatut("en attente");
                detached.setEcheances(null);
                detached.setEcheancesRestantes(0);
                detached.setEcheancesTotales(0);
            } else if (isTypeEchelonne(type)) {
                detached.setMontantPaye(0.0);
                detached.setMontantRestant(safeMontant(detached.getMontantTotal()));
                detached.setStatut("en attente");
            }

            return paiementRepository.save(detached);
        } else {
            // --- Mise à jour ---
            Paiement managed = paiementRepository.findById(detached.getId())
                    .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + detached.getId()));

            if (detached.getStatut() != null) managed.setStatut(detached.getStatut());
            if (detached.getMontantTotal() != null) managed.setMontantTotal(detached.getMontantTotal());
            if (detached.getMontantPaye() != null) managed.setMontantPaye(detached.getMontantPaye());
            if (detached.getMontantRestant() != null) managed.setMontantRestant(detached.getMontantRestant());
            if (detached.getEcheancesTotales() != null) managed.setEcheancesTotales(detached.getEcheancesTotales());
            if (detached.getEcheancesRestantes() != null) managed.setEcheancesRestantes(detached.getEcheancesRestantes());
            if (detached.getModePaiement() != null) managed.setModePaiement(normalizeMode(detached.getModePaiement()));
            if (detached.getType() != null) managed.setType(normalizeType(detached.getType()));

            syncEcheancesFromDetached(managed, detached);
            recomputeAggregates(managed);

            return paiementRepository.save(managed);
        }
    }

    @Transactional
    public void delete(Long id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable avec l'ID : " + id));

        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            for (Echeance echeance : paiement.getEcheances()) {
                echeanceService.delete(echeance.getId());
            }
        }

        paiementRepository.deleteById(id);
        log.info("Paiement avec ID {} supprimé avec succès.", id);
    }

    public List<Paiement> filterPaiements(String statut, String modePaiement) {
        List<Paiement> paiements = paiementRepository.findAll();
        if (statut != null && !statut.isBlank()) {
            String s = norm(statut);
            paiements = paiements.stream()
                    .filter(p -> p.getStatut() != null && norm(p.getStatut()).equals(s))
                    .toList();
        }
        if (modePaiement != null && !modePaiement.isBlank()) {
            String m = normalizeMode(modePaiement);
            paiements = paiements.stream()
                    .filter(p -> p.getModePaiement() != null && normalizeMode(p.getModePaiement()).equals(m))
                    .toList();
        }
        return paiements;
    }

    /** Délègue au PaiementStatsService — conservé pour compatibilité avec les controllers existants. */
    public DashboardStatsDTO buildDashboardStats(Long clubId) {
        return paiementStatsService.buildDashboardStats(clubId);
    }

    @Transactional
    public Paiement ajouterPaiementManuel(PaiementDTO dto) {
        Paiement paiement = new Paiement();
        String type = (dto.getType() == null || dto.getType().isBlank()) ? "COTISATION" : norm(dto.getType());
        paiement.setType(type);
        paiement.setModePaiement(normalizeMode(dto.getModePaiement()));
        LocalDate date = null;
        try { if (dto.getDatePaiement() != null && !dto.getDatePaiement().isBlank()) { date = LocalDate.parse(dto.getDatePaiement()); } }
        catch (Exception ignored) { }
        if (date == null) date = LocalDate.now();
        paiement.setDatePaiement(date);
        Optional<Utilisateur> utilisateurOpt = Optional.empty();
        String nom = dto.getUtilisateurNom();
        String prenom = dto.getUtilisateurPrenom();
        String email = dto.getUtilisateurEmail() != null ? dto.getUtilisateurEmail().trim() : null;
        if (dto.getUtilisateurId() != null) {
            utilisateurOpt = utilisateurService.getUtilisateurEntityById(dto.getUtilisateurId());
        } else {
            if (email != null && !email.isEmpty()) {
                utilisateurOpt = utilisateurService.findByEmailIgnoreCase(email);
            }
            if (utilisateurOpt.isEmpty() && nom != null && prenom != null) {
                utilisateurOpt = utilisateurService.findByNomPrenom(nom, prenom);
            }
            if (utilisateurOpt.isEmpty() && nom != null && prenom != null) {
                Utilisateur nouveau = new Utilisateur();
                nouveau.setNom(nom);
                nouveau.setPrenom(prenom);
                nouveau.setEmail(email != null && !email.isEmpty() ? email : generateUniquePlaceholderEmail());
                nouveau.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                nouveau.setRole(Role.PARENT);
                utilisateurOpt = Optional.of(utilisateurService.save(nouveau));
            }
        }
        if (utilisateurOpt.isEmpty()) throw new RuntimeException("Utilisateur non trouvé ou informations insuffisantes.");
        paiement.setUtilisateur(utilisateurOpt.get());
        if (dto.getMembreId() == null || dto.getMembreId() <= 0) {
            throw new RuntimeException("ID du membre invalide pour le paiement !");
        }
        Membre membre = membreService.getMembreEntityById(dto.getMembreId())
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        paiement.setMembre(membre);
        if (dto.getEcheances() != null && !dto.getEcheances().isEmpty()) {
            paiement.setType("ECHELONNE");
            List<Echeance> echeances = new ArrayList<>();
            double total = 0.0;
            int restantes = 0;
            double montantPaye = 0.0;
            int numeroAuto = 1;
        
            // !!! Utiliser le DTO externe
            for (EcheanceDTO edto : dto.getEcheances()) {
                if (edto.getMontant() == null || edto.getMontant() <= 0) {
                    throw new RuntimeException("Échéance invalide (montant).");
                }
                if (edto.getDateEcheance() == null) {
                    throw new RuntimeException("Échéance invalide (date manquante).");
                }
        
                Echeance e = new Echeance();
                e.setPaiement(paiement);
                e.setNumero(edto.getNumero() != null ? edto.getNumero() : numeroAuto++);
                // DTO externe -> LocalDate directement
                e.setDateEcheance(edto.getDateEcheance());
                e.setMontant(edto.getMontant());
                e.setStatut(edto.getStatut() != null ? edto.getStatut() : "en attente");
        
                // si ton entité Echeance possède ces champs :
                e.setModePaiement(edto.getModePaiement());
                e.setDatePaiementReel(edto.getDatePaiementReel());
                e.setReference(edto.getReference());
        
                echeances.add(e);
        
                total += edto.getMontant();
                if ("payé".equalsIgnoreCase(e.getStatut())) {
                    montantPaye += edto.getMontant();
                } else if (!"annulé".equalsIgnoreCase(e.getStatut())) {
                    restantes++;
                }
            }
        
            paiement.setEcheances(echeances);
            paiement.setMontantTotal(total);
            paiement.setMontantPaye(montantPaye);
            paiement.setMontantRestant(Math.max(0.0, total - montantPaye));
            paiement.setEcheancesTotales(echeances.size());
            paiement.setEcheancesRestantes(restantes);
            paiement.setStatut(restantes == 0 ? "payé" : "en attente");
        } else {
            paiement.setType("UNIQUE");
            double montant = dto.getMontantTotal() != null && dto.getMontantTotal() > 0 ? dto.getMontantTotal() : 0.0;
            paiement.setMontantTotal(montant);
            paiement.setMontantPaye(0.0);
            paiement.setMontantRestant(montant);
            paiement.setStatut("en attente");
            paiement.setEcheancesTotales(0);
            paiement.setEcheancesRestantes(0);
            paiement.setEcheances(null);
        }
        return paiementRepository.save(paiement);
    }

    /* =========================================================
       ============  Orchestration complète  =================
       ========================================================= */

    @Transactional
    public List<PaiementDTO> ajouterPaiementsCompletFromDto(PaiementRequestDTO req, MultipartFile justificatif) {
        final String type = normalizeType(req.getTypePaiement());
        final String mode = normalizeMode(req.getModePaiement());
        final LocalDate date = LocalDate.parse(req.getDatePaiement());
        final double total = Optional.ofNullable(req.getMontantTotal()).orElse(0.0);
        if (total <= 0.0) throw new IllegalArgumentException("Montant total invalide.");

        Utilisateur payeur = resolveOrCreatePayeur(req);
        List<Membre> membresCibles = resolveMembresCibles(req, payeur);
        if (membresCibles.isEmpty()) {
            Membre adulte = new Membre();
            adulte.setPrenom(Optional.ofNullable(payeur.getPrenom()).orElse("Adulte"));
            adulte.setNom(Optional.ofNullable(payeur.getNom()).orElse("Inconnu"));
            adulte.setParent(payeur);
            adulte = membreService.save(adulte);
            membresCibles.add(adulte);
        }

        List<PaiementDTO> out = new ArrayList<>();
        for (Membre membre : membresCibles) {
            Paiement p = new Paiement();
            p.setUtilisateur(payeur);
            p.setMembre(membre);
            p.setDatePaiement(date);
            p.setModePaiement(mode);
            p.setType(type);

            if ("UNIQUE".equals(type)) {
                p.setMontantTotal(total);
                fillUnique(p);
            } else {
                p.setMontantTotal(total);
                fillEchelonne(p, req, date);
            }

            Paiement saved = paiementRepository.save(p);
            out.add(toPaiementDTO(saved));
        }
        return out;
    }

    private Utilisateur resolveOrCreatePayeur(PaiementRequestDTO req) {
        if (req.getUtilisateurId() != null) {
            return utilisateurService.getUtilisateurEntityById(req.getUtilisateurId())
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable (id=" + req.getUtilisateurId() + ")"));
        }
        final String nom = trimOrNull(req.getUtilisateurNom());
        final String prenom = trimOrNull(req.getUtilisateurPrenom());
        final String email = trimOrNull(req.getUtilisateurEmail());
        if (nom == null || prenom == null) {
            throw new IllegalArgumentException("Nom et prénom du payeur requis (ou utilisateurId).");
        }
        if (email != null && !email.isEmpty()) {
            Optional<Utilisateur> byEmail = utilisateurService.findByEmailIgnoreCase(email);
            if (byEmail.isPresent()) return byEmail.get();
        } else {
            Optional<Utilisateur> byNomPrenom = utilisateurService.findByNomPrenom(nom, prenom);
            if (byNomPrenom.isPresent()) return byNomPrenom.get();
        }
        Utilisateur nouveau = new Utilisateur();
        nouveau.setNom(nom);
        nouveau.setPrenom(prenom);
        String finalEmail = (email != null && !email.isEmpty()) ? email : generateUniquePlaceholderEmail();
        nouveau.setEmail(finalEmail);
        nouveau.setPassword("defaultPassword");
        nouveau.setRole(Role.PARENT);
        return utilisateurService.save(nouveau);
    }

    private String generateUniquePlaceholderEmail() {
        String candidate;
        int guard = 0;
        do {
            candidate = "noemail+" + System.currentTimeMillis() + "-" +
                    UUID.randomUUID().toString().substring(0, 8) + "@carelink.local";
            guard++;
        } while (utilisateurService.existsByEmailIgnoreCase(candidate) && guard < 5);
        return candidate;
    }

    private List<Membre> resolveMembresCibles(PaiementRequestDTO req, Utilisateur payeur) {
        List<Membre> out = new ArrayList<>();
        if (req.getMembreId() != null) {
            out.add(membreService.getMembreEntityById(req.getMembreId())
                    .orElseThrow(() -> new IllegalArgumentException("Membre introuvable (id=" + req.getMembreId() + ")")));
        }
        if (req.getMembreIds() != null && !req.getMembreIds().isEmpty()) {
            for (Long mid : req.getMembreIds()) {
                if (mid == null) continue;
                out.add(membreService.getMembreEntityById(mid)
                        .orElseThrow(() -> new IllegalArgumentException("Membre introuvable (id=" + mid + ")")));
            }
            out = out.stream().distinct().collect(Collectors.toList());
        }
        if (req.getNewMembre() != null) {
            PaiementRequestDTO.NewMembreInput nm = req.getNewMembre();
            String prenom = trimOrNull(nm.getPrenom());
            String nom = trimOrNull(nm.getNom());
            if (prenom == null || nom == null) {
                throw new IllegalArgumentException("newMembre.prenom et newMembre.nom sont requis.");
            }
            Membre enfant = new Membre();
            enfant.setPrenom(prenom);
            enfant.setNom(nom);
            enfant.setParent(payeur);
            // Si le parent a un club, on hérite pour l'enfant créé à la volée (cohérence parent/enfant)
            if (payeur.getClub() != null) {
                enfant.setClub(payeur.getClub());
            }
            enfant = membreService.save(enfant);
            out.add(enfant);
        }
        return out;
    }

    private void fillUnique(Paiement p) {
        p.setMontantPaye(0.0);
        p.setMontantRestant(p.getMontantTotal());
        p.setStatut("en attente");
        p.setEcheances(Collections.emptyList());
        p.setEcheancesTotales(0);
        p.setEcheancesRestantes(0);
    }

    private void fillEchelonne(Paiement p, PaiementRequestDTO req, LocalDate startDate) {
        List<PaiementRequestDTO.EcheanceInput> in = req.getEcheances();
        List<Echeance> echs = new ArrayList<>();

        if (in != null && !in.isEmpty()) {
            int numAuto = 1;
            for (PaiementRequestDTO.EcheanceInput e : in) {
                Echeance ee = new Echeance();
                ee.setPaiement(p);
                ee.setNumero(e.getNumero() != null ? e.getNumero() : numAuto++);
                ee.setDateEcheance(LocalDate.parse(e.getDateEcheance()));
                ee.setMontant(Optional.ofNullable(e.getMontant()).orElse(0.0));
                ee.setStatut(Optional.ofNullable(e.getStatut()).orElse("en attente"));
                ee.setModePaiement(null);
                ee.setReference(null);
                ee.setDatePaiementReel(null);
                echs.add(ee);
            }
        } else if (req.getNombreEcheances() != null && req.getNombreEcheances() > 0) {
            echs.addAll(autoSplitEcheances(startDate, p.getMontantTotal(), req.getNombreEcheances(), 30, p));
        } else {
            throw new IllegalArgumentException("Aucune échéance fournie pour un paiement échelonné.");
        }
        p.setEcheances(echs);
        p.setEcheancesTotales(echs.size());

        double paye = echs.stream()
                .filter(x -> "payé".equalsIgnoreCase(x.getStatut()))
                .map(Echeance::getMontant)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        int restantes = (int) echs.stream()
                .filter(x -> !"payé".equalsIgnoreCase(x.getStatut()))
                .count();
        p.setMontantPaye(paye);
        p.setMontantRestant(Math.max(0.0, p.getMontantTotal() - paye));
        p.setEcheancesRestantes(restantes);
        p.setStatut(p.getMontantRestant() <= 0.0 ? "payé" : "en attente");

        log.info("[CREATE:ECH] total={} paye={} restant={} restantes={}",
                p.getMontantTotal(), p.getMontantPaye(), p.getMontantRestant(), p.getEcheancesRestantes());
        echs.stream().sorted(Comparator.comparingInt(Echeance::getNumero)).forEach(ee ->
                log.info("   • ech#{} montant={} statut={} date={} ref={}",
                        ee.getNumero(), ee.getMontant(), ee.getStatut(),
                        ee.getDateEcheance(), ee.getReference()));
    }

    private List<Echeance> autoSplitEcheances(LocalDate start, double total, int n, int stepDays, Paiement p) {
        n = Math.max(1, Math.min(12, n));
        stepDays = Math.max(7, stepDays);
        double base = Math.floor((total / n) * 100.0) / 100.0;
        double somme = 0.0;
        List<Echeance> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate d = start.plusDays((long) i * stepDays);
            double montant = (i == n - 1) ? round2(total - somme) : base;
            somme += montant;
            Echeance e = new Echeance();
            e.setPaiement(p);
            e.setNumero(i + 1);
            e.setDateEcheance(d);
            e.setMontant(montant);
            e.setStatut("en attente");
            out.add(e);
        }
        return out;
    }

    /** Délègue au PaiementMapper — conservé pour compatibilité avec les controllers existants. */
    public PaiementDTO toPaiementDTO(Paiement paiement) {
        return paiementMapper.toDTO(paiement);
    }

    @Transactional
    public PaiementDTO annulerPaiement(Long paiementId, AnnulationRequestDTO request) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        if ("payé".equalsIgnoreCase(paiement.getStatut()) || "annulé".equalsIgnoreCase(paiement.getStatut())) {
            throw new IllegalStateException("Ce paiement ne peut pas être annulé (déjà payé ou déjà annulé).");
        }

        double montantAnnule = 0.0;

        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            for (Echeance e : paiement.getEcheances()) {
                if (!"payé".equalsIgnoreCase(e.getStatut())) {
                    e.setStatut("annulé");
                    montantAnnule += safeMontant(e.getMontant());
                }
            }
        } else {
            montantAnnule = safeMontant(paiement.getMontantRestant());
        }

        paiement.setStatut("annulé");
        paiement.setMotifAnnulation(request.getMotif() != null ? request.getMotif() : "");
        paiement.setDateAnnulation(request.getDateAnnulation() != null ? request.getDateAnnulation() : LocalDateTime.now());
        paiement.setAdminResponsable(request.getAdminResponsable() != null ? request.getAdminResponsable() : "admin inconnu");

        paiement.setMontantRestant(0.0);
        paiement.setEcheancesRestantes(0);
        paiement.setMontantPaye(Math.max(0.0, safeMontant(paiement.getMontantTotal()) - montantAnnule));

        Paiement saved = paiementRepository.save(paiement);
        return toPaiementDTO(saved);
    }

    @Transactional
    public Paiement ajouterPaiementParent(PaiementRequestDTO req, Long parentId) {
        log.info("=== [PaiementService] Début ajout paiement parent ===");
        log.info("[Request reçu] {}", req);
        log.info("[Parent connecté ID] {}", parentId);

        if (req.getMembreId() == null || req.getMembreId() <= 0) {
            throw new RuntimeException("ID du membre invalide pour le paiement !");
        }
        Membre membre = membreService.getMembreEntityById(req.getMembreId())
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));
        if (membre.getParent() == null || !Objects.equals(membre.getParent().getId(), parentId)) {
            throw new RuntimeException("Ce membre n'appartient pas au parent connecté !");
        }

    // 🔒 Enforcer: parent et enfant doivent appartenir au même club
    if (membre.getParent() == null || membre.getParent().getClub() == null || membre.getClub() == null) {
        log.warn("[PaiementParent] Club manquant (parentClub={}, enfantClub={}) pour parentId={}, membreId={}",
            membre.getParent() != null ? (membre.getParent().getClub() != null ? membre.getParent().getClub().getId() : null) : null,
            membre.getClub() != null ? membre.getClub().getId() : null,
            parentId, membre.getId());
        throw new IllegalStateException("Impossible d'initier le paiement: club parent ou enfant non renseigné.");
    }
    if (!Objects.equals(membre.getParent().getClub().getId(), membre.getClub().getId())) {
        Long clubParentId = membre.getParent().getClub().getId();
        Long clubEnfantId = membre.getClub().getId();
        log.warn("[PaiementParent] Blocage club mismatch: parentClubId={} vs enfantClubId={} (parentId={}, membreId={})",
            clubParentId, clubEnfantId, parentId, membre.getId());
        throw new IllegalStateException("Cet enfant appartient au club " + clubEnfantId + 
            ", différent de votre club (" + clubParentId + "). Veuillez effectuer le paiement depuis le club de l'enfant.");
    }

        double montant = req.getMontantTotal() != null ? req.getMontantTotal() : 0.0;
        if (montant <= 0.0) {
            throw new IllegalArgumentException("Montant total invalide.");
        }

        final String type = normalizeType(Optional.ofNullable(req.getTypePaiement()).orElse("COTISATION"));
        final String mode = normalizeMode(req.getModePaiement());

        Paiement paiement = new Paiement();
        paiement.setType(type);
        paiement.setModePaiement(mode);
        paiement.setDatePaiement(LocalDate.now());
        paiement.setUtilisateur(membre.getParent());
        paiement.setMembre(membre);
        // 📌 Rattache le club (si connu) depuis le membre ou le parent
        if (membre.getClub() != null) {
            paiement.setClub(membre.getClub());
        } else if (membre.getParent() != null && membre.getParent().getClub() != null) {
            paiement.setClub(membre.getParent().getClub());
        }
        paiement.setMontantTotal(montant);

        paiement.setMontantPaye(0.0);
        paiement.setMontantRestant(montant);
        paiement.setStatut("en attente");

        if (isTypeEchelonne(type)) {
            List<Echeance> echs = new ArrayList<>();

            if (req.getEcheances() != null && !req.getEcheances().isEmpty()) {
                int autoNum = 1;
                for (PaiementRequestDTO.EcheanceInput ein : req.getEcheances()) {
                    if (ein.getMontant() == null || ein.getMontant() <= 0) {
                        throw new RuntimeException("Échéance invalide (montant).");
                    }
                    if (ein.getDateEcheance() == null || ein.getDateEcheance().isBlank()) {
                        throw new RuntimeException("Échéance invalide (date manquante).");
                    }
                    Echeance e = new Echeance();
                    e.setPaiement(paiement);
                    e.setNumero(ein.getNumero() != null ? ein.getNumero() : autoNum++);
                    e.setDateEcheance(LocalDate.parse(ein.getDateEcheance()));
                    e.setMontant(ein.getMontant());

                    e.setStatut(Optional.ofNullable(ein.getStatut()).orElse("en attente"));
                    e.setModePaiement(null);
                    e.setReference(null);
                    e.setDatePaiementReel(null);

                    echs.add(e);
                }
            } else {
                int n = (req.getNombreEcheances() != null && req.getNombreEcheances() > 0)
                        ? Math.min(12, Math.max(1, req.getNombreEcheances()))
                        : 2;

                LocalDate start = LocalDate.now();
                double base = Math.floor((montant / n) * 100.0) / 100.0;
                double somme = 0.0;

                for (int i = 0; i < n; i++) {
                    double part = (i == n - 1) ? round2(montant - somme) : base;
                    somme += part;

                    Echeance e = new Echeance();
                    e.setPaiement(paiement);
                    e.setNumero(i + 1);
                    e.setDateEcheance(start.plusMonths(i));
                    e.setMontant(part);
                    e.setStatut("en attente");
                    e.setModePaiement(null);
                    e.setReference(null);
                    e.setDatePaiementReel(null);

                    echs.add(e);
                }
            }

            paiement.setEcheances(echs);
            paiement.setEcheancesTotales(echs.size());

            long nbRest = echs.stream().filter(e -> !"payé".equalsIgnoreCase(e.getStatut())).count();
            double montantPaye = echs.stream().filter(e -> "payé".equalsIgnoreCase(e.getStatut()))
                    .map(Echeance::getMontant).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
            double restant = echs.stream().filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                    .map(Echeance::getMontant).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

            paiement.setEcheancesRestantes((int) nbRest);
            paiement.setMontantPaye(montantPaye);
            paiement.setMontantRestant(restant);
        } else {
            paiement.setEcheances(Collections.emptyList());
            paiement.setEcheancesTotales(0);
            paiement.setEcheancesRestantes(0);
        }

        Paiement saved = paiementRepository.save(paiement);
        log.info("[Paiement enregistré] ID={} | MembreID={} | UtilisateurID={} | Total={} | Payé={} | Restant={} | Statut={}",
                saved.getId(), saved.getMembre() != null ? saved.getMembre().getId() : null,
                saved.getUtilisateur() != null ? saved.getUtilisateur().getId() : null,
                saved.getMontantTotal(), saved.getMontantPaye(),
                saved.getMontantRestant(), saved.getStatut());
        if (saved.getEcheances() != null) {
            saved.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> log.info("   • ech#{} id={} montant={} statut={} dateEch={} ref={}",
                            e.getNumero(), e.getId(), e.getMontant(), e.getStatut(), e.getDateEcheance(), e.getReference()));
        }
        log.info("=== [PaiementService] Fin ajout paiement parent ===");
        return saved;
    }

    /**
     * Création de paiement initiée par un MEMBRE connecté (adulte).
     * Vérifie que le membre ciblé appartient bien au compte utilisateur courant (compteUtilisateur.id == utilisateurId)
     * puis construit un paiement UNIQUE ou ECHELONNE identique au flux parent.
     */
    @Transactional
    public Paiement ajouterPaiementMembre(PaiementRequestDTO req, Long utilisateurId) {
        log.info("=== [PaiementService] Début ajout paiement membre ===");
        log.info("[Request reçu] {}", req);
        log.info("[Utilisateur connecté ID] {}", utilisateurId);

        if (req.getMembreId() == null || req.getMembreId() <= 0) {
            throw new RuntimeException("ID du membre invalide pour le paiement !");
        }
        Membre membre = membreService.getMembreEntityById(req.getMembreId())
                .orElseThrow(() -> new RuntimeException("Membre non trouvé"));

        // Autorisation: le membre doit être rattaché au compte utilisateur courant
        if (membre.getCompteUtilisateur() == null
                || membre.getCompteUtilisateur().getId() == null
                || !Objects.equals(membre.getCompteUtilisateur().getId(), utilisateurId)) {
            throw new RuntimeException("Ce membre n'appartient pas au compte connecté !");
        }

        double montant = req.getMontantTotal() != null ? req.getMontantTotal() : 0.0;
        if (montant <= 0.0) {
            throw new IllegalArgumentException("Montant total invalide.");
        }

        final String type = normalizeType(Optional.ofNullable(req.getTypePaiement()).orElse("COTISATION"));
        final String mode = normalizeMode(req.getModePaiement());

        Paiement paiement = new Paiement();
        paiement.setType(type);
        paiement.setModePaiement(mode);
        paiement.setDatePaiement(LocalDate.now());
        paiement.setUtilisateur(membre.getCompteUtilisateur());
        paiement.setMembre(membre);
        // 📌 Rattache le club (si connu) depuis le membre ou l'utilisateur
        if (membre.getClub() != null) {
            paiement.setClub(membre.getClub());
        } else if (membre.getCompteUtilisateur() != null && membre.getCompteUtilisateur().getClub() != null) {
            paiement.setClub(membre.getCompteUtilisateur().getClub());
        }
        paiement.setMontantTotal(montant);

        paiement.setMontantPaye(0.0);
        paiement.setMontantRestant(montant);
        paiement.setStatut("en attente");

        if (isTypeEchelonne(type)) {
            List<Echeance> echs = new ArrayList<>();

            if (req.getEcheances() != null && !req.getEcheances().isEmpty()) {
                int autoNum = 1;
                for (PaiementRequestDTO.EcheanceInput ein : req.getEcheances()) {
                    if (ein.getMontant() == null || ein.getMontant() <= 0) {
                        throw new RuntimeException("Échéance invalide (montant).");
                    }
                    if (ein.getDateEcheance() == null || ein.getDateEcheance().isBlank()) {
                        throw new RuntimeException("Échéance invalide (date manquante).");
                    }
                    Echeance e = new Echeance();
                    e.setPaiement(paiement);
                    e.setNumero(ein.getNumero() != null ? ein.getNumero() : autoNum++);
                    e.setDateEcheance(LocalDate.parse(ein.getDateEcheance()));
                    e.setMontant(ein.getMontant());

                    e.setStatut(Optional.ofNullable(ein.getStatut()).orElse("en attente"));
                    e.setModePaiement(null);
                    e.setReference(null);
                    e.setDatePaiementReel(null);

                    echs.add(e);
                }
            } else {
                int n = (req.getNombreEcheances() != null && req.getNombreEcheances() > 0)
                        ? Math.min(12, Math.max(1, req.getNombreEcheances()))
                        : 2;

                LocalDate start = LocalDate.now();
                double base = Math.floor((montant / n) * 100.0) / 100.0;
                double somme = 0.0;

                for (int i = 0; i < n; i++) {
                    double part = (i == n - 1) ? round2(montant - somme) : base;
                    somme += part;

                    Echeance e = new Echeance();
                    e.setPaiement(paiement);
                    e.setNumero(i + 1);
                    e.setDateEcheance(start.plusMonths(i));
                    e.setMontant(part);
                    e.setStatut("en attente");
                    e.setModePaiement(null);
                    e.setReference(null);
                    e.setDatePaiementReel(null);

                    echs.add(e);
                }
            }

            paiement.setEcheances(echs);
            paiement.setEcheancesTotales(echs.size());

            long nbRest = echs.stream().filter(e -> !"payé".equalsIgnoreCase(e.getStatut())).count();
            double montantPaye = echs.stream().filter(e -> "payé".equalsIgnoreCase(e.getStatut()))
                    .map(Echeance::getMontant).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
            double restant = echs.stream().filter(e -> !"payé".equalsIgnoreCase(e.getStatut()))
                    .map(Echeance::getMontant).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();

            paiement.setEcheancesRestantes((int) nbRest);
            paiement.setMontantPaye(montantPaye);
            paiement.setMontantRestant(restant);
        } else {
            paiement.setEcheances(Collections.emptyList());
            paiement.setEcheancesTotales(0);
            paiement.setEcheancesRestantes(0);
        }

        Paiement saved = paiementRepository.save(paiement);
        log.info("[Paiement enregistré (membre)] ID={} | MembreID={} | UtilisateurID={} | Total={} | Payé={} | Restant={} | Statut={}",
                saved.getId(), saved.getMembre() != null ? saved.getMembre().getId() : null,
                saved.getUtilisateur() != null ? saved.getUtilisateur().getId() : null,
                saved.getMontantTotal(), saved.getMontantPaye(),
                saved.getMontantRestant(), saved.getStatut());
        if (saved.getEcheances() != null) {
            saved.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> log.info("   • ech#{} id={} montant={} statut={} dateEch={} ref={}",
                            e.getNumero(), e.getId(), e.getMontant(), e.getStatut(), e.getDateEcheance(), e.getReference()));
        }
        log.info("=== [PaiementService] Fin ajout paiement membre ===");
        return saved;
    }

    @Transactional
    public Paiement validerPaiementAdmin(Long id) {
        Paiement p = paiementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + id));

        log.debug("[PAY] Validation admin id={} type={} mode={} total={}",
                p.getId(), p.getType(), p.getModePaiement(), p.getMontantTotal());

        if (p.getEcheances() != null && !p.getEcheances().isEmpty()) {
            for (Echeance e : p.getEcheances()) {
                e.setStatut("payé");
            }
        }
        p.setEcheancesRestantes(0);
        double total = p.getMontantTotal() != null ? p.getMontantTotal() : 0.0;
        p.setMontantPaye(total);
        p.setMontantRestant(0.0);
        p.setStatut("payé");

        Paiement saved = paiementRepository.save(p);

        // ✅ si un paiement (ex. au club) est validé manuellement, on marque la commande payée aussi
        if (saved.getCommande() != null) {
            marquerCommandePayee(saved.getCommande(), saved.getModePaiement());
        }

        log.info("✅ [PAY] Paiement validé id={} statut={} payé={} restant={}",
                saved.getId(), saved.getStatut(), saved.getMontantPaye(), saved.getMontantRestant());
        return saved;
    }

    private void recomputeAggregates(Paiement p) {
        double paye = 0.0;
        double restant = 0.0;
        int restantes = 0;

        if (p.getEcheances() != null && !p.getEcheances().isEmpty()) {
            for (Echeance e : p.getEcheances()) {
                double m = safeMontant(e.getMontant());
                if ("payé".equalsIgnoreCase(e.getStatut())) {
                    paye += m;
                } else if (!"annulé".equalsIgnoreCase(e.getStatut())) {
                    restant += m;
                    restantes++;
                }
            }
            p.setEcheancesTotales(p.getEcheances().size());
            p.setEcheancesRestantes(restantes);
            p.setMontantPaye(paye);
            p.setMontantRestant(restant);
            p.setStatut(restant <= 0.0 ? "payé" : "en attente");
        } else {
            double total = safeMontant(p.getMontantTotal());
            if ("payé".equalsIgnoreCase(p.getStatut())) {
                p.setMontantPaye(total);
                p.setMontantRestant(0.0);
            } else {
                p.setMontantPaye(0.0);
                p.setMontantRestant(total);
                if (!"annulé".equalsIgnoreCase(p.getStatut())) {
                    p.setStatut("en attente");
                }
            }
            p.setEcheancesTotales(0);
            p.setEcheancesRestantes(0);
        }
    }

    private void syncEcheancesFromDetached(Paiement managed, Paiement detached) {
        if (managed.getEcheances() == null || managed.getEcheances().isEmpty()) return;
        if (detached.getEcheances() == null || detached.getEcheances().isEmpty()) return;

        Map<Long, Echeance> byIdDetached = detached.getEcheances().stream()
                .filter(e -> e.getId() != null)
                .collect(Collectors.toMap(Echeance::getId, e -> e, (a, b) -> b));

        for (Echeance me : managed.getEcheances()) {
            if (me.getId() == null) continue;
            Echeance de = byIdDetached.get(me.getId());
            if (de != null) {
                if (de.getStatut() != null) me.setStatut(de.getStatut());
                if (de.getMontant() != null) me.setMontant(de.getMontant());
                if (de.getDateEcheance() != null) me.setDateEcheance(de.getDateEcheance());
                if (de.getDatePaiementReel() != null) me.setDatePaiementReel(de.getDatePaiementReel());
                if (de.getNumero() != null) me.setNumero(de.getNumero());
            }
        }
    }

    @Transactional
    public Paiement persisterEtat(Paiement paiement) {
        return paiementRepository.save(paiement);
    }

    /**
     * Maintenance: rattache le club aux paiements existants si manquant, en déduisant depuis commande/membre/utilisateur.
     * Retourne le nombre de paiements mis à jour.
     */
    @Transactional
    public int backfillClubForExistingPaiements() {
        List<Paiement> all = paiementRepository.findAll();
        int updated = 0;
        for (Paiement p : all) {
            if (p.getClub() != null) continue;
            Club club = null;
            if (p.getCommande() != null && p.getCommande().getClub() != null) {
                club = p.getCommande().getClub();
            } else if (p.getMembre() != null && p.getMembre().getClub() != null) {
                club = p.getMembre().getClub();
            } else if (p.getUtilisateur() != null && p.getUtilisateur().getClub() != null) {
                club = p.getUtilisateur().getClub();
            }
            if (club != null) {
                p.setClub(club);
                paiementRepository.save(p);
                updated++;
            }
        }
        return updated;
    }

    /**
     * Maintenance: backfill chargeId et receiptUrl depuis Stripe pour les paiements existants
     * possédant un paymentIntentId, mais sans chargeId/receiptUrl.
     * Retourne le nombre de paiements mis à jour.
     */
    @Transactional
    public int backfillStripeChargeInfoForExistingPaiements() {
        List<Paiement> all = paiementRepository.findAll();
        int updated = 0;
        for (Paiement p : all) {
            try {
                String piId = p.getPaymentIntentId();
                boolean needs = (piId != null && !piId.isBlank()) && (p.getChargeId() == null || p.getChargeId().isBlank() || p.getReceiptUrl() == null || p.getReceiptUrl().isBlank());
                if (!needs) continue;

                PaymentIntent pi = PaymentIntent.retrieve(piId);
                if (pi == null) continue;

                String latestChargeId = pi.getLatestCharge();
                if (latestChargeId != null && !latestChargeId.isBlank()) {
                    p.setChargeId(latestChargeId);
                    try {
                        Charge c = Charge.retrieve(latestChargeId);
                        if (c != null && c.getReceiptUrl() != null && !c.getReceiptUrl().isBlank()) {
                            p.setReceiptUrl(c.getReceiptUrl());
                        }
                    } catch (Exception ignored) { }
                } else {
                    Object latestObj = pi.getLatestChargeObject();
                    if (latestObj instanceof Charge c2) {
                        // fallback si l'objet latestCharge est présent
                        if (p.getChargeId() == null || p.getChargeId().isBlank()) p.setChargeId(c2.getId());
                        if (p.getReceiptUrl() == null || p.getReceiptUrl().isBlank()) p.setReceiptUrl(c2.getReceiptUrl());
                    }
                }

                if (pi.getStatus() != null && (p.getStripeStatus() == null || p.getStripeStatus().isBlank())) {
                    p.setStripeStatus(pi.getStatus());
                }

                paiementRepository.save(p);
                updated++;
            } catch (Exception ex) {
                log.warn("[BACKFILL] Échec backfill charge pour paiement id={} : {}", p.getId(), ex.getMessage());
            }
        }
        log.info("[BACKFILL] charge_id/receipt_url mis à jour pour {} paiements", updated);
        return updated;
    }

    private void marquerCommandePayee(Commande cmd, String mode) {
        if (cmd == null) return;
        cmd.setStatut("PAYEE");
        cmd.setModePaiement(normalizeMode(mode != null ? mode : "CB"));
        cmd.setDatePaiement(LocalDate.now());
        commandeRepository.save(cmd);
    }

    /* =========================================================
       =========  Méthodes Stripe / Échéances (BDD)  ==========
       ========================================================= */

    /** Délègue au PaiementStripeService. */
    public void saveEcheanceReference(Long echeanceId, String paymentIntentId) {
        paiementStripeService.saveEcheanceReference(echeanceId, paymentIntentId);
    }

    /** Délègue au PaiementStripeService. */
    public void marquerEcheancePayeeParStripe(Long paiementId, Long echeanceId,
                                              String paymentIntentId, Long amountCents) {
        paiementStripeService.marquerEcheancePayeeParStripe(paiementId, echeanceId, paymentIntentId, amountCents);
    }

    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsParMembre(Long membreId) {
        if (membreId == null) return Collections.emptyList();

        return paiementRepository.findAll().stream()
                .filter(p -> p.getMembre() != null && Objects.equals(p.getMembre().getId(), membreId))
                .map(this::toPaiementDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaiementDTO> getPaiementsParUtilisateur(Long utilisateurId) {
        if (utilisateurId == null) return Collections.emptyList();
        return paiementRepository.findByUtilisateurId(utilisateurId).stream()
                .map(this::toPaiementDTO)
                .collect(Collectors.toList());
    }

}
