package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AlertLevelTest {

    @Test
    void scaleRunsFromZeroToEight() {
        assertEquals(0, AlertLevel.MIN_ALERT_LEVEL);
        assertEquals(8, AlertLevel.MAX_ALERT_LEVEL);
    }

    @Test
    void startsAtZero() {
        assertEquals(0, new AlertLevel().getLevel());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void acceptsEveryLevelFromZeroToEight(int level) {
        AlertLevel alert = new AlertLevel();
        alert.setLevel(level);
        assertEquals(level, alert.getLevel());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 9, -100, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsLevelsOutsideTheScale(int level) {
        AlertLevel alert = new AlertLevel();
        assertThrows(IllegalArgumentException.class, () -> alert.setLevel(level));
    }

    @Test
    void aRejectedUpdateKeepsTheOldLevel() {
        AlertLevel alert = new AlertLevel();
        alert.setLevel(5);

        assertThrows(IllegalArgumentException.class, () -> alert.setLevel(9));

        assertEquals(5, alert.getLevel());
    }

    @Test
    void canGoDownAsWellAsUp() {
        AlertLevel alert = new AlertLevel();
        alert.setLevel(8);
        alert.setLevel(0);
        assertEquals(0, alert.getLevel());
    }
}