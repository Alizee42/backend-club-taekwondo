package club.taekwondo.dto;

import java.time.LocalDate;
import java.util.Map;

public record CaisseSuiviDTO(
    LocalDate from,
    LocalDate to,
    Map<String, Double> totalParMode,   // ex. {"CB": 450.0, "VIREMENT": 200.0, "ESPECES": 80.0}
    Map<String, Integer> nbParMode,
    double totalGeneral
) {}
