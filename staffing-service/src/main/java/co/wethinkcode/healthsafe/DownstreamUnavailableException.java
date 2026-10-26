package co.wethinkcode.healthsafe;

/**
 * Thrown when ward-service or alert-level-service cannot give us a usable answer:
 * not running, timed out, returned a server error, or returned a bad body.
 */
public class DownstreamUnavailableException extends RuntimeException {

    public DownstreamUnavailableException(String service, String detail) {
        super(service + " " + detail);
    }

    public DownstreamUnavailableException(String service, String detail, Throwable cause) {
        super(service + " " + detail, cause);
    }
}