package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A staffing update broadcast by staffing-service on "staffing-events-topic".
 * Fields we do not know about are ignored, so staffing can add more later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffingUpdate(
        String wardId,
        String department,
        int alertLevel,
        String alertMode,
        int doctorsOnCall
) {
}