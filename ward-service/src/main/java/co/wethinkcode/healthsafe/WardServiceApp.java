
package co.wethinkcode.healthsafe;

import io.javalin.Javalin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Starts the Ward Service REST API on port 7031.
 * Provides endpoints to retrieve all wards, find a ward by ID,
 * and retrieve the departments available in the hospital.
 * Retrieves ward data from the Ingestion Service.
 */
public class WardServiceApp {

    /**
     * Starts the Ward Service on port 7031.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        IngestionClient ingestionClient = new IngestionClient();
        Javalin app = createApp(ingestionClient);

        app.start(7031);

        System.out.println("Ward Service running on port 7031");
    }

    /**
     * Creates and configures the Javalin application with all Ward Service
     * REST API endpoints.
     *
     * The application retrieves ward data through the supplied
     * IngestionClient. This method does not start the server, allowing
     * the application to be tested independently of server startup.
     *
     * @param ingestionClient the client used to retrieve ward data
     * @return the configured Javalin application
     */
    public static Javalin createApp(IngestionClient ingestionClient) {
        Javalin app = Javalin.create();

        // Checks whether the Ward Service is running.
        app.get("/health", ctx -> ctx.result("OK"));

        // Retrieves all wards from the Ingestion Service.
        app.get("/wards", ctx -> {
            try {
                List<Ward> wards = ingestionClient.getWards();
                ctx.json(wards);
            } catch (IOException e) {
                ctx.status(503);
                ctx.json(Map.of(
                        "error",
                        "Ward data is currently unavailable"
                ));
            }
        });

        // Retrieves a specific ward using its unique ID.
        app.get("/wards/{id}", ctx -> {
            String requestedId = ctx.pathParam("id");

            try {
                List<Ward> wards = ingestionClient.getWards();

                Ward matchingWard = wards.stream()
                        .filter(ward ->
                                requestedId.equals(ward.getId()))
                        .findFirst()
                        .orElse(null);

                if (matchingWard == null) {
                    ctx.status(404);
                    ctx.json(Map.of(
                            "error",
                            "Ward not found"
                    ));
                    return;
                }

                ctx.json(matchingWard);

            } catch (IOException e) {
                ctx.status(503);
                ctx.json(Map.of(
                        "error",
                        "Ward data is currently unavailable"
                ));
            }
        });

        // Retrieves a sorted list of distinct department names.
        app.get("/departments", ctx -> {
            try {
                List<Ward> wards = ingestionClient.getWards();

                List<String> departments = wards.stream()
                        .map(Ward::getDepartment)
                        .filter(department ->
                                department != null && !department.isBlank())
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(TreeSet::new),
                                List::copyOf
                        ));

                ctx.json(departments);

            } catch (IOException e) {
                ctx.status(503);
                ctx.json(Map.of(
                        "error",
                        "Department data is currently unavailable"
                ));
            }
        });

        return app;
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL
// (see co.wethinkcode.healthsafe.mq.MqConfig).
// MQ TODO: publishes to ActiveMQ queue MqConfig.QUEUE when it detects
// an equipment failure on one of its wards.
