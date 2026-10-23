package co.wethinkcode.healthsafe;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static void main(String[] args) throws IOException {
        Javalin app = Javalin.create().start(7030);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/wards-outdated.csv (wards, wings, specialist departments data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        //This  prints raw vs cleaned rows checking the cleaning methods are working as expected
        List<Ward> wards = loadWards("/wards-outdated.csv");
        app.get("/wards", ctx -> ctx.json(wards));


    }
    public static List<String> csvFileReader(String filePath) throws IOException {
        try (InputStream input = IngestionServiceApp.class.getResourceAsStream(filePath)) {
            if (input == null) throw new FileNotFoundException(filePath + " not found");
            return new BufferedReader(new InputStreamReader(input)).lines().toList();
        }
    }

    /*
     * Cleans the ward ID by trimming whitespace and converting it to uppercase.
     */
    public static String cleanId(String id) {
        return id == null ? null : id.trim().toUpperCase();
    }

    /*
     * Cleans the wing name by
     * - Trimming whitespace
     * - Converting each word to Title Case
     * - Returning null if the value is blank or empty after trimming
     */
    public static String cleanWing(String wing) {
        if (wing == null || wing.isBlank()) return null;
        return toTitleCase(wing);
    }

    /*
     * Cleans the department name by normalizing casing and spelling variations.
     * - Converts the department name to lowercase for comparison.
     * - Unknown departments become null.
     */
    private static final Map<String, String> DEPARTMENTS = Map.of(
            "cardiology", "Cardiology",
            "paediatrics", "Paediatrics",
            "pediatrics", "Paediatrics",
            "oncology", "Oncology",
            "radiology", "Radiology",
            "maternity", "Maternity",
            "icu", "ICU");

    public  static String cleanDepartment(String department) {
        if (department == null || department.isBlank()) return null;
        return DEPARTMENTS.get(department.trim().toLowerCase());
    }

    // Helper function for title casing
    public  static String toTitleCase(String text) {
        return Arrays.stream(text.trim().split("\\s+"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static final int MAX_BEDS = 100;

    // Reads English number words such as "five" or "twenty-one" (ICU4J)
    private static final RuleBasedNumberFormat ENGLISH_NUMBER_WORD_PARSER =
            new RuleBasedNumberFormat(Locale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);

    /*
     * Cleans the beds-available value.
     * - "full" becomes 0
     * - digits and number words ("five") become numbers
     * - negatives, anything over MAX_BEDS, and non-numbers become null
     */
    public static Integer cleanBeds(String beds) {
        if (beds == null || beds.isBlank()) return null;
        String value = beds.trim().toLowerCase();

        if (value.equals("full")) return 0;

        Long number = parseDigits(value);
        if (number == null) number = parseWords(value);

        return (number == null || number < 0 || number > MAX_BEDS) ? null : number.intValue();
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

        // the whole text must be read, so "five apples" is rejected
        if (result == null || position.getIndex() != value.length()) return null;
        // reject decimals like "one point five"
        if (result.doubleValue() != result.longValue()) return null;

        return result.longValue();
    }

    /**
     * Turns one raw CSV line into a cleaned Ward.
     * The -1 limit keeps empty fields (like a missing wing).
     *
     * @throws IllegalArgumentException if the row has fewer than 4 fields
     *                                  or has no ward id
     */
    public  static Ward parseLine(String line) {
        String[] fields = line.split(",", -1);
        if (fields.length < 4) throw new IllegalArgumentException("Expected 4 fields: " + line);
        String id = cleanId(fields[0]);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing ward id: " + line);
        return new Ward(
                id,
                cleanWing(fields[1]),
                cleanDepartment(fields[2]),
                cleanBeds(fields[3]));
    }


    /**
     * Reads the CSV file, skips the header, and turns every valid row into a cleaned Ward.
     * Blank or malformed rows are logged and skipped. Duplicate ids are merged.
     */
    public static List<Ward> loadWards(String filePath) throws IOException {
       List<Ward> parsed = new ArrayList<>();
    for (String line : csvFileReader(filePath).stream().skip(1).toList()) {
        if (line.isBlank()) continue;
        try {
            parsed.add(parseLine(line));
        } catch (IllegalArgumentException e) {
            System.err.println("Skipping bad row: " + e.getMessage());
        }
    }
    return mergeDuplicates(parsed);
    }


    /*
     * Merges wards that share the same ID into one, keeping the original order.
     */
    public static List<Ward> mergeDuplicates(List<Ward> wards) {
        Map<String, Ward> byId = new LinkedHashMap<>();
        for (Ward ward : wards) {
            byId.merge(ward.id(), ward, IngestionServiceApp::mergeWards);
        }
        return List.copyOf(byId.values());
    }

    /*
     * Combines two rows for the same ward: the first non-null value wins for each field.
     */
    static Ward mergeWards(Ward first, Ward second) {
        return new Ward(
                first.id(),
                first.wing() != null ? first.wing() : second.wing(),
                first.department() != null ? first.department() : second.department(),
                first.bedsAvailable() != null ? first.bedsAvailable() : second.bedsAvailable());
    }


    public record Ward(String id, String wing, String department, Integer bedsAvailable) {}

}