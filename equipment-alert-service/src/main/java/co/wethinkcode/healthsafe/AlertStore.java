package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Append-only local persistence for alerts; prevents duplicate records. */
public class AlertStore {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public AlertStore(Path file) {
        this.file = file;
    }

    public synchronized void append(EquipmentFailureAlert alert)
            throws IOException {

        // Load existing alerts.
        Set<EquipmentFailureAlert> existingAlerts =
                new HashSet<>(all());

        // Skip this alert if an identical record already exists.
        if (existingAlerts.contains(alert)) {
            return;
        }

        Path parent = file.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                file,
                mapper.writeValueAsString(alert)
                        + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
    }

    public synchronized List<EquipmentFailureAlert> all()
            throws IOException {

        if (!Files.exists(file)) {
            return List.of();
        }

        List<EquipmentFailureAlert> alerts = new ArrayList<>();

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                alerts.add(
                        mapper.readValue(line, EquipmentFailureAlert.class)
                );
            }
        }

        return List.copyOf(alerts);
    }
}