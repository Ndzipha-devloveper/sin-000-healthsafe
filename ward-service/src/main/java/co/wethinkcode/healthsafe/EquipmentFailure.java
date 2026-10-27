package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

/** Report describing equipment that has failed in a hospital ward. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EquipmentFailure(String wardId, String equipment, String description, String reportedAt) {
    public EquipmentFailure withDefaults() {
        return new EquipmentFailure(wardId.trim().toUpperCase(), equipment.trim(),
                description == null ? "" : description.trim(),
                reportedAt == null || reportedAt.isBlank() ? Instant.now().toString() : reportedAt);
    }
}
