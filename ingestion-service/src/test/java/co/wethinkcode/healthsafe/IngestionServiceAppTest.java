package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import co.wethinkcode.healthsafe.IngestionServiceApp.Ward;

class IngestionServiceAppTest {

    @BeforeEach
    void announceTest(TestInfo testInfo) {
        System.out.println("-------- Running: " + testInfo.getDisplayName() + " --------");
    }

    // ---------- csvFileReader ----------

    @Test
    @DisplayName("csvFileReader returns the header plus all 18 data rows")
    void csvFileReader_returnsHeaderPlusAllRows() throws IOException {
        List<String> lines = IngestionServiceApp.csvFileReader("/wards-outdated.csv");
        System.out.println("Lines read from CSV: " + lines.size());

        assertEquals(19, lines.size());   // 1 header + 18 rows
    }

    // ---------- cleanId ----------

    @Test
    @DisplayName("cleanId trims spaces and makes the ID uppercase")
    void cleanId_trimsAndUppercases() {
        System.out.println("\"w-02\"  -> \"" + IngestionServiceApp.cleanId("w-02") + "\"");
        System.out.println("\"W-03 \" -> \"" + IngestionServiceApp.cleanId("W-03 ") + "\"");

        assertEquals("W-02", IngestionServiceApp.cleanId("w-02"));
        assertEquals("W-03", IngestionServiceApp.cleanId("W-03 "));
    }

    @Test
    @DisplayName("cleanId and cleanWing return null when given null")
    void cleanId_cleanWing_nullInput() {
        System.out.println("cleanId(null)   -> " + IngestionServiceApp.cleanId(null));
        System.out.println("cleanWing(null) -> " + IngestionServiceApp.cleanWing(null));

        assertNull(IngestionServiceApp.cleanId(null));
        assertNull(IngestionServiceApp.cleanWing(null));
    }

    // ---------- cleanWing ----------

    @Test
    @DisplayName("cleanWing trims spaces and capitalises each word")
    void cleanWing_titleCasesAndTrims() {
        System.out.println("\"east wing\" -> \"" + IngestionServiceApp.cleanWing("east wing") + "\"");
        System.out.println("\"North wing\" -> \"" + IngestionServiceApp.cleanWing("North wing") + "\"");
        System.out.println("\"East Wing \" -> \"" + IngestionServiceApp.cleanWing("East Wing ") + "\"");

        assertEquals("East Wing", IngestionServiceApp.cleanWing("east wing"));
        assertEquals("North Wing", IngestionServiceApp.cleanWing("North wing"));
        assertEquals("East Wing", IngestionServiceApp.cleanWing("East Wing "));
    }

    @Test
    @DisplayName("cleanWing squashes extra spaces between words")
    void cleanWing_extraInnerSpaces() {
        System.out.println("\"east    wing\" -> \"" + IngestionServiceApp.cleanWing("east    wing") + "\"");

        assertEquals("East Wing", IngestionServiceApp.cleanWing("east    wing"));
    }

    @Test
    @DisplayName("cleanWing returns null when the wing is empty or only spaces")
    void cleanWing_blankBecomesNull() {
        System.out.println("\"\"    -> " + IngestionServiceApp.cleanWing(""));
        System.out.println("\"   \" -> " + IngestionServiceApp.cleanWing("   "));

        assertNull(IngestionServiceApp.cleanWing(""));
        assertNull(IngestionServiceApp.cleanWing("   "));
    }

    // ---------- cleanDepartment ----------

    @Test
    @DisplayName("cleanDepartment fixes casing, spacing and the Pediatrics spelling")
    void cleanDepartment_normalisesCasingAndSpelling() {
        System.out.println("\"PAEDIATRICS\" -> " + IngestionServiceApp.cleanDepartment("PAEDIATRICS"));
        System.out.println("\"Pediatrics\"  -> " + IngestionServiceApp.cleanDepartment("Pediatrics"));
        System.out.println("\"oncology \"   -> " + IngestionServiceApp.cleanDepartment("oncology "));
        System.out.println("\"ICU\"         -> " + IngestionServiceApp.cleanDepartment("ICU"));

        assertEquals("Cardiology", IngestionServiceApp.cleanDepartment("cardiology"));
        assertEquals("Paediatrics", IngestionServiceApp.cleanDepartment("PAEDIATRICS"));
        assertEquals("Paediatrics", IngestionServiceApp.cleanDepartment("Pediatrics"));
        assertEquals("Oncology", IngestionServiceApp.cleanDepartment("oncology "));
        assertEquals("ICU", IngestionServiceApp.cleanDepartment("ICU"));
    }

    @Test
    @DisplayName("cleanDepartment returns null for unknown or empty departments")
    void cleanDepartment_unknownOrBlankBecomesNull() {
        System.out.println("\"Dentistry\" -> " + IngestionServiceApp.cleanDepartment("Dentistry"));
        System.out.println("null         -> " + IngestionServiceApp.cleanDepartment(null));

        assertNull(IngestionServiceApp.cleanDepartment("Dentistry"));
        assertNull(IngestionServiceApp.cleanDepartment("   "));
        assertNull(IngestionServiceApp.cleanDepartment(null));
    }

    // ---------- cleanBeds ----------

    @Test
    @DisplayName("cleanBeds reads normal numbers, even with spaces around them")
    void cleanBeds_parsesValidNumbers() {
        System.out.println("\" 4 \" -> " + IngestionServiceApp.cleanBeds(" 4 "));

        assertEquals(3, IngestionServiceApp.cleanBeds("3"));
        assertEquals(0, IngestionServiceApp.cleanBeds("0"));
        assertEquals(4, IngestionServiceApp.cleanBeds(" 4 "));
    }

    @Test
    @DisplayName("cleanBeds accepts 100 beds but rejects 101")
    void cleanBeds_upperBoundary() {
        System.out.println("\"100\" -> " + IngestionServiceApp.cleanBeds("100"));
        System.out.println("\"101\" -> " + IngestionServiceApp.cleanBeds("101"));

        assertEquals(100, IngestionServiceApp.cleanBeds("100"));
        assertNull(IngestionServiceApp.cleanBeds("101"));
    }

    @Test
    @DisplayName("cleanBeds turns number words into numbers and 'full' into 0")
    void cleanBeds_convertsWordsToNumbers() {
        System.out.println("\"five\" -> " + IngestionServiceApp.cleanBeds("five"));
        System.out.println("\"full\" -> " + IngestionServiceApp.cleanBeds("full"));

        assertEquals(5, IngestionServiceApp.cleanBeds("five"));
        assertEquals(0, IngestionServiceApp.cleanBeds("full"));
    }

    @Test
    @DisplayName("cleanBeds treats 'FULL' in any casing or spacing as 0")
    void cleanBeds_fullAnyCase() {
        System.out.println("\" FULL \" -> " + IngestionServiceApp.cleanBeds(" FULL "));

        assertEquals(0, IngestionServiceApp.cleanBeds(" FULL "));
    }

    @Test
    @DisplayName("cleanBeds understands bigger number words in any casing")
    void cleanBeds_biggerWordsAnyCase() {
        System.out.println("\"Seven\"      -> " + IngestionServiceApp.cleanBeds("Seven"));
        System.out.println("\"TWELVE\"     -> " + IngestionServiceApp.cleanBeds("TWELVE"));
        System.out.println("\"twenty-one\" -> " + IngestionServiceApp.cleanBeds("twenty-one"));

        assertEquals(7, IngestionServiceApp.cleanBeds("Seven"));
        assertEquals(12, IngestionServiceApp.cleanBeds("TWELVE"));
        assertEquals(21, IngestionServiceApp.cleanBeds("twenty-one"));
    }

    @Test
    @DisplayName("cleanBeds returns null for text, negatives, years and empty values")
    void cleanBeds_invalidBecomesNull() {
        System.out.println("\"N/A\"  -> " + IngestionServiceApp.cleanBeds("N/A"));
        System.out.println("\"-1\"   -> " + IngestionServiceApp.cleanBeds("-1"));
        System.out.println("\"2023\" -> " + IngestionServiceApp.cleanBeds("2023"));

        assertNull(IngestionServiceApp.cleanBeds("N/A"));
        assertNull(IngestionServiceApp.cleanBeds("TBD"));
        assertNull(IngestionServiceApp.cleanBeds("unknown"));
        assertNull(IngestionServiceApp.cleanBeds("-1"));
        assertNull(IngestionServiceApp.cleanBeds("-2"));
        assertNull(IngestionServiceApp.cleanBeds("2023"));
        assertNull(IngestionServiceApp.cleanBeds(""));
        assertNull(IngestionServiceApp.cleanBeds(null));
    }

    @Test
    @DisplayName("cleanBeds rejects words that are not just a number")
    void cleanBeds_rejectsExtraText() {
        System.out.println("\"five apples\"          -> " + IngestionServiceApp.cleanBeds("five apples"));
        System.out.println("\"one point five\"       -> " + IngestionServiceApp.cleanBeds("one point five"));
        System.out.println("\"one hundred and one\"  -> " + IngestionServiceApp.cleanBeds("one hundred and one"));

        assertNull(IngestionServiceApp.cleanBeds("five apples"));
        assertNull(IngestionServiceApp.cleanBeds("one point five"));
        assertNull(IngestionServiceApp.cleanBeds("one hundred and one"));
    }

    @Test
    @DisplayName("cleanBeds rejects decimals written as digits")
    void cleanBeds_rejectsDigitDecimals() {
        System.out.println("\"3.5\" -> " + IngestionServiceApp.cleanBeds("3.5"));

        assertNull(IngestionServiceApp.cleanBeds("3.5"));
    }

    // ---------- parseLine ----------

    @Test
    @DisplayName("parseLine cleans all four fields of a normal row")
    void parseLine_normalRow() {
        Ward ward = IngestionServiceApp.parseLine("w-17,North wing,oncology,6");
        System.out.println("Parsed: " + ward);

        assertEquals(new Ward("W-17", "North Wing", "Oncology", 6), ward);
    }

    @Test
    @DisplayName("parseLine copes with messy spacing and a number written as a word")
    void parseLine_messySpacingAndWordNumber() {
        Ward ward = IngestionServiceApp.parseLine("  w-02 , east wing , PEDIATRICS , five ");
        System.out.println("Parsed: " + ward);

        assertEquals(new Ward("W-02", "East Wing", "Paediatrics", 5), ward);
    }

    @Test
    @DisplayName("parseLine leaves the wing as null when it is empty")
    void parseLine_missingWing() {
        Ward ward = IngestionServiceApp.parseLine("W-08,,cardiology,3");
        System.out.println("Parsed: " + ward);

        assertEquals(new Ward("W-08", null, "Cardiology", 3), ward);
    }

    @Test
    @DisplayName("parseLine turns an unknown department and a bad bed count into null")
    void parseLine_unknownDepartmentAndBadBeds() {
        Ward ward = IngestionServiceApp.parseLine("W-09,west wing,Dentistry,N/A");
        System.out.println("Parsed: " + ward);

        assertEquals(new Ward("W-09", "West Wing", null, null), ward);
    }
}