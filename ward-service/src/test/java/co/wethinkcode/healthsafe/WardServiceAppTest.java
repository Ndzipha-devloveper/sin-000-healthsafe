package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WardServiceAppTest {
    private IngestionClient ingestionClient;
    private Javalin app;
    private String baseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Ward emergencyWard = new Ward("W-01", "North", "Emergency", 10);
    private final Ward maternityWard = new Ward("W-02", "South", "Maternity", 5);

    @BeforeEach
    void setUp() throws Exception {
        ingestionClient = mock(IngestionClient.class);
        int port = findAvailablePort();
        baseUrl = "http://localhost:" + port;
        app = WardServiceApp.createApp(ingestionClient);
        app.start(port);
    }

    @AfterEach
    void tearDown() {
        if (app != null) {
            app.stop();
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReturnsOkWithoutCallingIngestionService() throws Exception {
        HttpResponse<String> response = get("/health");
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
        verifyNoInteractions(ingestionClient);
    }

    @Test
    void getWardsReturnsAllWards() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of(emergencyWard, maternityWard));

        HttpResponse<String> response = get("/wards");
        List<Ward> wards = objectMapper.readValue(
                response.body(), new TypeReference<List<Ward>>() {});

        assertEquals(200, response.statusCode());
        assertEquals(2, wards.size());
        assertEquals("W-01", wards.get(0).getId());
        assertEquals("W-02", wards.get(1).getId());
        verify(ingestionClient).getWards();
    }

    @Test
    void getWardsReturnsEmptyArrayWhenThereAreNoWards() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of());

        HttpResponse<String> response = get("/wards");

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void getWardsReturns503WhenIngestionServiceFails() throws Exception {
        when(ingestionClient.getWards()).thenThrow(new IOException("connection failed"));

        HttpResponse<String> response = get("/wards");

        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("Ward data is currently unavailable"));
    }

    @Test
    void getWardByIdReturnsMatchingWard() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of(emergencyWard, maternityWard));

        HttpResponse<String> response = get("/wards/W-02");
        Ward ward = objectMapper.readValue(response.body(), Ward.class);

        assertEquals(200, response.statusCode());
        assertEquals("W-02", ward.getId());
        assertEquals("Maternity", ward.getDepartment());
    }

    @Test
    void getWardByIdReturns404WhenWardDoesNotExist() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of(emergencyWard));

        HttpResponse<String> response = get("/wards/W-99");

        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("Ward not found"));
    }

    @Test
    void getWardByIdReturns404WhenWardListIsEmpty() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of());

        HttpResponse<String> response = get("/wards/W-01");

        assertEquals(404, response.statusCode());
    }

    @Test
    void getWardByIdReturns503WhenIngestionServiceFails() throws Exception {
        when(ingestionClient.getWards()).thenThrow(new IOException("connection failed"));

        HttpResponse<String> response = get("/wards/W-01");

        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("Ward data is currently unavailable"));
    }

    @Test
    void departmentsAreSortedAndDuplicatesRemoved() throws Exception {
        Ward emergencyWardAgain = new Ward("W-03", "East", "Emergency", 4);
        Ward surgeryWard = new Ward("W-04", "West", "Surgery", 7);
        when(ingestionClient.getWards()).thenReturn(
                List.of(surgeryWard, emergencyWard, emergencyWardAgain, maternityWard));

        HttpResponse<String> response = get("/departments");
        List<String> departments = objectMapper.readValue(
                response.body(), new TypeReference<List<String>>() {});

        assertEquals(200, response.statusCode());
        assertEquals(List.of("Emergency", "Maternity", "Surgery"), departments);
    }

    @Test
    void departmentsIgnoreNullAndBlankValues() throws Exception {
        Ward nullDepartment = new Ward("W-03", "East", null, 4);
        Ward blankDepartment = new Ward("W-04", "West", "   ", 7);
        when(ingestionClient.getWards()).thenReturn(
                List.of(nullDepartment, blankDepartment, emergencyWard));

        HttpResponse<String> response = get("/departments");
        List<String> departments = objectMapper.readValue(
                response.body(), new TypeReference<List<String>>() {});

        assertEquals(200, response.statusCode());
        assertEquals(List.of("Emergency"), departments);
    }

    @Test
    void departmentsReturnsEmptyArrayWhenThereAreNoWards() throws Exception {
        when(ingestionClient.getWards()).thenReturn(List.of());

        HttpResponse<String> response = get("/departments");

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void departmentsReturns503WhenIngestionServiceFails() throws Exception {
        when(ingestionClient.getWards()).thenThrow(new IOException("connection failed"));

        HttpResponse<String> response = get("/departments");

        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("Department data is currently unavailable"));
    }
}
