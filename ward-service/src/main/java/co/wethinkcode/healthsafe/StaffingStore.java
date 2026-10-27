package co.wethinkcode.healthsafe;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the latest staffing update for each ward, in memory. */
public class StaffingStore {

    private final Map<String, StaffingUpdate> latestByWard = new ConcurrentHashMap<>();

    /** Saves the update. A newer update for the same ward replaces the older one. */
    public void put(StaffingUpdate update) {
        latestByWard.put(normalise(update.wardId()), update);
    }

    /** @return the latest update for the ward, or null if none has arrived yet */
    public StaffingUpdate get(String wardId) {
        return latestByWard.get(normalise(wardId));
    }

    private static String normalise(String wardId) {
        return wardId.trim().toUpperCase(Locale.ROOT);
    }
}