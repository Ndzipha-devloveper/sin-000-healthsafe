package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

/**
 * Communicates with the Ingestion Service to retrieve hospital ward data.
 * Sends HTTP requests and converts JSON responses into Ward objects.
 */
public class IngestionClient {

    private static final String DEFAULT_INGESTION_URL =
            "http://localhost:7030/wards";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String ingestionUrl;

    /**
     * Creates an IngestionClient using the default Ingestion Service URL.
     */
    public IngestionClient() {
        this(DEFAULT_INGESTION_URL);
    }

    /**
     * Creates an IngestionClient using a specified URL.
     * Useful for testing with MockWebServer.
     *
     * @param ingestionUrl URL of the Ingestion Service wards endpoint
     */
    public IngestionClient(String ingestionUrl) {
        if (ingestionUrl == null || ingestionUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Ingestion URL must not be null or blank"
            );
        }

        this.ingestionUrl = ingestionUrl;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retrieves all wards from the Ingestion Service.
     *
     * @return a list of wards retrieved from the Ingestion Service
     * @throws IOException if the request fails, the response is unsuccessful,
     *                     the response body is empty, or JSON parsing fails
     */
    public List<Ward> getWards() throws IOException {
        Request request = new Request.Builder()
                .url(ingestionUrl)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException(
                        "Ingestion Service returned HTTP "
                                + response.code()
                );
            }

            if (response.body() == null) {
                throw new IOException(
                        "Ingestion Service returned an empty response"
                );
            }

            return objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<List<Ward>>() {}
            );
        }
    }
}