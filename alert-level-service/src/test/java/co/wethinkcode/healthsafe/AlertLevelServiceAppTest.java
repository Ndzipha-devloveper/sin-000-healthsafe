package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertLevelServiceAppTest {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private AlertLevel alertLevel;
    private Javalin app;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        alertLevel = new AlertLevel();
        app = AlertLevelServiceApp.createApp(alertLevel);
        app.start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(baseUrl + path));
        if ("PUT".equals(method)) {
            builder.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReturnsOk() throws Exception {
        HttpResponse<String> response = request("GET", "/health", null);

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void getReturnsInitialAlertLevelZero() throws Exception {
        HttpResponse<String> response = request("GET", "/alert-level", null);
        JsonNode json = mapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals(0, json.get("level").asInt());
    }

    @Test
    void putUpdatesTheAlertLevel() throws Exception {
        HttpResponse<String> response = request("PUT", "/alert-level", "{\"level\":5}");
        JsonNode json = mapper.readTree(response.body());

        assertEquals(200, response.statusCode());
        assertEquals(5, json.get("level").asInt());
        assertEquals(5, alertLevel.getLevel());
    }

    @Test
    void putRejectsLevelsOutsideTheAllowedRange() throws Exception {
        HttpResponse<String> response = request("PUT", "/alert-level", "{\"level\":9}");

        assertEquals(400, response.statusCode());
        assertEquals(0, alertLevel.getLevel());
    }

    @Test
    void putRejectsMissingNonIntegerAndMalformedLevels() throws Exception {
        for (String body : new String[] {"{}", "{\"level\":\"5\"}", "{\"level\":2.5}", "not-json"}) {
            HttpResponse<String> response = request("PUT", "/alert-level", body);
            assertEquals(400, response.statusCode(), "Expected rejection for body: " + body);
        }
        assertEquals(0, alertLevel.getLevel());
    }
}
