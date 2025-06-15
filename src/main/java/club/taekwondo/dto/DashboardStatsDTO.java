package club.taekwondo.dto;

import java.util.List;

public record DashboardStatsDTO(
    double totalPayes, 
    double totalAttente, 
    double totalAnnules, 
    double pourcentagePayesMois,
    List<DaySumDTO> courbe30J,
    List<MembreRetardDTO> topRetards
) {}