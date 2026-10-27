package co.wethinkcode.healthsafe;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.javalin.Javalin;

/**
 * Starts the Ward Service REST API on port 7031.
 *
 * Provides endpoints for:
 * - Retrieving all wards
 * - Finding a ward by ID
 * - Retrieving hospital departments
 * - Retrieving staffing updates
 * - Reporting equipment failures
 */
public class WardServiceApp {

    /**
     * Starts the Ward Service.
     */
    public static void main(String[] args) {
        IngestionClient ingestionClient = new IngestionClient();
        StaffingStore staffingStore = new StaffingStore();

        EquipmentFailurePublisher equipmentFailurePublisher =
                new EquipmentFailurePublisher();

        Javalin app = createApp(
                ingestionClient,
                staffingStore,
                equipmentFailurePublisher
        );

        app.start(7031);

        // Listen for staffing updates in the background.
        new StaffingEventSubscriber(staffingStore).start();

        System.out.println("Ward Service running on port 7031");
    }

    /**
     * Creates the application using the default staffing store.
     *
     * This overload preserves compatibility with existing callers
     * that supply only an IngestionClient.
     */
    public static Javalin createApp(IngestionClient ingestionClient) {
        return createApp(
                ingestionClient,
                new StaffingStore()
        );
    }

    /**
     * Creates the application using the supplied staffing store.
     *
     * This overload preserves compatibility with existing callers
     * that supply an IngestionClient and StaffingStore.
     */
    public static Javalin createApp(
            IngestionClient ingestionClient,
            StaffingStore staffingStore
    ) {
        return createApp(
                ingestionClient,
                staffingStore,
                new EquipmentFailurePublisher()
        );
    }

    /**
     * Creates and configures the Ward Service REST API.
     *
     * @param ingestionClient client used to retrieve ward data
     * @param staffingStore store containing staffing updates
     * @param equipmentFailurePublisher publishes equipment failure alerts
     * @return configured Javalin application
     */
    public static Javalin createApp(
            IngestionClient ingestionClient,
            StaffingStore staffingStore,
            EquipmentFailurePublisher equipmentFailurePublisher
    ) {
        Javalin app = Javalin.create();

        /*
         * Health check.
         *
         * GET /health
         */
        app.get("/health", ctx -> ctx.result("OK"));

        /*
         * Retrieve all wards.
         *
         * GET /wards
         */
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

        /*
         * Retrieve one ward by ID.
         *
         * GET /wards/{id}
         */
        app.get("/wards/{id}", ctx -> {
            String requestedId = ctx.pathParam("id")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            try {
                List<Ward> wards = ingestionClient.getWards();

                Ward matchingWard = wards.stream()
                        .filter(ward ->
                                requestedId.equals(
                                        ward.getId() == null
                                                ? ""
                                                : ward.getId().trim()
                                                .toUpperCase(Locale.ROOT)
                                )
                        )
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

        /*
         * Retrieve a sorted list of distinct departments.
         *
         * GET /departments
         */
        app.get("/departments", ctx -> {
            try {
                List<Ward> wards = ingestionClient.getWards();

                List<String> departments = wards.stream()
                        .map(Ward::getDepartment)
                        .filter(department ->
                                department != null
                                        && !department.isBlank())
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

        /*
         * Retrieve the latest staffing update for a ward.
         *
         * GET /wards/{id}/staffing
         */
        app.get("/wards/{id}/staffing", ctx -> {
            String requestedId = ctx.pathParam("id")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            StaffingUpdate update = staffingStore.get(requestedId);

            if (update == null) {
                ctx.status(404);
                ctx.json(Map.of(
                        "error",
                        "No staffing update received for this ward yet"
                ));
                return;
            }

            ctx.json(update);
        });

        /*
         * Report an equipment failure for a ward.
         *
         * POST /wards/{id}/equipment-failures
         *
         * Example request body:
         * {
         *   "wardId": "W-01",
         *   "equipment": "Ventilator",
         *   "description": "Ventilator stopped working",
         *   "reportedAt": "2026-10-26T12:00:00Z"
         * }
         *
         * Responses:
         * 202 - failure report accepted and published
         * 400 - invalid request
         * 404 - ward does not exist
         * 503 - ward service cannot retrieve ward data,
         *       or the failure report could not be published
         */
        app.post("/wards/{id}/equipment-failures", ctx -> {
            String requestedWardId = ctx.pathParam("id")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (requestedWardId.isBlank()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Ward ID is required"
                ));
                return;
            }

            final EquipmentFailure submittedFailure;

            try {
                submittedFailure =
                        ctx.bodyAsClass(EquipmentFailure.class);

            } catch (Exception e) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Invalid equipment failure request body"
                ));
                return;
            }

            if (submittedFailure == null
                    || submittedFailure.equipment() == null
                    || submittedFailure.equipment().isBlank()) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Equipment name is required"
                ));
                return;
            }

            // If the request body includes a ward ID, it must match
            // the ward ID in the URL.
            if (submittedFailure.wardId() != null
                    && !submittedFailure.wardId().isBlank()
                    && !requestedWardId.equals(
                    submittedFailure.wardId()
                            .trim()
                            .toUpperCase(Locale.ROOT)
            )) {
                ctx.status(400);
                ctx.json(Map.of(
                        "error",
                        "Ward ID in the URL does not match the request body"
                ));
                return;
            }

            // Confirm that the ward exists before publishing the report.
            try {
                List<Ward> wards = ingestionClient.getWards();

                boolean wardExists = wards.stream()
                        .anyMatch(ward ->
                                ward.getId() != null
                                        && requestedWardId.equals(
                                        ward.getId()
                                                .trim()
                                                .toUpperCase(Locale.ROOT)
                                )
                        );

                if (!wardExists) {
                    ctx.status(404);
                    ctx.json(Map.of(
                            "error",
                            "Ward not found"
                    ));
                    return;
                }

            } catch (IOException e) {
                ctx.status(503);
                ctx.json(Map.of(
                        "error",
                        "Ward data is currently unavailable"
                ));
                return;
            }

            // Use the URL's ward ID as the authoritative ward ID.
            EquipmentFailure failure = new EquipmentFailure(
                    requestedWardId,
                    submittedFailure.equipment(),
                    submittedFailure.description(),
                    submittedFailure.reportedAt()
            ).withDefaults();

            // Publish the failure report to the ActiveMQ queue.
            try {
                equipmentFailurePublisher.publish(failure);

                ctx.status(202);
                ctx.json(Map.of(
                        "message",
                        "Equipment failure report accepted",
                        "wardId",
                        failure.wardId(),
                        "equipment",
                        failure.equipment()
                ));

            } catch (Exception e) {
                ctx.status(503);
                ctx.json(Map.of(
                        "error",
                        "Could not publish the equipment failure report"
                ));
            }
        });

        return app;
    }
}