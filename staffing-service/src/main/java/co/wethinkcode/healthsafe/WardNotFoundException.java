package co.wethinkcode.healthsafe;

/** Thrown when ward-service says the ward does not exist (HTTP 404). */
public class WardNotFoundException extends RuntimeException {

    public WardNotFoundException(String wardId) {
        super("Ward not found: " + wardId);
    }
}