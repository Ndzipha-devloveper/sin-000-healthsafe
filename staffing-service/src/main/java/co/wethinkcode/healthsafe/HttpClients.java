package co.wethinkcode.healthsafe;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/** Builds the HTTP client used to call other services, with explicit timeouts. */
final class HttpClients {

    private HttpClients() {
    }

    /**
     * Without timeouts, a service that hangs would make staffing-service hang too.
     * These limits make a slow service fail fast instead.
     */
    static OkHttpClient withTimeouts() {
        return new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .callTimeout(5, TimeUnit.SECONDS)
                .build();
    }
}