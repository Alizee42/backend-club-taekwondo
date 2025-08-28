package club.taekwondo.service.jpa;

import club.taekwondo.dto.*;
import club.taekwondo.entity.jpa.Echeance;
import club.taekwondo.entity.jpa.Membre;
import club.taekwondo.entity.jpa.Paiement;
import club.taekwondo.entity.jpa.Utilisateur;
import club.taekwondo.enums.Role;
import club.taekwondo.repository.jpa.PaiementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaiementService {

    private static final Logger log = LoggerFactory.getLogger(PaiementService.class);

    private final PaiementRepository paiementRepository;
    private final EcheanceService echeanceService;
    private final UtilisateurService utilisateurService;
    private final MembreService membreService;

    public PaiementService(
            PaiementRepository paiementRepository,
            EcheanceService echeanceService,
            UtilisateurService utilisateurService,
            MembreService membreService
    ) {
        this.paiementRepository = paiementRepository;
        this.echeanceService = echeanceService;
        this.utilisateurService = utilisateurService;
        this.membreService = membreService;
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

    public List<PaiementDTO> getAllWithEcheances() {
        List<Paiement> paiements = paiementRepository.findAllWithEcheances();
        List<PaiementDTO> dtos = new ArrayList<>();
        for (Paiement paiement : paiements) {
            dtos.add(toPaiementDTO(paiement));
        }
        return dtos;
    }

    /** Ajouté pour le contrôleur Stripe */
    public Optional<Paiement> findById(Long id) { return paiementRepository.findById(id); }

    public Optional<Paiement> getById(Long id) { return paiementRepository.findById(id); }

    public List<Paiement> getByMembreId(Long membreId) {
        return paiementRepository.findAll().stream()
                .filter(p -> p.getMembre() != null && Objects.equals(p.getMembre().getId(), membreId))
                .collect(Collectors.toList());
    }

    public Optional<Paiement> findPaiementByUtilisateurAndMontantAndStatut(Long utilisateurId, Double montantTotal, String modePaiement, String statut) {
        return paiementRepository.findPaiementByUtilisateurAndMontantAndStatut(utilisateurId, montantTotal, modePaiement, statut);
    }

    public List<PaiementDTO> getPaiementsParMembres(List<Long> membresIds) {
        return paiementRepository.findByMembreIdIn(membresIds)
                .stream()
                .map(this::toPaiementDTO)
                .toList();
    }

    /* ===========================
     *  Commandes
     * =========================== */

    /**
     * Sauvegarde "safe":
     * - Création (id == null) : initialise le statut selon le type et NE REMPLACE PAS la liste d’échéances sauf création.
     * - Mise à jour (id != null) : ne remplace jamais la collection -> sync par ID + recalc agrégats.
     */
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
                // UNIQUE (ou autre assimilé) → en attente, pas d'échéances
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
                // garde la liste transmise à la création (si présente)
            }

            return paiementRepository.save(detached);
        } else {
            // --- Mise à jour sans remplacer la collection ---
            Paiement managed = paiementRepository.findById(detached.getId())
                    .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + detached.getId()));

            // Champs scalaires (on reste conservateur)
            if (detached.getStatut() != null) managed.setStatut(detached.getStatut());
            if (detached.getMontantTotal() != null) managed.setMontantTotal(detached.getMontantTotal());
            if (detached.getMontantPaye() != null) managed.setMontantPaye(detached.getMontantPaye());
            if (detached.getMontantRestant() != null) managed.setMontantRestant(detached.getMontantRestant());
            if (detached.getEcheancesTotales() != null) managed.setEcheancesTotales(detached.getEcheancesTotales());
            if (detached.getEcheancesRestantes() != null) managed.setEcheancesRestantes(detached.getEcheancesRestantes());
            if (detached.getModePaiement() != null) managed.setModePaiement(normalizeMode(detached.getModePaiement()));
            if (detached.getType() != null) managed.setType(normalizeType(detached.getType()));

            // Synchroniser uniquement les champs des échéances existantes
            syncEcheancesFromDetached(managed, detached);

            // Recalcule agrégats à partir du managed
            recomputeAggregates(managed);

            Paiement saved = paiementRepository.save(managed);
            return saved;
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

    public DashboardStatsDTO buildDashboardStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate firstDayMonth = today.withDayOfMonth(1);
            LocalDate minus30 = today.minusDays(30);

            List<Paiement> paiements = paiementRepository.findAll();

            double totalPayes = 0.0;
            double totalAttente = 0.0;
            double totalAnnules = 0.0;

            for (Paiement paiement : paiements) {
                double montantPaye = 0.0;
                double montantRestant = 0.0;

                if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
                    for (Echeance e : paiement.getEcheances()) {
                        if ("payé".equalsIgnoreCase(e.getStatut())) {
                            montantPaye += safeMontant(e.getMontant());
                        } else if ("en attente".equalsIgnoreCase(e.getStatut())) {
                            montantRestant += safeMontant(e.getMontant());
                        } else if ("annulé".equalsIgnoreCase(e.getStatut())) {
                            totalAnnules += safeMontant(e.getMontant());
                        }
                    }
                } else {
                    if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                        montantPaye = safeMontant(paiement.getMontantTotal());
                    } else if ("en attente".equalsIgnoreCase(paiement.getStatut())) {
                        montantRestant = safeMontant(paiement.getMontantRestant());
                        montantPaye = safeMontant(paiement.getMontantTotal()) - montantRestant;
                    } else if ("annulé".equalsIgnoreCase(paiement.getStatut())) {
                        totalAnnules += Math.max(0.0, safeMontant(paiement.getMontantTotal()) - safeMontant(paiement.getMontantPaye()));
                        montantPaye = safeMontant(paiement.getMontantPaye());
                    }
                }

                if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                } else if ("en attente".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                    totalAttente += montantRestant;
                } else if ("annulé".equalsIgnoreCase(paiement.getStatut())) {
                    totalPayes += montantPaye;
                }
            }

            Double montantTotalMois = paiementRepository.sumByDatePaiementBetween(firstDayMonth, today);
            Double montantPayesMois = paiementRepository.sumByStatutAndDatePaiementBetween("payé", firstDayMonth, today);

            montantTotalMois = (montantTotalMois != null) ? montantTotalMois : 0.0;
            montantPayesMois = (montantPayesMois != null) ? montantPayesMois : 0.0;

            double pctMois = montantTotalMois == 0 ? 0 : (montantPayesMois / montantTotalMois) * 100;

            List<DaySumDTO> courbe = paiementRepository.sumByDay(minus30, today);
            List<MembreRetardDTO> top = echeanceService.getMembresEnRetard();

            return new DashboardStatsDTO(
                    totalPayes,
                    totalAttente,
                    totalAnnules,
                    pctMois,
                    courbe != null ? courbe : new ArrayList<>(),
                    top != null ? top : new ArrayList<>()
            );
        } catch (Exception e) {
            log.error("Erreur buildDashboardStats", e);
            return new DashboardStatsDTO(0, 0, 0, 0, new ArrayList<>(), new ArrayList<>());
        }
    }

    @Transactional
    public Paiement ajouterPaiementManuel(PaiementDTO dto) {
        // (ton code d’origine conservé)
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
                nouveau.setPassword("defaultPassword");
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
            for (PaiementDTO.EcheanceDTO edto : dto.getEcheances()) {
                if (edto.getMontant() == null || edto.getMontant() <= 0) {
                    throw new RuntimeException("Échéance invalide (montant).");
                }
                if (edto.getDateEcheance() == null || edto.getDateEcheance().isBlank()) {
                    throw new RuntimeException("Échéance invalide (date manquante).");
                }
                Echeance e = new Echeance();
                e.setPaiement(paiement);
                e.setNumero(edto.getNumero() != null ? edto.getNumero() : numeroAuto++);
                e.setDateEcheance(LocalDate.parse(edto.getDateEcheance()));
                e.setMontant(edto.getMontant());
                e.setStatut(edto.getStatut() != null ? edto.getStatut() : "en attente");
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
       ============  NOUVEAU : Orchestration complète  =========
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

    /** ❗ Corrigé : plus de pré-paiement CB de la 1ʳᵉ échéance */
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
                // Toutes en attente (sauf si la requête impose explicitement "payé")
                ee.setStatut(Optional.ofNullable(e.getStatut()).orElse("en attente"));
                ee.setModePaiement(null);
                ee.setReference(null);
                ee.setDatePaiementReel(null);
                echs.add(ee);
            }
        } else if (req.getNombreEcheances() != null && req.getNombreEcheances() > 0) {
            // Auto-split : toutes "en attente"
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

        // Logs diagnostic
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

    public PaiementDTO toPaiementDTO(Paiement paiement) {
        PaiementDTO dto = new PaiementDTO();
        dto.setId(paiement.getId());
        dto.setType(paiement.getType());
        dto.setDatePaiement(paiement.getDatePaiement() != null ? paiement.getDatePaiement().toString() : null);
        dto.setStatut(paiement.getStatut());
        dto.setModePaiement(paiement.getModePaiement());
        dto.setMontantTotal(paiement.getMontantTotal());
        dto.setMotifAnnulation(paiement.getMotifAnnulation());
        dto.setDateAnnulation(paiement.getDateAnnulation());
        dto.setAdminResponsable(paiement.getAdminResponsable());

        if (paiement.getUtilisateur() != null) {
            dto.setUtilisateurId(paiement.getUtilisateur().getId());
            dto.setUtilisateurNom(paiement.getUtilisateur().getNom());
            dto.setUtilisateurPrenom(paiement.getUtilisateur().getPrenom());
            dto.setUtilisateurEmail(paiement.getUtilisateur().getEmail());
        }

        if (paiement.getMembre() != null) {
            dto.setMembreId(paiement.getMembre().getId());
            dto.setMembreNom(paiement.getMembre().getNom());
            dto.setMembrePrenom(paiement.getMembre().getPrenom());
            dto.setEnfantNomComplet(
                    (Optional.ofNullable(paiement.getMembre().getPrenom()).orElse("") + " " +
                            Optional.ofNullable(paiement.getMembre().getNom()).orElse("")).trim()
            );
        }

        double montantPaye = 0.0;

        if (paiement.getEcheances() != null && !paiement.getEcheances().isEmpty()) {
            List<PaiementDTO.EcheanceDTO> liste = new ArrayList<>();
            for (Echeance e : paiement.getEcheances()) {
                PaiementDTO.EcheanceDTO edto = new PaiementDTO.EcheanceDTO();
                edto.setId(e.getId());
                edto.setNumero(e.getNumero());
                edto.setDateEcheance(e.getDateEcheance() != null ? e.getDateEcheance().toString() : null);
                edto.setMontant(e.getMontant());
                edto.setStatut(e.getStatut());

                if ("payé".equalsIgnoreCase(e.getStatut())) {
                    montantPaye += safeMontant(e.getMontant());
                }

                liste.add(edto);
            }
            dto.setEcheances(liste);
        } else {
            if ("payé".equalsIgnoreCase(paiement.getStatut())) {
                montantPaye = safeMontant(paiement.getMontantTotal());
            }
        }

        double total = safeMontant(paiement.getMontantTotal());
        dto.setMontantPaye(montantPaye);
        dto.setMontantRestant(Math.max(0.0, total - montantPaye));

        return dto;
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

    /** ❗ Corrigé : plus de pré-paiement CB de la 1ʳᵉ échéance */
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

        double montant = req.getMontantTotal() != null ? req.getMontantTotal() : 0.0;
        if (montant <= 0.0) {
            throw new IllegalArgumentException("Montant total invalide.");
        }

        final String type = normalizeType(Optional.ofNullable(req.getTypePaiement()).orElse("COTISATION"));
        final String mode = normalizeMode(req.getModePaiement());

        Paiement paiement = new Paiement();
        paiement.setType(type);
        paiement.setModePaiement(mode);
        paiement.setDatePaiement(LocalDate.now()); // date de création (pas une preuve de paiement)
        paiement.setUtilisateur(membre.getParent());
        paiement.setMembre(membre);
        paiement.setMontantTotal(montant);

        // À la création : rien n'est payé
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

                    // Toutes en attente à la création
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
                    e.setStatut("en attente");   // ✅ TOUTES en attente à la création
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
                saved.getId(),
                saved.getMembre() != null ? saved.getMembre().getId() : null,
                saved.getUtilisateur() != null ? saved.getUtilisateur().getId() : null,
                saved.getMontantTotal(),
                saved.getMontantPaye(),
                saved.getMontantRestant(),
                saved.getStatut());
        if (saved.getEcheances() != null) {
            saved.getEcheances().stream()
                    .sorted(Comparator.comparingInt(Echeance::getNumero))
                    .forEach(e -> log.info("   • ech#{} id={} montant={} statut={} dateEch={} ref={}",
                            e.getNumero(), e.getId(), e.getMontant(), e.getStatut(), e.getDateEcheance(), e.getReference()));
        }
        log.info("=== [PaiementService] Fin ajout paiement parent ===");
        return saved;
    }

    /* ===========================
     *  ✅ Validation admin (safe)
     * =========================== */

    @Transactional
    public Paiement validerPaiementAdmin(Long id) {
        Paiement p = paiementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + id));

        log.debug("[PAY] Validation admin id={} type={} mode={} total={}",
                p.getId(), p.getType(), p.getModePaiement(), p.getMontantTotal());

        // Ne JAMAIS remplacer la collection
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
        log.info("✅ [PAY] Paiement validé id={} statut={} payé={} restant={}",
                saved.getId(), saved.getStatut(), saved.getMontantPaye(), saved.getMontantRestant());
        return saved;
    }

    /* ===========================
     *  Helpers "safe update"
     * =========================== */

    /** Recalcule les agrégats depuis l'état courant du paiement (sans créer/remplacer de collection). */
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

    /**
     * Synchronise les champs des échéances sans remplacer la collection :
     * met à jour statut/date/montant sur les IDs existants uniquement.
     */
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

    /* =========================================================
       =========  🆕 Méthodes Stripe / Échéances (BDD)  =========
       ========================================================= */

    /**
     * Enregistre l'identifiant Stripe PaymentIntent sur l'échéance donnée (champ reference),
     * afin d'éviter toute collision entre échéances.
     */
    @Transactional
    public void saveEcheanceReference(Long echeanceId, String paymentIntentId) {
        if (echeanceId == null || paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Paramètres invalides pour saveEcheanceReference");
        }
        Echeance e = echeanceService.getEcheanceEntityById(echeanceId)
                .orElseThrow(() -> new NoSuchElementException("Échéance introuvable id=" + echeanceId));
        e.setReference(paymentIntentId);
        echeanceService.save(e);
        log.info("💾 Référence PaymentIntent {} enregistrée sur échéance {}", paymentIntentId, echeanceId);
    }

    /**
     * Appelée par le webhook Stripe (payment_intent.succeeded).
     * Marque l’échéance comme payée, met à jour les montants agrégés et conserve la référence PI.
     */
    @Transactional
    public void marquerEcheancePayeeParStripe(Long paiementId, Long echeanceId, String paymentIntentId, Long amountCents) {
        if (echeanceId == null) {
            // Paiement unique (sans échéances)
            if (paiementId == null) return;
            Paiement p = paiementRepository.findById(paiementId)
                    .orElseThrow(() -> new NoSuchElementException("Paiement introuvable id=" + paiementId));
            p.setModePaiement("CB");
            p.setStatut("payé");
            p.setMontantPaye(safeMontant(p.getMontantTotal()));
            p.setMontantRestant(0.0);
            p.setDatePaiement(LocalDate.now());
            // optionnel : si le champ existe
            p.setPaymentIntentId(paymentIntentId);
            paiementRepository.save(p);
            log.info("✅ Paiement unique {} marqué payé via Stripe (PI={})", p.getId(), paymentIntentId);
            return;
        }

        // Cas échéance identifiée
        Echeance e = echeanceService.getEcheanceEntityById(echeanceId)
                .orElseThrow(() -> new NoSuchElementException("Échéance introuvable id=" + echeanceId));
        Paiement p = e.getPaiement();
        if (paiementId != null && (p == null || !Objects.equals(p.getId(), paiementId))) {
            log.warn("⚠️ Incohérence webhook: paiementId={} ne matche pas la paiements de l'échéance ({}).",
                    paiementId, (p != null ? p.getId() : null));
        }

        if (amountCents != null && e.getMontant() != null) {
            long expected = Math.round(e.getMontant() * 100.0);
            if (!Objects.equals(expected, amountCents)) {
                log.warn("⚠️ Montant Stripe ({}) différent du montant échéance attendu ({}).", amountCents, expected);
            }
        }

        e.setStatut("payé");
        e.setDatePaiementReel(LocalDate.now());
        e.setReference(paymentIntentId);
        e.setModePaiement("CB");
        echeanceService.save(e);

        if (p != null) {
            p.setModePaiement("CB");
            recomputeAggregates(p);
            paiementRepository.save(p);
            log.info("✅ Échéance {} du paiement {} marquée payée (PI={})", echeanceId, p.getId(), paymentIntentId);
        }
    }
}

