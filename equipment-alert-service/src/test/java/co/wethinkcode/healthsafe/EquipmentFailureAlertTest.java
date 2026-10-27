package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentFailureAlertTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsAValidAlert() {
        EquipmentFailureAlert alert = new EquipmentFailureAlert(
                "W-01", "Ventilator", "Not starting", "2026-10-26T14:15:00Z");

        assertEquals("W-01", alert.wardId());
        assertEquals("Ventilator", alert.equipment());
        assertEquals("Not starting", alert.description());
        assertEquals("2026-10-26T14:15:00Z", alert.reportedAt());
    }

    @Test
    void rejectsMissingOrBlankWardId() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentFailureAlert(null, "Ventilator", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentFailureAlert("  ", "Ventilator", null, null));
    }

    @Test
    void rejectsMissingOrBlankEquipment() {
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentFailureAlert("W-01", null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new EquipmentFailureAlert("W-01", "  ", null, null));
    }

    @Test
    void optionalFieldsMayBeNull() {
        EquipmentFailureAlert alert = new EquipmentFailureAlert("W-01", "Ventilator", null, null);

        assertNull(alert.description());
        assertNull(alert.reportedAt());
    }

    @Test
    void ignoresUnknownJsonProperties() throws Exception {
        EquipmentFailureAlert alert = mapper.readValue("""
                {
                  "wardId": "W-01",
                  "equipment": "Ventilator",
                  "description": "Failure",
                  "reportedAt": "2026-10-26T14:15:00Z",
                  "futureField": "ignored"
                }
                """, EquipmentFailureAlert.class);

        assertEquals("W-01", alert.wardId());
        assertEquals("Ventilator", alert.equipment());
    }
}
