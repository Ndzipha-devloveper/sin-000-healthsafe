package co.wethinkcode.healthsafe;

public class AlertLevel {
    public static final int MIN_ALERT_LEVEL = 0;
    public static final int MAX_ALERT_LEVEL = 8;

    //volatile int alertLevel = MIN_ALERT_LEVEL;
    private volatile int alertLevel = MIN_ALERT_LEVEL;

    public int getLevel() {
        return alertLevel;
    }
    
    /** @throws IllegalArgumentException if newLevel is outside 0 to 8 */
    public void setLevel(int newLevel) {
        if (newLevel < MIN_ALERT_LEVEL || newLevel > MAX_ALERT_LEVEL) {
            throw new IllegalArgumentException("Alert level must be between " + MIN_ALERT_LEVEL + " and " + MAX_ALERT_LEVEL);
        }
        alertLevel = newLevel;
    }
}
