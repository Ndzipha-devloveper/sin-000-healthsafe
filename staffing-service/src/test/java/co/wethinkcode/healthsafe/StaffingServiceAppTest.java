package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

class StaffingServiceAppTest {

    private WardClient wardClient;
    private AlertLevelClient alertLevelClient;
    private Javalin app;
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        wardClient = mock(WardClient.class);
        alertLevelClient = mock(AlertLevelClient.class);
        app = StaffingServiceApp.createApp(wardClient, alertLevelClient);
        app.start(0);
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void tearDown() {
        app.stop();
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReturnsOk() throws Exception {
        HttpResponse<String> response = get("/health");

        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
    }

    @Test
    void returnsTheScheduleForAKnownWard() throws Exception {
        when(wardClient.getWard("W-01"))
                .thenReturn(new Ward("W-01", "East Wing", "Cardiology", 3));
        when(alertLevelClient.getLevel()).thenReturn(4);

        HttpResponse<String> response = get("/schedule/W-01");

        assertEquals(200, response.statusCode());
        JsonNode json = objectMapper.readTree(response.body());
        assertEquals("W-01", json.get("wardId").asText());
        assertEquals("Cardiology", json.get("department").asText());
        assertEquals(4, json.get("alertLevel").asInt());
        assertEquals("ELEVATED", json.get("alertMode").asText());
        assertEquals(3, json.get("doctorsOnCall").asInt());
    }

    @Test
    void normalisesTheWardIdBeforeAskingWardService() throws Exception {
        when(wardClient.getWard("W-02"))
                .thenReturn(new Ward("W-02", "West Wing", "Paediatrics", 1));
        when(alertLevelClient.getLevel()).thenReturn(0);

        HttpResponse<String> response = get("/schedule/w-02");

        assertEquals(200, response.statusCode());
        verify(wardClient).getWard("W-02");
    }

    @Test
    void returns404AndSkipsAlertLevelWhenWardIsUnknown() throws Exception {
        when(wardClient.getWard("W-99")).thenThrow(new WardNotFoundException("W-99"));

        HttpResponse<String> response = get("/schedule/W-99");

        assertEquals(404, response.statusCode());
        verify(alertLevelClient, never()).getLevel();
    }

    @Test
    void returns503WhenWardServiceIsDown() throws Exception {
        when(wardClient.getWard("W-01"))
                .thenThrow(new DownstreamUnavailableException("ward-service", "could not be reached"));

        HttpResponse<String> response = get("/schedule/W-01");

        assertEquals(503, response.statusCode());
    }

    @Test
    void returns503WhenAlertLevelServiceIsDown() throws Exception {
        when(wardClient.getWard("W-01"))
                .thenReturn(new Ward("W-01", "East Wing", "Cardiology", 3));
        when(alertLevelClient.getLevel())
                .thenThrow(new DownstreamUnavailableException("alert-level-service", "timed out"));

        HttpResponse<String> response = get("/schedule/W-01");

        assertEquals(503, response.statusCode());
    }

    @Test
    void returns400AndCallsNoServiceForAnInvalidWardId() throws Exception {
        HttpResponse<String> response = get("/schedule/bad_id");

        assertEquals(400, response.statusCode());
        verify(wardClient, never()).getWard("BAD_ID");
        verify(alertLevelClient, never()).getLevel();
    }
}