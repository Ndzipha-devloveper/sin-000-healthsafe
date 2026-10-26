package co.wethinkcode.healthsafe;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.Javalin;

public class AlertLevelServiceApp {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        createApp(new AlertLevel()).start(7032);
    }

    // Builds the app without starting it, so that tests can start it on a random port.
    public static Javalin createApp(AlertLevel alertLevel) {
        Javalin app = Javalin.create();

        app.get("/health", ctx -> ctx.result("OK"));

        app.get("/alert-level", ctx -> ctx.json(Map.of("level", alertLevel.getLevel())));

        app.put("/alert-level", ctx -> {
            Integer requestedLevel = parseLevel(ctx.body());
            if (requestedLevel == null) {
                ctx.status(400).json(Map.of("error", "Body must be JSON like {\"level\": 0-8}"));
                return;
            }
            try {
                alertLevel.setLevel(requestedLevel);
                ctx.json(Map.of("level", alertLevel.getLevel()));
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            }
        });

        return app;
    }

    /**
     * Reads {"level": n} and returns n, or null if the body is not a JSON object
     * with an integer "level". Missing fields, strings, decimals, nulls and
     * numbers too big for an int are all rejected.
     */
    static Integer parseLevel(String body) {
        if (body == null) return null;
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node == null || !node.isObject()) return null;
            JsonNode level = node.get("level");
            return (level != null && level.isInt()) ? level.intValue() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}