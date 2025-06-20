package club.taekwondo.dto;

import java.util.List;


public record DashboardStatsDTO(
    double totalPayes,                   // Somme totale des paiements avec statut "payé"
    double totalAttente,                 // Somme totale des paiements avec statut "en attente"
    double totalAnnules,                 // Somme totale des paiements avec statut "annulé"
    double pourcentagePayesMois,         // Pourcentage des paiements payés ce mois-ci
    List<DaySumDTO> courbe30J,           // Liste des paiements journaliers sur les 30 derniers jours
    List<MembreRetardDTO> membresEnRetard
) {}
