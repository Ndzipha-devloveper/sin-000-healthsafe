package co.wethinkcode.healthsafe;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Calls ward-service to check that a ward exists and to read its details. */
public class WardClient {

    private static final String SERVICE = "ward-service";
    private static final String DEFAULT_URL = "http://localhost:7031";

    private final HttpUrl baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WardClient() {
        this(DEFAULT_URL);
    }

    public WardClient(String baseUrl) {
        this(baseUrl, HttpClients.withTimeouts());
    }

    // Package-private so tests can pass a client with very short timeouts.
    WardClient(String baseUrl, OkHttpClient httpClient) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        if (this.baseUrl == null) {
            throw new IllegalArgumentException("Invalid ward-service URL: " + baseUrl);
        }
        this.httpClient = httpClient;
    }

    /**
     * Fetches one ward.
     *
     * @throws WardNotFoundException          if ward-service answers 404
     * @throws DownstreamUnavailableException if ward-service is down, slow,
     *                                        returns another error, or sends bad JSON
     */
    public Ward getWard(String wardId) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("wards")
                .addPathSegment(wardId)
                .build();

        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 404) {
                throw new WardNotFoundException(wardId);
            }
            if (!response.isSuccessful()) {
                throw new DownstreamUnavailableException(
                        SERVICE, "returned HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new DownstreamUnavailableException(SERVICE, "returned an empty response");
            }
            return objectMapper.readValue(body.string(), Ward.class);

        } catch (IOException e) {
            // Covers connection refused, timeouts and unreadable JSON.
            throw new DownstreamUnavailableException(
                    SERVICE, "could not be reached or understood: " + e.getMessage(), e);
        }
    }
}