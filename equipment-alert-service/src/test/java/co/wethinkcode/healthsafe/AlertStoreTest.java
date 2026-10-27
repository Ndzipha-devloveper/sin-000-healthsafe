package co.wethinkcode.healthsafe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertStoreTest {
    @TempDir
    Path tempDir;

    private EquipmentFailureAlert alert(String equipment, String description) {
        return new EquipmentFailureAlert("W-01", equipment, description, "2026-10-26T14:15:00Z");
    }

    @Test
    void returnsAnEmptyListWhenStorageFileDoesNotExist() throws IOException {
        AlertStore store = new AlertStore(tempDir.resolve("alerts.jsonl"));

        assertTrue(store.all().isEmpty());
    }

    @Test
    void appendsAndReadsAlertsInOrder() throws IOException {
        AlertStore store = new AlertStore(tempDir.resolve("alerts.jsonl"));
        EquipmentFailureAlert first = alert("Ventilator", "Not starting");
        EquipmentFailureAlert second = alert("Monitor", "Screen blank");

        store.append(first);
        store.append(second);

        assertEquals(List.of(first, second), store.all());
    }

    @Test
    void doesNotWriteAnIdenticalAlertTwice() throws IOException {
        Path file = tempDir.resolve("alerts.jsonl");
        AlertStore store = new AlertStore(file);
        EquipmentFailureAlert alert = alert("Ventilator", "Not starting");

        store.append(alert);
        store.append(alert);

        assertEquals(List.of(alert), store.all());
        assertEquals(1, Files.readAllLines(file).size());
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        Path file = tempDir.resolve("nested/data/alerts.jsonl");
        AlertStore store = new AlertStore(file);
        EquipmentFailureAlert alert = alert("Ventilator", "Not starting");

        store.append(alert);

        assertTrue(Files.exists(file));
        assertEquals(List.of(alert), store.all());
    }

    @Test
    void writesOneJsonObjectPerLine() throws IOException {
        Path file = tempDir.resolve("alerts.jsonl");
        AlertStore store = new AlertStore(file);

        store.append(alert("Ventilator", "Not starting"));
        store.append(alert("Monitor", "Screen blank"));

        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("{"));
        assertTrue(lines.get(1).endsWith("}"));
        assertFalse(lines.get(0).isBlank());
    }
}
