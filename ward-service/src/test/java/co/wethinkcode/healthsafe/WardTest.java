package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WardTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void constructorAndGettersExposeWardData() {
        Ward ward = new Ward("W-05", "North", "Emergency", 12);
        assertEquals("W-05", ward.getId());
        assertEquals("North", ward.getWing());
        assertEquals("Emergency", ward.getDepartment());
        assertEquals(12, ward.getBedsAvailable());
    }

    @Test
    void noArgConstructorCanCreateWard() {
        assertNotNull(new Ward());
    }

    @Test
    void wardSerializesToExpectedJson() throws Exception {
        Ward ward = new Ward("W-05", "North", "Emergency", 12);
        String json = objectMapper.writeValueAsString(ward);

        assertTrue(json.contains("\"id\":\"W-05\""));
        assertTrue(json.contains("\"wing\":\"North\""));
        assertTrue(json.contains("\"department\":\"Emergency\""));
        assertTrue(json.contains("\"bedsAvailable\":12"));
    }

    @Test
    void wardDeserializesFromJson() throws Exception {
        String json = """
                {
                  "id": "W-05",
                  "wing": "North",
                  "department": "Emergency",
                  "bedsAvailable": 12
                }
                """;

        Ward ward = objectMapper.readValue(json, Ward.class);
        assertEquals("W-05", ward.getId());
        assertEquals("North", ward.getWing());
        assertEquals("Emergency", ward.getDepartment());
        assertEquals(12, ward.getBedsAvailable());
    }
}
