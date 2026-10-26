package co.wethinkcode.healthsafe;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.ibm.icu.text.RuleBasedNumberFormat;

import io.javalin.Javalin;

public class IngestionServiceApp {

    private static final int PORT = 7030;
    private static final String WARDS_FILE = "/wards-outdated.csv";
    private static final int EXPECTED_FIELDS = 4;
    private static final int MAX_BEDS = 100;

    private static final Map<String, String> DEPARTMENTS = Map.of(
            "cardiology", "Cardiology",
            "paediatrics", "Paediatrics",
            "pediatrics", "Paediatrics",
            "oncology", "Oncology",
            "radiology", "Radiology",
            "maternity", "Maternity",
            "icu", "ICU"
    );

    private static final RuleBasedNumberFormat ENGLISH_NUMBER_WORD_PARSER =
            new RuleBasedNumberFormat(
                    Locale.ENGLISH,
                    RuleBasedNumberFormat.SPELLOUT
            );

    public static void main(String[] args) throws IOException {
        // Load and clean the ward data before starting the server.
        List<Ward> wards = loadWards(WARDS_FILE);

        Javalin app = Javalin.create().start(PORT);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/wards", ctx -> ctx.json(wards));
    }

    /**
     * Reads a classpath resource as UTF-8 and returns its lines.
     *
     * @param filePath classpath resource path
     * @return the lines in the resource
     * @throws IOException if the resource cannot be read
     */
    public static List<String> csvFileReader(String filePath) throws IOException {
        try (InputStream input =
                     IngestionServiceApp.class.getResourceAsStream(filePath)) {

            if (input == null) {
                throw new FileNotFoundException(
                        "CSV resource not found: " + filePath
                );
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {

                return reader.lines().toList();
            }
        }
    }

    /**
     * Cleans a ward ID by trimming whitespace and converting it to uppercase.
     */
    public static String cleanId(String id) {
        return id == null ? null : id.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Cleans a wing name by trimming whitespace and applying title case.
     * Returns null for null or blank values.
     */
    public static String cleanWing(String wing) {
        if (wing == null || wing.isBlank()) {
            return null;
        }

        return toTitleCase(wing);
    }

    /**
     * Normalises recognised department names.
     * Returns null for unknown, null, or blank values.
     */
    public static String cleanDepartment(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }

        return DEPARTMENTS.get(department.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Converts ordinary English words to title case.
     */
    public static String toTitleCase(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        return Arrays.stream(text.trim().split("\\s+"))
                .map(word ->
                        word.substring(0, 1).toUpperCase(Locale.ROOT)
                                + word.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    /**
     * Cleans a beds-available value.
     *
     * "full" becomes 0.
     * Digits and English number words become integers.
     * Negative numbers, numbers above MAX_BEDS, and invalid values become null.
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

        if (number == null) {
            number = parseWords(value);
        }

        if (number == null || number < 0 || number > MAX_BEDS) {
            return null;
        }

        return number.intValue();
    }

    private static Long parseDigits(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseWords(String value) {
        ParsePosition position = new ParsePosition(0);

        Number result = ENGLISH_NUMBER_WORD_PARSER.parse(value, position);

        // Reject invalid text and values that were only partially parsed.
        if (result == null || position.getIndex() != value.length()) {
            return null;
        }

        // Reject decimal values such as "one point five".
        if (result.doubleValue() != result.longValue()) {
            return null;
        }

        return result.longValue();
    }

    /**
     * Converts one CSV row into a cleaned Ward.
     *
     * @throws IllegalArgumentException if the row has fewer than four fields
     *                                  or has a missing ward ID
     */
    public static Ward parseLine(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("CSV row is blank");
        }

        String[] fields = line.split(",", -1);

        if (fields.length < EXPECTED_FIELDS) {
            throw new IllegalArgumentException(
                    "Expected " + EXPECTED_FIELDS + " fields: " + line
            );
        }

        if (fields.length > EXPECTED_FIELDS) {
            throw new IllegalArgumentException(
                    "Expected " + EXPECTED_FIELDS + " fields but found "
                            + fields.length + ": " + line
            );
        }

        String id = cleanId(fields[0]);

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Missing ward ID: " + line);
        }

        return new Ward(
                id,
                cleanWing(fields[1]),
                cleanDepartment(fields[2]),
                cleanBeds(fields[3])
        );
    }

    /**
     * Reads the CSV, skips its header and blank lines, and parses valid rows.
     * Malformed rows are logged and skipped.
     * Duplicate ward IDs are merged in their original order.
     */
    public static List<Ward> loadWards(String filePath) throws IOException {
        List<Ward> parsed = new ArrayList<>();

        List<String> lines = csvFileReader(filePath);

        for (String line : lines.stream().skip(1).toList()) {
            if (line.isBlank()) {
                continue;
            }

            try {
                parsed.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                System.err.println("Skipping bad row: " + e.getMessage());
            }
        }

        return mergeDuplicates(parsed);
    }

    /**
     * Merges wards with matching IDs while preserving the first occurrence's
     * position in the result.
     */
    public static List<Ward> mergeDuplicates(List<Ward> wards) {
        Map<String, Ward> byId = new LinkedHashMap<>();

        for (Ward ward : wards) {
            byId.merge(ward.id(), ward, IngestionServiceApp::mergeWards);
        }

        return List.copyOf(byId.values());
    }

    /**
     * Combines two records for the same ward.
     * The first non-null value wins for each field.
     */
    static Ward mergeWards(Ward first, Ward second) {
        return new Ward(
                first.id(),
                first.wing() != null ? first.wing() : second.wing(),
                first.department() != null
                        ? first.department()
                        : second.department(),
                first.bedsAvailable() != null
                        ? first.bedsAvailable()
                        : second.bedsAvailable()
        );
    }

    public record Ward(
            String id,
            String wing,
            String department,
            Integer bedsAvailable
    ) {
    }
}