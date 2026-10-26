package co.wethinkcode.healthsafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import co.wethinkcode.healthsafe.IngestionServiceApp.Ward;

class IngestionServiceAppTest {

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println(
                "-------- Running: " + testInfo.getDisplayName() + " --------");
    }

    // ---------- csvFileReader ----------

    @Test
    @DisplayName("csvFileReader reads the header and all data rows")
    void csvFileReader_returnsHeaderPlusAllRows() throws IOException {
        List<String> lines;

        try (BufferedReader reader = IngestionServiceApp.csvFileReader()) {
            lines = reader.lines().toList();
        }

        System.out.println("Lines read from CSV: " + lines.size());

        assertEquals(19, lines.size());
    }

    @Test
    @DisplayName("csvFileReader can load the default CSV using its resource path")
    void csvFileReader_readsSpecifiedResource() throws IOException {
        List<String> lines;

        try (BufferedReader reader =
                     IngestionServiceApp.csvFileReader("/wards-outdated.csv")) {
            lines = reader.lines().toList();
        }

        assertEquals(19, lines.size());
    }

    // ---------- cleanId ----------

    @Test
    @DisplayName("cleanId trims spaces and converts the ID to uppercase")
    void cleanId_trimsAndUppercases() {
        assertEquals("W-02", IngestionServiceApp.cleanId("  w-02  "));
    }

    @Test
    @DisplayName("cleanId returns an empty string for null or blank input")
    void cleanId_handlesNullAndBlank() {
        assertEquals("", IngestionServiceApp.cleanId(null));
        assertEquals("", IngestionServiceApp.cleanId(""));
        assertEquals("", IngestionServiceApp.cleanId("   "));
    }

    @Test
    @DisplayName("cleanId rejects placeholder values")
    void cleanId_rejectsPlaceholderValues() {
        assertEquals("", IngestionServiceApp.cleanId("N/A"));
        assertEquals("", IngestionServiceApp.cleanId("NA"));
        assertEquals("", IngestionServiceApp.cleanId("NULL"));
        assertEquals("", IngestionServiceApp.cleanId("UNKNOWN"));
    }

    // ---------- cleanWing ----------

    @Test
    @DisplayName("cleanWing trims and title-cases the wing")
    void cleanWing_trimsAndTitleCases() {
        assertEquals(
                "East Wing",
                IngestionServiceApp.cleanWing("  EAST WING  "));
    }

    @Test
    @DisplayName("cleanWing collapses repeated spaces")
    void cleanWing_collapsesRepeatedSpaces() {
        assertEquals(
                "East Wing",
                IngestionServiceApp.cleanWing("east    wing"));
    }

    @Test
    @DisplayName("cleanWing returns an empty string for null or blank input")
    void cleanWing_handlesNullAndBlank() {
        assertEquals("", IngestionServiceApp.cleanWing(null));
        assertEquals("", IngestionServiceApp.cleanWing(""));
        assertEquals("", IngestionServiceApp.cleanWing("   "));
    }

    @Test
    @DisplayName("cleanWing rejects placeholder values")
    void cleanWing_rejectsPlaceholderValues() {
        assertEquals("", IngestionServiceApp.cleanWing("N/A"));
        assertEquals("", IngestionServiceApp.cleanWing("NA"));
        assertEquals("", IngestionServiceApp.cleanWing("NULL"));
        assertEquals("", IngestionServiceApp.cleanWing("UNKNOWN"));
    }

    // ---------- cleanDepartment ----------

    @Test
    @DisplayName("cleanDepartment normalises recognised departments")
    void cleanDepartment_normalisesDepartments() {
        assertEquals(
                "Cardiology",
                IngestionServiceApp.cleanDepartment("cardiology"));

        assertEquals(
                "Paediatrics",
                IngestionServiceApp.cleanDepartment("pediatrics"));

        assertEquals(
                "Paediatrics",
                IngestionServiceApp.cleanDepartment("PAEDIATRICS"));

        assertEquals(
                "Oncology",
                IngestionServiceApp.cleanDepartment("ONCOLOGY"));

        assertEquals(
                "Intensive Care",
                IngestionServiceApp.cleanDepartment("ICU"));
    }

    @Test
    @DisplayName("cleanDepartment normalises repeated spaces")
    void cleanDepartment_collapsesRepeatedSpaces() {
        assertEquals(
                "General Medicine",
                IngestionServiceApp.cleanDepartment(" general   medicine "));
    }

    @Test
    @DisplayName("cleanDepartment title-cases unknown department names")
    void cleanDepartment_titleCasesUnknownDepartments() {
        assertEquals(
                "Radiotherapy",
                IngestionServiceApp.cleanDepartment("RADIOTHERAPY"));
    }

    @Test
    @DisplayName("cleanDepartment returns an empty string for null or blank input")
    void cleanDepartment_handlesNullAndBlank() {
        assertEquals("", IngestionServiceApp.cleanDepartment(null));
        assertEquals("", IngestionServiceApp.cleanDepartment(""));
        assertEquals("", IngestionServiceApp.cleanDepartment("   "));
    }

    @Test
    @DisplayName("cleanDepartment rejects placeholder values")
    void cleanDepartment_rejectsPlaceholderValues() {
        assertEquals("", IngestionServiceApp.cleanDepartment("N/A"));
        assertEquals("", IngestionServiceApp.cleanDepartment("NA"));
        assertEquals("", IngestionServiceApp.cleanDepartment("NULL"));
        assertEquals("", IngestionServiceApp.cleanDepartment("UNKNOWN"));
    }

    // ---------- toTitleCase ----------

    @Test
    @DisplayName("toTitleCase converts text to title case")
    void toTitleCase_convertsText() {
        assertEquals(
                "East Wing",
                IngestionServiceApp.toTitleCase("EAST WING"));
    }

    @Test
    @DisplayName("toTitleCase handles null and blank input")
    void toTitleCase_handlesNullAndBlank() {
        assertEquals("", IngestionServiceApp.toTitleCase(null));
        assertEquals("", IngestionServiceApp.toTitleCase(""));
        assertEquals("", IngestionServiceApp.toTitleCase("   "));
    }

    // ---------- cleanBeds ----------

    @Test
    @DisplayName("cleanBeds accepts valid numeric values")
    void cleanBeds_acceptsValidNumbers() {
        assertEquals(0, IngestionServiceApp.cleanBeds("0"));
        assertEquals(1, IngestionServiceApp.cleanBeds("1"));
        assertEquals(5, IngestionServiceApp.cleanBeds("5"));
        assertEquals(50, IngestionServiceApp.cleanBeds("50"));
        assertEquals(100, IngestionServiceApp.cleanBeds("100"));
    }

    @Test
    @DisplayName("cleanBeds trims whitespace")
    void cleanBeds_trimsWhitespace() {
        assertEquals(5, IngestionServiceApp.cleanBeds(" 5 "));
    }

    @Test
    @DisplayName("cleanBeds converts full to zero regardless of case")
    void cleanBeds_fullAnyCase() {
        assertEquals(0, IngestionServiceApp.cleanBeds("full"));
        assertEquals(0, IngestionServiceApp.cleanBeds(" FULL "));
        assertEquals(0, IngestionServiceApp.cleanBeds("FULL"));
    }

    @Test
    @DisplayName("cleanBeds rejects number words")
    void cleanBeds_rejectsNumberWords() {
        assertNull(IngestionServiceApp.cleanBeds("five"));
        assertNull(IngestionServiceApp.cleanBeds("Seven"));
        assertNull(IngestionServiceApp.cleanBeds("TWELVE"));
        assertNull(IngestionServiceApp.cleanBeds("twenty-one"));
    }

    @Test
    @DisplayName("cleanBeds rejects values outside the permitted range")
    void cleanBeds_rejectsOutOfRangeValues() {
        assertNull(IngestionServiceApp.cleanBeds("101"));
        assertNull(IngestionServiceApp.cleanBeds("-1"));
        assertNull(IngestionServiceApp.cleanBeds("1000"));
    }

    @Test
    @DisplayName("cleanBeds rejects decimals and invalid text")
    void cleanBeds_rejectsInvalidValues() {
        assertNull(IngestionServiceApp.cleanBeds("2.5"));
        assertNull(IngestionServiceApp.cleanBeds("abc"));
        assertNull(IngestionServiceApp.cleanBeds("five beds"));
    }

    @Test
    @DisplayName("cleanBeds returns null for null or blank input")
    void cleanBeds_handlesNullAndBlank() {
        assertNull(IngestionServiceApp.cleanBeds(null));
        assertNull(IngestionServiceApp.cleanBeds(""));
        assertNull(IngestionServiceApp.cleanBeds("   "));
    }

    // ---------- parseLine ----------

    @Test
    @DisplayName("parseLine parses a valid CSV row")
    void parseLine_parsesValidRow() {
        Ward ward = IngestionServiceApp.parseLine(
                "W-01,East Wing,Cardiology,5");

        assertNotNull(ward);
        assertEquals("W-01", ward.id());
        assertEquals("East Wing", ward.wing());
        assertEquals("Cardiology", ward.department());
        assertEquals(5, ward.bedsAvailable());
    }

    @Test
    @DisplayName("parseLine handles whitespace and invalid bed words")
    void parseLine_messySpacingAndWordNumber() {
        Ward ward = IngestionServiceApp.parseLine(
                "  w-02 , east wing , PEDIATRICS , five ");

        assertNotNull(ward);
        assertEquals("W-02", ward.id());
        assertEquals("East Wing", ward.wing());
        assertEquals("Paediatrics", ward.department());
        assertNull(ward.bedsAvailable());
    }

    @Test
    @DisplayName("parseLine converts full beds to zero")
    void parseLine_fullBeds() {
        Ward ward = IngestionServiceApp.parseLine(
                "W-03,South Wing,Radiology,full");

        assertNotNull(ward);
        assertEquals(0, ward.bedsAvailable());
    }

    @Test
    @DisplayName("parseLine returns null for null or blank input")
    void parseLine_handlesNullAndBlank() {
        assertNull(IngestionServiceApp.parseLine(null));
        assertNull(IngestionServiceApp.parseLine(""));
        assertNull(IngestionServiceApp.parseLine("   "));
    }

    @Test
    @DisplayName("parseLine rejects rows with the wrong number of fields")
    void parseLine_rejectsWrongNumberOfFields() {
        assertNull(IngestionServiceApp.parseLine("W-01,East Wing,Cardiology"));
        assertNull(IngestionServiceApp.parseLine(
                "W-01,East Wing,Cardiology,5,Extra"));
    }

    @Test
    @DisplayName("parseLine rejects rows with a missing or invalid ID")
    void parseLine_rejectsMissingId() {
        assertNull(IngestionServiceApp.parseLine(
                ",East Wing,Cardiology,5"));

        assertNull(IngestionServiceApp.parseLine(
                "UNKNOWN,East Wing,Cardiology,5"));
    }

    // ---------- mergeWards ----------

    @Test
    @DisplayName("mergeWards fills missing values from the incoming ward")
    void mergeWards_fillsGaps() {
        Ward existing = new Ward(
                "W-01",
                "",
                "Cardiology",
                null);

        Ward incoming = new Ward(
                "W-01",
                "East Wing",
                "Oncology",
                5);

        Ward merged = IngestionServiceApp.mergeWards(existing, incoming);

        assertEquals("W-01", merged.id());
        assertEquals("East Wing", merged.wing());
        assertEquals("Cardiology", merged.department());
        assertEquals(5, merged.bedsAvailable());
    }

    @Test
    @DisplayName("mergeWards returns incoming ward when existing is null")
    void mergeWards_handlesNullExisting() {
        Ward incoming = new Ward(
                "W-01",
                "East Wing",
                "Cardiology",
                5);

        assertEquals(
                incoming,
                IngestionServiceApp.mergeWards(null, incoming));
    }

    @Test
    @DisplayName("mergeWards returns existing ward when incoming is null")
    void mergeWards_handlesNullIncoming() {
        Ward existing = new Ward(
                "W-01",
                "East Wing",
                "Cardiology",
                5);

        assertEquals(
                existing,
                IngestionServiceApp.mergeWards(existing, null));
    }

    // ---------- mergeDuplicates ----------

    @Test
    @DisplayName("mergeDuplicates combines duplicate ward records")
    void mergeDuplicates_fillsGaps() {
        List<Ward> wards = new ArrayList<>();

        wards.add(new Ward("W-01", "", "Cardiology", null));
        wards.add(new Ward("W-01", "East Wing", "", 5));

        List<Ward> merged = IngestionServiceApp.mergeDuplicates(wards);

        assertEquals(1, merged.size());
        assertEquals("W-01", merged.get(0).id());
        assertEquals("East Wing", merged.get(0).wing());
        assertEquals("Cardiology", merged.get(0).department());
        assertEquals(5, merged.get(0).bedsAvailable());
    }

    @Test
    @DisplayName("mergeDuplicates preserves the order of unique ward IDs")
    void mergeDuplicates_preservesInsertionOrder() {
        List<Ward> wards = List.of(
                new Ward("W-02", "West Wing", "Surgery", 2),
                new Ward("W-01", "East Wing", "Cardiology", 5),
                new Ward("W-02", "West Wing", "Surgery", 3));

        List<Ward> merged = IngestionServiceApp.mergeDuplicates(wards);

        assertEquals(2, merged.size());
        assertEquals("W-02", merged.get(0).id());
        assertEquals("W-01", merged.get(1).id());
    }

    @Test
    @DisplayName("mergeDuplicates handles null or empty lists")
    void mergeDuplicates_handlesNullAndEmpty() {
        assertTrue(IngestionServiceApp.mergeDuplicates(null).isEmpty());
        assertTrue(IngestionServiceApp.mergeDuplicates(List.of()).isEmpty());
    }

    @Test
    @DisplayName("mergeDuplicates ignores null wards and invalid IDs")
    void mergeDuplicates_ignoresInvalidWards() {
        List<Ward> wards = new ArrayList<>();

        wards.add(null);
        wards.add(new Ward("", "East Wing", "Cardiology", 5));
        wards.add(new Ward("W-01", "East Wing", "Cardiology", 5));

        List<Ward> merged = IngestionServiceApp.mergeDuplicates(wards);

        assertEquals(1, merged.size());
        assertEquals("W-01", merged.get(0).id());
    }

    // ---------- loadWards ----------

    @Test
    @DisplayName("loadWards loads and merges the default CSV")
    void loadWards_loadsDefaultCsv() {
        List<Ward> wards = IngestionServiceApp.loadWards();

        assertNotNull(wards);
        assertEquals(17, wards.size());
    }

    @Test
    @DisplayName("loadWards merges duplicate W-05 records")
    void loadWards_mergesW05() {
        List<Ward> wards = IngestionServiceApp.loadWards();

        long count = wards.stream()
                .filter(ward -> ward.id().equals("W-05"))
                .count();

        assertEquals(1, count);
    }

    @Test
    @DisplayName("loadWards can load a specified CSV resource")
    void loadWards_loadsSpecifiedResource() {
        List<Ward> wards =
                IngestionServiceApp.loadWards("/wards-outdated.csv");

        assertEquals(17, wards.size());
    }
}
