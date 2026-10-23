package co.wethinkcode.healthsafe;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class IngestionClientTest {
    private MockWebServer mockWebServer;
    private IngestionClient ingestionClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        ingestionClient = new IngestionClient(mockWebServer.url("/wards").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getWardsReturnsParsedWardsForSuccessfulResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {"id":"W-05","wing":"North","department":"Emergency","bedsAvailable":12},
                          {"id":"W-06","wing":"South","department":"Maternity","bedsAvailable":8}
                        ]
                        """));

        List<Ward> wards = ingestionClient.getWards();

        assertEquals(2, wards.size());
        assertEquals("W-05", wards.get(0).getId());
        assertEquals("Emergency", wards.get(0).getDepartment());
        assertEquals(12, wards.get(0).getBedsAvailable());
        assertEquals("W-06", wards.get(1).getId());
        assertEquals(8, wards.get(1).getBedsAvailable());

        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/wards", request.getPath());
    }

    @Test
    void getWardsReturnsEmptyListWhenResponseIsEmptyArray() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        assertTrue(ingestionClient.getWards().isEmpty());
    }

    @Test
    void getWardsThrowsIOExceptionForNonSuccessfulStatus() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        IOException exception = assertThrows(IOException.class, () -> ingestionClient.getWards());
        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    void getWardsThrowsIOExceptionForInvalidJson() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("this is not JSON"));

        assertThrows(IOException.class, () -> ingestionClient.getWards());
    }

    @Test
    void getWardsThrowsIOExceptionForEmptyResponseBody() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(""));

        assertThrows(IOException.class, () -> ingestionClient.getWards());
    }

    @Test
    void getWardsThrowsIOExceptionWhenServerCannotBeReached() throws Exception {
        mockWebServer.shutdown();
        assertThrows(IOException.class, () -> ingestionClient.getWards());
    }
}
