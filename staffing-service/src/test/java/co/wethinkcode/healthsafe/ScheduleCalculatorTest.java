package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScheduleCalculatorTest {

    private static Ward ward(String department) {
        return new Ward("W-01", "East Wing", department, 3);
    }

    @ParameterizedTest(name = "level {0} adds {2} doctors and is {1}")
    @CsvSource({
            "0, NORMAL,    0",
            "2, NORMAL,    0",
            "3, ELEVATED,  1",
            "5, ELEVATED,  1",
            "6, CRITICAL,  2",
            "7, CRITICAL,  2",
            "8, CODE_BLUE, 3"
    })
    void alertLevelAddsExtraDoctorsAtEachBoundary(int level, String mode, int extra) {
        // Cardiology has 2 base doctors.
        Schedule schedule = ScheduleCalculator.calculate(ward("Cardiology"), level);

        assertEquals(mode, schedule.alertMode());
        assertEquals(2 + extra, schedule.doctorsOnCall());
        assertEquals(level, schedule.alertLevel());
    }

    @ParameterizedTest(name = "{0} has {1} base doctors")
    @CsvSource({
            "ICU, 3",
            "Maternity, 2",
            "Cardiology, 2",
            "Paediatrics, 2",
            "Oncology, 2",
            "Radiology, 1"
    })
    void eachDepartmentHasItsOwnBaseNumberOfDoctors(String department, int expected) {
        assertEquals(expected, ScheduleCalculator.calculate(ward(department), 0).doctorsOnCall());
    }

    @Test
    void missingDepartmentFallsBackToOneBaseDoctor() {
        Schedule schedule = ScheduleCalculator.calculate(ward(null), 0);

        assertEquals(1, schedule.doctorsOnCall());
        assertNull(schedule.department());
    }

    @Test
    void unknownDepartmentFallsBackToOneBaseDoctor() {
        assertEquals(1, ScheduleCalculator.calculate(ward("Dermatology"), 0).doctorsOnCall());
    }

    @Test
    void scheduleCarriesTheWardId() {
        assertEquals("W-01", ScheduleCalculator.calculate(ward("ICU"), 4).wardId());
    }

    @Test
    void rejectsLevelsOutsideZeroToEight() {
        assertThrows(IllegalArgumentException.class,
                () -> ScheduleCalculator.calculate(ward("ICU"), -1));
        assertThrows(IllegalArgumentException.class,
                () -> ScheduleCalculator.calculate(ward("ICU"), 9));
    }

    @Test
    void rejectsNullWard() {
        assertThrows(IllegalArgumentException.class,
                () -> ScheduleCalculator.calculate(null, 3));
    }
}