package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class WardClientTest {

    private MockWebServer server;
    private WardClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient fastClient = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.MILLISECONDS)
                .build();
        client = new WardClient(server.url("/").toString(), fastClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void returnsTheWardWhenWardServiceAnswers200() throws Exception {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"W-01\",\"wing\":\"East Wing\",\"department\":\"Cardiology\","
                        + "\"bedsAvailable\":3,\"somethingNew\":true}"));

        Ward ward = client.getWard("W-01");

        assertEquals("W-01", ward.id());
        assertEquals("Cardiology", ward.department());
        assertEquals(3, ward.bedsAvailable());

        RecordedRequest request = server.takeRequest();
        assertEquals("/wards/W-01", request.getPath());
    }

    @Test
    void throwsWardNotFoundWhenWardServiceAnswers404() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThrows(WardNotFoundException.class, () -> client.getWard("W-99"));
    }

    @Test
    void throwsDownstreamUnavailableWhenWardServiceAnswers500() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThrows(DownstreamUnavailableException.class, () -> client.getWard("W-01"));
    }

    @Test
    void throwsDownstreamUnavailableWhenBodyIsNotJson() {
        server.enqueue(new MockResponse().setBody("this is not json"));

        assertThrows(DownstreamUnavailableException.class, () -> client.getWard("W-01"));
    }

    @Test
    void throwsDownstreamUnavailableWhenWardServiceIsSlow() {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"W-01\"}")
                .setHeadersDelay(2, TimeUnit.SECONDS));

        assertThrows(DownstreamUnavailableException.class, () -> client.getWard("W-01"));
    }

    @Test
    void throwsDownstreamUnavailableWhenWardServiceIsNotRunning() throws IOException {
        server.shutdown();

        assertThrows(DownstreamUnavailableException.class, () -> client.getWard("W-01"));
    }

    @Test
    void rejectsAnInvalidBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> new WardClient("not a url"));
    }
}