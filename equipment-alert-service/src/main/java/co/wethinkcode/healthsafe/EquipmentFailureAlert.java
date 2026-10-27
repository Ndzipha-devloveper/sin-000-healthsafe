package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipmentFailureAlert(
        String wardId,
        String equipment,
        String description,
        String reportedAt
) {
    public EquipmentFailureAlert {
        if (wardId == null || wardId.isBlank()) {
            throw new IllegalArgumentException("wardId is required");
        }

        if (equipment == null || equipment.isBlank()) {
            throw new IllegalArgumentException("equipment is required");
        }
    }
}