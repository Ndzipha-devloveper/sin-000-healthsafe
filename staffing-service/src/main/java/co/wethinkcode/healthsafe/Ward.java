package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A ward as returned by ward-service (GET /wards/{id}).
 * Fields we don't need are ignored, so ward-service can add more later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Ward(
        String id,
        String wing,
        String department,
        Integer bedsAvailable
) {
}