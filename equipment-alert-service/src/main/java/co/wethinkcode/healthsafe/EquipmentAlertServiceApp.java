package co.wethinkcode.healthsafe;

import io.javalin.Javalin;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** REST health/read API and reliable ActiveMQ queue consumer for equipment alerts. */
public class EquipmentAlertServiceApp {
    private static final int PORT = 7034;

    public static void main(String[] args) {
        Path alertFile = Path.of(System.getenv().getOrDefault(
                "HEALTHSAFE_ALERTS_FILE", "data/equipment-alerts.jsonl"));
        AlertStore store = new AlertStore(alertFile);
        EquipmentAlertConsumer consumer = new EquipmentAlertConsumer(store);
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::close));
        createApp(store).start(PORT);
        consumer.start();
        System.out.println("Equipment Alert Service started on port " + PORT);
    }

    public static Javalin createApp(AlertStore store) {
        Javalin app = Javalin.create();
        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/alerts", ctx -> {
            try { ctx.json(store.all()); }
            catch (IOException e) { ctx.status(500).json(Map.of("error", "Could not read persisted alerts")); }
        });
        return app;
    }
}
