package co.wethinkcode.healthsafe;

/**
 * The on-call schedule returned by GET /schedule/{wardId}.
 *
 * @param wardId        the ward this schedule is for
 * @param department    the ward's department (null if ingestion could not clean it)
 * @param alertLevel    the hospital Emergency Status (0-8) used to build it
 * @param alertMode     NORMAL, ELEVATED, CRITICAL or CODE_BLUE
 * @param doctorsOnCall how many doctors must be on call for this ward
 */
public record Schedule(
        String wardId,
        String department,
        int alertLevel,
        String alertMode,
        int doctorsOnCall
) {
}