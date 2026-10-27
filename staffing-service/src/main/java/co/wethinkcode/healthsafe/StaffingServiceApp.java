package co.wethinkcode.healthsafe;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import io.javalin.Javalin;

public class StaffingServiceApp {

    private static final int PORT = 7033;

    private static final Pattern WARD_ID =
            Pattern.compile("[A-Za-z0-9-]{1,20}");

    public static void main(String[] args) {
        StaffingEventPublisher publisher = new StaffingEventPublisher();

        Runtime.getRuntime().addShutdownHook(
                new Thread(publisher::close)
        );

        Javalin app = createApp(
                new WardClient(),
                new AlertLevelClient(),
                publisher::publish
        );

        app.start(PORT);

        System.out.println(
                "Staffing Service started on port " + PORT
        );
    }

    /**
     * Creates the application without a schedule event publisher.
     * Kept static for compatibility with existing tests.
     */
    public static Javalin createApp(
            WardClient wardClient,
            AlertLevelClient alertLevelClient
    ) {
        return createApp(
                wardClient,
                alertLevelClient,
                schedule -> {
                    // No publisher configured.
                }
        );
    }

    @FunctionalInterface
    public interface SchedulePublisher {
        void publish(Schedule schedule) throws Exception;
    }

    /**
     * Creates the application with a schedule event publisher.
     */
    public static Javalin createApp(
            WardClient wardClient,
            AlertLevelClient alertLevelClient,
            SchedulePublisher publisher
    ) {
        Javalin app = Javalin.create();

        Map<String, Schedule> lastPublishedByWard =
                new ConcurrentHashMap<>();

        // GET /health
        app.get("/health", ctx -> ctx.result("OK"));

        /*
         * GET /schedule/{wardId}
         *
         * 200 - schedule successfully calculated
         * 400 - invalid ward ID
         * 404 - ward does not exist
         * 503 - a downstream service is unavailable
         */
        app.get("/schedule/{wardId}", ctx -> {
            String wardId = ctx.pathParam("wardId")
                    .trim()
                    .toUpperCase(Locale.ROOT);

            // Validate the ward ID before making service calls.
            if (!WARD_ID.matcher(wardId).matches()) {
                ctx.status(400).json(Map.of(
                        "error",
                        "Ward id must be 1-20 letters, digits or dashes"
                ));
                return;
            }

            try {
                // Retrieve the ward.
                Ward ward = wardClient.getWard(wardId);

                // Retrieve the emergency level.
                int level = alertLevelClient.getLevel();

                // Calculate the on-call schedule.
                Schedule schedule =
                        ScheduleCalculator.calculate(ward, level);

                // Remember the last published schedule for this ward.
                Schedule previous =
                        lastPublishedByWard.put(wardId, schedule);

                // Publish only when the schedule changes.
                if (!schedule.equals(previous)) {
                    try {
                        publisher.publish(schedule);
                    } catch (Exception publishFailure) {
                        /*
                         * Allow a later request to retry publication
                         * if publishing fails.
                         */
                        lastPublishedByWard.remove(wardId, schedule);

                        System.err.println(
                                "Could not publish staffing event: "
                                        + publishFailure.getMessage()
                        );
                    }
                }

                ctx.json(schedule);

            } catch (WardNotFoundException e) {
                ctx.status(404).json(Map.of(
                        "error",
                        "Ward not found: " + wardId
                ));

            } catch (DownstreamUnavailableException e) {
                ctx.status(503).json(Map.of(
                        "error",
                        "Cannot build schedule right now: "
                                + e.getMessage()
                ));
            }
        });

        return app;
    }
}