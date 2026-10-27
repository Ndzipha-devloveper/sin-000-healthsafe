package co.wethinkcode.healthsafe;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Calls alert-level-service to read the current Emergency Status (0-8). */
public class AlertLevelClient {

    private static final String SERVICE = "alert-level-service";
    private static final String DEFAULT_URL = "http://localhost:7032";
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 8;

    private final HttpUrl baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlertLevelClient() {
        this(DEFAULT_URL);
    }

    public AlertLevelClient(String baseUrl) {
        this(baseUrl, HttpClients.withTimeouts());
    }

    // Package-private so tests can pass a client with very short timeouts.
    AlertLevelClient(String baseUrl, OkHttpClient httpClient) {
        this.baseUrl = HttpUrl.parse(baseUrl);
        if (this.baseUrl == null) {
            throw new IllegalArgumentException("Invalid alert-level-service URL: " + baseUrl);
        }
        this.httpClient = httpClient;
    }

    /**
     * Reads the current alert level.
     *
     * @return a whole number from 0 to 8
     * @throws DownstreamUnavailableException if the service is down, slow,
     *                                        returns an error, or sends a level outside 0-8
     */
    public int getLevel() {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("alert-level").build();
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new DownstreamUnavailableException(
                        SERVICE, "returned HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new DownstreamUnavailableException(SERVICE, "returned an empty response");
            }

            JsonNode level = objectMapper.readTree(body.string()).get("level");
            if (level == null || !level.isInt()
                    || level.intValue() < MIN_LEVEL
                    || level.intValue() > MAX_LEVEL) {
                throw new DownstreamUnavailableException(
                        SERVICE, "returned a missing or invalid level");
            }
            return level.intValue();

        } catch (IOException e) {
            // Covers connection refused, timeouts and unreadable JSON.
            throw new DownstreamUnavailableException(
                    SERVICE, "could not be reached or understood: " + e.getMessage(), e);
        }
    }
}
