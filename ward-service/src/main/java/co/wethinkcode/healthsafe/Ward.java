package co.wethinkcode.healthsafe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * A ward as returned by ward-service (GET /wards/{id}).
 * Fields we don't need are ignored, so ward-service can add more later.
 */
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Represents a hospital ward and its details, including its ID, wing,
 * department, and number of available beds.
 * Used to map ward data received from the Ingestion Service.
 */
public class Ward {

    public String id;
    public String wing;
    public String department;
    public Integer bedsAvailable;

    public Ward() {
        // Required by Jackson for JSON deserialization.
    }

    public Ward(String id, String wing, String department, Integer bedsAvailable) {
        this.id = id;
        this.wing = wing;
        this.department = department;
        this.bedsAvailable = bedsAvailable;
    }

    public String getId() {
        return id;
    }

    public String getWing() {
        return wing;
    }

    public String getDepartment() {
        return department;
    }

    public Integer getBedsAvailable() {
        return bedsAvailable;
    }
}