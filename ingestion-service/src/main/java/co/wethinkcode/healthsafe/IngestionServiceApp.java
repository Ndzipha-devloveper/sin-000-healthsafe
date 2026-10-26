package co.wethinkcode.healthsafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.javalin.Javalin;

/**
 * Ingestion service for loading, cleaning, and serving ward data.
 */
public class IngestionServiceApp {

    private static final int PORT = 7030;
    private static final String WARDS_FILE = "/wards-outdated.csv";
    private static final int EXPECTED_FIELDS = 4;
    private static final int MAX_BEDS = 100;

    private static final Set<String> INVALID_PLACEHOLDERS =
            Set.of("N/A", "NA", "NULL", "UNKNOWN");

    private static final Map<String, String> DEPARTMENTS = createDepartments();

    private IngestionServiceApp() {
        // Utility class.
    }

    /**
     * Starts the ingestion service.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        List<Ward> wards = loadWards();

        Javalin app = createApp(wards);
        app.start(PORT);

        System.out.println("Ingestion Service running on port " + PORT);
    }

    /**
     * Creates the REST application.
     *
     * @param wards the wards to serve
     * @return configured Javalin application
     */
    public static Javalin createApp(List<Ward> wards) {
        Javalin app = Javalin.create();

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx -> ctx.json(wards));

        app.get("/wards/{id}", ctx -> {
            String id = cleanId(ctx.pathParam("id"));

            Ward ward = wards.stream()
                    .filter(candidate -> candidate.id().equalsIgnoreCase(id))
                    .findFirst()
                    .orElse(null);

            if (ward == null) {
                ctx.status(404).json(Map.of("error", "Ward not found"));
                return;
            }

            ctx.json(ward);
        });

        return app;
    }

    /**
     * Creates a reader for the default bundled CSV file.
     *
     * @return reader for the CSV file
     * @throws IOException if the file cannot be opened
     */
    public static BufferedReader csvFileReader() throws IOException {
        return csvFileReader(WARDS_FILE);
    }

    /**
     * Creates a reader for a bundled CSV resource.
     *
     * @param resource the CSV resource path
     * @return reader for the CSV file
     * @throws IOException if the file cannot be opened
     */
    public static BufferedReader csvFileReader(String resource)
            throws IOException {

        if (isMissingValue(resource)) {
            throw new IOException("CSV resource path cannot be blank");
        }

        String resourcePath = resource.startsWith("/")
                ? resource
                : "/" + resource;

        InputStream input = IngestionServiceApp.class
                .getResourceAsStream(resourcePath);

        if (input == null) {
            throw new IOException(
                    "Could not find CSV resource: " + resourcePath);
        }

        return new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    /**
     * Checks whether a value is null, blank, or a known placeholder.
     *
     * @param value value to check
     * @return true if the value is missing or invalid
     */
    private static boolean isMissingValue(String value) {
        return value == null
                || value.isBlank()
                || INVALID_PLACEHOLDERS.contains(
                        value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Cleans a ward ID.
     *
     * @param id raw ward ID
     * @return cleaned ward ID, or an empty string if invalid
     */
    public static String cleanId(String id) {
        if (isMissingValue(id)) {
            return "";
        }

        return id.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Cleans a ward wing.
     *
     * @param wing raw wing value
     * @return cleaned wing value, or an empty string if invalid
     */
    public static String cleanWing(String wing) {
        if (isMissingValue(wing)) {
            return "";
        }

        return toTitleCase(wing);
    }

    /**
     * Cleans and standardises a department name.
     *
     * @param department raw department name
     * @return standardised department name, or an empty string if invalid
     */
    public static String cleanDepartment(String department) {
        if (isMissingValue(department)) {
            return "";
        }

        String normalised = department.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);

        String canonical = DEPARTMENTS.get(normalised);

        if (canonical != null) {
            return canonical;
        }

        return toTitleCase(department);
    }

    /**
     * Converts text to title case and normalises whitespace.
     *
     * @param text text to convert
     * @return title-cased text
     */
    public static String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return Arrays.stream(text.trim().split("\\s+"))
                .map(word -> {
                    String lower = word.toLowerCase(Locale.ROOT);

                    return Character.toUpperCase(lower.charAt(0))
                            + lower.substring(1);
                })
                .collect(Collectors.joining(" "));
    }

    /**
     * Cleans a beds-available value.
     *
     * "full" becomes 0.
     * Numeric digits between 0 and MAX_BEDS become integers.
     * Number words, negative numbers, values above MAX_BEDS,
     * and invalid values become null.
     *
     * @param beds raw beds-available value
     * @return cleaned bed count, or null if invalid
     */
    public static Integer cleanBeds(String beds) {
        if (beds == null || beds.isBlank()) {
            return null;
        }

        String value = beds.trim().toLowerCase(Locale.ROOT);

        if (value.equals("full")) {
            return 0;
        }

        Long number = parseDigits(value);

        if (number == null || number > MAX_BEDS) {
            return null;
        }

        return number.intValue();
    }

    /**
     * Parses a value containing numeric digits only.
     *
     * @param value value to parse
     * @return parsed number, or null if invalid
     */
    private static Long parseDigits(String value) {
        if (value == null || !value.matches("\\d+")) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses one CSV row into a Ward.
     *
     * @param line CSV row
     * @return parsed ward, or null if the row is invalid
     */
    public static Ward parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] fields = line.split(",", -1);

        if (fields.length != EXPECTED_FIELDS) {
            return null;
        }

        String id = cleanId(fields[0]);

        if (id.isBlank()) {
            return null;
        }

        return new Ward(
                id,
                cleanWing(fields[1]),
                cleanDepartment(fields[2]),
                cleanBeds(fields[3])
        );
    }

    /**
     * Loads and cleans wards from the default bundled CSV file.
     *
     * @return list of cleaned wards
     */
    public static List<Ward> loadWards() {
        return loadWards(WARDS_FILE);
    }

    /**
     * Loads and cleans wards from a specified bundled CSV resource.
     *
     * @param resource CSV resource path
     * @return list of cleaned wards
     */
    public static List<Ward> loadWards(String resource) {
        List<Ward> wards = new ArrayList<>();

        try (BufferedReader reader = csvFileReader(resource)) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;

                    String header = line.toLowerCase(Locale.ROOT);

                    if (header.contains("id")
                            && header.contains("department")) {
                        continue;
                    }
                }

                Ward ward = parseLine(line);

                if (ward != null) {
                    wards.add(ward);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not load ward data from " + resource, e);
        }

        return mergeDuplicates(wards);
    }

    /**
     * Merges duplicate wards by their ID, preserving insertion order.
     *
     * @param wards input wards
     * @return wards with duplicate IDs merged
     */
    public static List<Ward> mergeDuplicates(List<Ward> wards) {
        if (wards == null || wards.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Ward> merged = new LinkedHashMap<>();

        for (Ward ward : wards) {
            if (ward == null || ward.id() == null || ward.id().isBlank()) {
                continue;
            }

            String id = ward.id().trim().toUpperCase(Locale.ROOT);
            Ward existing = merged.get(id);

            if (existing == null) {
                merged.put(id, new Ward(
                        id,
                        ward.wing(),
                        ward.department(),
                        ward.bedsAvailable()
                ));
            } else {
                merged.put(id, mergeWards(existing, ward));
            }
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Merges two records for the same ward.
     * Existing non-empty values are preferred; missing values are filled
     * from the incoming record.
     *
     * @param existing existing ward
     * @param incoming incoming ward
     * @return merged ward
     */
    public static Ward mergeWards(Ward existing, Ward incoming) {
        if (existing == null) {
            return incoming;
        }

        if (incoming == null) {
            return existing;
        }

        String wing = hasText(existing.wing())
                ? existing.wing()
                : incoming.wing();

        String department = hasText(existing.department())
                ? existing.department()
                : incoming.department();

        Integer beds = existing.bedsAvailable() != null
                ? existing.bedsAvailable()
                : incoming.bedsAvailable();

        return new Ward(existing.id(), wing, department, beds);
    }

    /**
     * Checks whether a string contains meaningful text.
     *
     * @param value value to check
     * @return true if the value is not null or blank
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Creates a map of recognised department names and aliases.
     *
     * @return department normalisation map
     */
    private static Map<String, String> createDepartments() {
        Map<String, String> departments = new LinkedHashMap<>();

        departments.put("accident and emergency", "Accident and Emergency");
        departments.put("a&e", "Accident and Emergency");
        departments.put("emergency", "Accident and Emergency");
        departments.put("cardiology", "Cardiology");
        departments.put("general medicine", "General Medicine");
        departments.put("medicine", "General Medicine");
        departments.put("paediatrics", "Paediatrics");
        departments.put("pediatrics", "Paediatrics");
        departments.put("maternity", "Maternity");
        departments.put("obstetrics", "Obstetrics");
        departments.put("gynaecology", "Gynaecology");
        departments.put("gynecology", "Gynaecology");
        departments.put("surgery", "Surgery");
        departments.put("general surgery", "General Surgery");
        departments.put("orthopaedics", "Orthopaedics");
        departments.put("orthopedics", "Orthopaedics");
        departments.put("intensive care", "Intensive Care");
        departments.put("icu", "Intensive Care");
        departments.put("oncology", "Oncology");
        departments.put("radiology", "Radiology");
        departments.put("psychiatry", "Psychiatry");
        departments.put("neurology", "Neurology");
        departments.put("ophthalmology", "Ophthalmology");
        departments.put("dermatology", "Dermatology");
        departments.put("renal", "Renal");
        departments.put("nephrology", "Nephrology");
        departments.put("outpatients", "Outpatients");
        departments.put("outpatient", "Outpatients");

        return Map.copyOf(departments);
    }

    /**
     * Ward data model.
     *
     * @param id ward identifier
     * @param wing ward wing
     * @param department ward department
     * @param bedsAvailable available bed count, or null if invalid/unknown
     */
    public record Ward(
            String id,
            String wing,
            String department,
            Integer bedsAvailable
    ) {
    }
}
