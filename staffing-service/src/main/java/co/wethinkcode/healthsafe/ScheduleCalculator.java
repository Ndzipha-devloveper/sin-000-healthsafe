package co.wethinkcode.healthsafe;

import java.util.Map;

/**
 * Works out how many doctors must be on call for a ward.
 *
 * Assumed rule (the task does not define one):
 *   doctors on call = base doctors for the ward's department + extra doctors for the alert level
 *
 * Base doctors: ICU 3, Maternity 2, Cardiology 2, Paediatrics 2, Oncology 2,
 *               Radiology 1, unknown or missing department 1.
 *
 * Extra doctors by alert level:
 *   0-2 NORMAL    +0
 *   3-5 ELEVATED  +1
 *   6-7 CRITICAL  +2
 *   8   CODE_BLUE +3
 */
public final class ScheduleCalculator {

    public static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 8;

    private static final int DEFAULT_BASE_DOCTORS = 1;

    private static final Map<String, Integer> BASE_DOCTORS = Map.of(
            "ICU", 3,
            "Maternity", 2,
            "Cardiology", 2,
            "Paediatrics", 2,
            "Oncology", 2,
            "Radiology", 1
    );

    private ScheduleCalculator() {
    }

    /**
     * @throws IllegalArgumentException if the ward is null or the level is outside 0-8
     */
    public static Schedule calculate(Ward ward, int alertLevel) {
        if (ward == null) {
            throw new IllegalArgumentException("Ward must not be null");
        }
        if (alertLevel < MIN_LEVEL || alertLevel > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Alert level must be between " + MIN_LEVEL + " and " + MAX_LEVEL);
        }

        int base = ward.department() == null
                ? DEFAULT_BASE_DOCTORS
                : BASE_DOCTORS.getOrDefault(ward.department(), DEFAULT_BASE_DOCTORS);

        return new Schedule(
                ward.id(),
                ward.department(),
                alertLevel,
                modeFor(alertLevel),
                base + extraDoctors(alertLevel)
        );
    }

    private static int extraDoctors(int level) {
        if (level <= 2) {
            return 0;
        }
        if (level <= 5) {
            return 1;
        }
        if (level <= 7) {
            return 2;
        }
        return 3;
    }

    private static String modeFor(int level) {
        if (level <= 2) {
            return "NORMAL";
        }
        if (level <= 5) {
            return "ELEVATED";
        }
        if (level <= 7) {
            return "CRITICAL";
        }
        return "CODE_BLUE";
    }
}