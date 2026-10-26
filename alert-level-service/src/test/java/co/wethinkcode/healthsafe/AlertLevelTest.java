package co.wethinkcode.healthsafe;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AlertLevelTest {

    @Test
    void scaleRunsFromZeroToEight() {
        assertEquals(0, AlertLevel.MIN);
        assertEquals(8, AlertLevel.MAX);
    }

    @Test
    void startsAtZero() {
        assertEquals(0, new AlertLevel().get());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8})
    void acceptsEveryLevelFromZeroToEight(int level) {
        AlertLevel alert = new AlertLevel();
        alert.set(level);
        assertEquals(level, alert.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 9, -100, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsLevelsOutsideTheScale(int level) {
        AlertLevel alert = new AlertLevel();
        assertThrows(IllegalArgumentException.class, () -> alert.set(level));
    }

    @Test
    void aRejectedUpdateKeepsTheOldLevel() {
        AlertLevel alert = new AlertLevel();
        alert.set(5);

        assertThrows(IllegalArgumentException.class, () -> alert.set(9));

        assertEquals(5, alert.get());
    }

    @Test
    void canGoDownAsWellAsUp() {
        AlertLevel alert = new AlertLevel();
        alert.set(8);
        alert.set(0);
        assertEquals(0, alert.get());
    }
}