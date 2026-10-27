package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentAlertServiceAppTest {
    @TempDir
    Path tempDir;

    private Javalin app;
    private String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private AlertStore store;

    @BeforeEach
    void setUp() {
        store = new AlertStore(tempDir.resolve("alerts.jsonl"));
        app = EquipmentAlertServiceApp.createApp(store);
        app.start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReturnsOk() throws Exception {
        HttpResponse<String> response = get("/health");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void alertsReturnsAnEmptyArrayWhenThereAreNoStoredAlerts() throws Exception {
        HttpResponse<String> response = get("/alerts");

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void alertsReturnsStoredAlertsAsJson() throws Exception {
        EquipmentFailureAlert expected = new EquipmentFailureAlert(
                "W-01", "Ventilator", "Not starting", "2026-10-26T14:15:00Z");
        store.append(expected);

        HttpResponse<String> response = get("/alerts");
        List<EquipmentFailureAlert> alerts = mapper.readValue(
                response.body(), new TypeReference<List<EquipmentFailureAlert>>() {});

        assertEquals(200, response.statusCode());
        assertEquals(List.of(expected), alerts);
    }

    @Test
    void alertsReturns500WhenStoredJsonCannotBeRead() throws Exception {
        java.nio.file.Files.writeString(tempDir.resolve("alerts.jsonl"), "not-json\n");

        HttpResponse<String> response = get("/alerts");

        assertEquals(500, response.statusCode());
        assertTrue(response.body().contains("Could not read persisted alerts"));
    }
}
