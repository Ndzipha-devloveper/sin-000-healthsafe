# HealthSafe

## WeThinkCode_ Repository Verification

**Verification Code:** `WTC-FZCJL76X`

## Overview

HealthSafe is a small set of independent services for hospital ward status, emergency status tracking, and staffing schedules. It follows a development path from legacy CSV data cleanup, through synchronous REST communication, to asynchronous messaging and equipment-failure alerting.

**Domain entities:** wards, wings, and specialist departments.

Every class in this repository lives in the flat package `co.wethinkcode.healthsafe`.

The system is organized into these stages:

1. **Ingestion (required):** Clean the legacy `wards-outdated.csv` export using `IngestionServiceApp`.
2. **REST services (required):** Provide ward and department lookup, emergency-status tracking, and on-call scheduling through REST APIs.
3. **MQ decoupling (stretch):** Use the ActiveMQ topic `staffing-events-topic` for asynchronous staffing events.
4. **Equipment alerting (stretch):** Publish equipment-failure reports to `equipment-failure-queue` and consume and store them in `EquipmentAlertServiceApp`.

Stages 1–2 form the required core of the assignment. Stages 3–4 extend the system with asynchronous messaging.

## Services

| Service | Folder | Port | Role |
|---|---|---:|---|
| `IngestionServiceApp` | `ingestion-service/` | 7030 | Parses and cleans `wards-outdated.csv` and exposes ward records over REST. |
| `WardServiceApp` | `ward-service/` | 7031 | Provides ward information and accepts equipment-failure reports. |
| `AlertLevelServiceApp` | `alert-level-service/` | 7032 | Tracks the hospital emergency status from 0 to 8; 8 represents full Code Blue. |
| `StaffingServiceApp` | `staffing-service/` | 7033 | Provides on-call schedules based on ward and emergency status. |
| `EquipmentAlertServiceApp` | `equipment-alert-service/` | 7034 | Consumes equipment-failure messages from a queue and stores alerts. |
| ActiveMQ broker | `common/` | 61616, 8161 | Shared message broker. Port 61616 is for messaging; port 8161 is for the web console. |

## Implementation Status

The core REST services and ingestion functionality have been implemented. The staffing service provides ward schedules, and the equipment-failure reporting workflow has been tested end to end using ActiveMQ.

The equipment-failure workflow has been verified. Persistence across a service restart and every possible error scenario should be tested separately before claiming those cases are verified.

## Project Structure

```text
healthsafe/
├── README.md
├── .gitignore
├── ingestion-service/
│   ├── pom.xml
│   ├── README.md
│   └── src/main/
│       ├── java/co/wethinkcode/healthsafe/IngestionServiceApp.java
│       └── resources/wards-outdated.csv
├── ward-service/
├── alert-level-service/
├── staffing-service/
├── common/
│   ├── docker-compose.yml
│   └── README.md
└── equipment-alert-service/
```

Each service is an independent Maven project. There is no parent or aggregator `pom.xml` at the repository root.

## Requirements

- Java 17 or newer. Java 21 was used for the verified builds and tests.
- Maven 3.8 or newer.
- Docker with Docker Compose, for the ActiveMQ broker.
- `curl`, for the example HTTP requests.

## Build

Each service is an independent Maven project. There is no parent or aggregator `pom.xml` at the repository root. Build each service from its own directory.

### Select Java 21

Java 21 was used for the builds and tests during development. In every terminal where you build or run a service, set:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version
mvn -version
```

Confirm that Java and Maven are using Java 21.

### Build the services

Run each build command from the repository root, one at a time:

```bash
cd ingestion-service
mvn package
cd ..

cd ward-service
mvn package
cd ..

cd alert-level-service
mvn package
cd ..

cd staffing-service
mvn package
cd ..

cd equipment-alert-service
mvn package
cd ..
```

## Run the System

Start ActiveMQ and all five services. Keep each Java service running in its own terminal. Set the Java 21 environment variables in each terminal before launching a service.

### 1. Start ActiveMQ

In a terminal:

```bash
cd ~/personal_projects/Electives/sin-000-healthsafe/common
docker compose up -d
docker compose ps
```

The broker uses port `61616` for messaging and port `8161` for the web console.

### 2. Start the ingestion service

In a new terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd ~/personal_projects/Electives/sin-000-healthsafe/ingestion-service
java -jar target/ingestion-service.jar
```

### 3. Start the ward service

In a new terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd ~/personal_projects/Electives/sin-000-healthsafe/ward-service
java -jar target/ward-service.jar
```

### 4. Start the alert-level service

In a new terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd ~/personal_projects/Electives/sin-000-healthsafe/alert-level-service
java -jar target/alert-level-service.jar
```

### 5. Start the staffing service

In a new terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd ~/personal_projects/Electives/sin-000-healthsafe/staffing-service
java -jar target/staffing-service.jar
```

### 6. Start the equipment alert service

In a new terminal:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd ~/personal_projects/Electives/sin-000-healthsafe/equipment-alert-service
java -jar target/equipment-alert-service.jar
```

If a service reports that its port is already in use, check whether that service is already running before starting another instance. The commands above assume you have built each service with `mvn package` first.

## REST Endpoints and Manual Tests

Each service exposes a health endpoint.

```bash
curl -i http://localhost:7030/health
curl -i http://localhost:7031/health
curl -i http://localhost:7032/health
curl -i http://localhost:7033/health
curl -i http://localhost:7034/health
```

A running service should return HTTP 200 with `OK`.

### Ingestion service

Get the cleaned ward records:

```bash
curl -i http://localhost:7030/wards
```

Get one ward:

```bash
curl -i http://localhost:7030/wards/W-01
```

An unknown ward should return HTTP 404.

### Ward service

Get the wards:

```bash
curl -i http://localhost:7031/wards
```

Get one ward:

```bash
curl -i http://localhost:7031/wards/W-01
```

### Alert-level service

Get the current emergency status:

```bash
curl -i http://localhost:7032/alert-level
```

The response contains a `level` between 0 and 8, for example:

```json
{
  "level": 0
}
```

### Staffing service

Get the schedule for a ward:

```bash
curl -i http://localhost:7033/schedule/W-01
```

Example response observed during testing:

```json
{
  "wardId": "W-01",
  "department": "Cardiology",
  "alertLevel": 0,
  "alertMode": "NORMAL",
  "doctorsOnCall": 2
}
```

The returned schedule depends on the ward and current emergency status.

### Equipment alert service

Get stored equipment alerts:

```bash
curl -i http://localhost:7034/alerts
```

The endpoint returns the equipment alerts that have been consumed and stored by the service.

## End-to-End Equipment-Failure Test

The equipment-failure workflow was tested using the ward service, ActiveMQ, and the equipment alert service.

### 1. Submit an equipment-failure report

```bash
curl -i -X POST http://localhost:7031/wards/W-01/equipment-failures \
  -H "Content-Type: application/json" \
  -d '{
    "wardId": "W-01",
    "equipment": "Ventilator",
    "description": "Test: ventilator failure in Ward W-01",
    "reportedAt": "2026-10-26T14:15:00Z"
  }'
```

The request returned HTTP `202 Accepted`, confirming that the report was accepted for processing.

### 2. Retrieve the stored alert

```bash
curl -i http://localhost:7034/alerts
```

The response included the submitted report:

```json
[
  {
    "wardId": "W-01",
    "equipment": "Ventilator",
    "description": "Test: ventilator failure in Ward W-01",
    "reportedAt": "2026-10-26T14:15:00Z"
  }
]
```

### 3. Check the persisted record

The service stores alerts in JSON Lines format at:

```text
equipment-alert-service/data/equipment-alerts.jsonl
```

Inspect the file from the repository root:

```bash
cat equipment-alert-service/data/equipment-alerts.jsonl
```

### Verified result

The test confirmed that:

1. The ward service accepted the equipment-failure report.
2. The report was published to the `equipment-failure-queue`.
3. The equipment alert service consumed and stored the message.
4. The stored alert was returned by `GET /alerts`.

This verifies the equipment-failure workflow for the tested message. A separate restart test is still needed to verify persistence across service restarts.

## Integration Contracts

| From | To | Call or message | Purpose |
|---|---|---|---|
| Ward service | Ingestion service | `GET /wards` | Retrieve cleaned ward records. |
| Staffing service | Ward service | `GET /wards/{id}` | Validate that a ward exists before scheduling. |
| Staffing service | Alert-level service | `GET /alert-level` | Read the current emergency status. |
| Staffing service | ActiveMQ topic | `staffing-events-topic` | Broadcast staffing updates asynchronously. |
| Ward service | ActiveMQ topic | Subscribe to `staffing-events-topic` | Receive staffing updates asynchronously, where implemented. |
| Ward service | ActiveMQ queue | `equipment-failure-queue` | Publish equipment-failure reports. |
| Equipment alert service | ActiveMQ queue | Consume `equipment-failure-queue` | Consume and store equipment-failure alerts. |

## Messaging

The shared ActiveMQ broker configuration is in `common/`.

- `staffing-events-topic` is the topic intended for staffing update events.
- `equipment-failure-queue` is the queue used for equipment-failure reports.

Topics support broadcast-style event delivery, while queues distribute messages to consumers. The equipment-failure workflow was verified end to end. Consult the code and `common/README.md` for the current staffing-topic wiring and configuration.

## Testing

### Health checks

With the services running, check their health endpoints:

```bash
curl -i http://localhost:7030/health
curl -i http://localhost:7031/health
curl -i http://localhost:7032/health
curl -i http://localhost:7033/health
curl -i http://localhost:7034/health
```

Each running service returned HTTP 200 with `OK` during manual checks.

### Ingestion and ward checks

```bash
curl -i http://localhost:7030/wards
curl -i http://localhost:7030/wards/W-01
curl -i http://localhost:7031/wards
curl -i http://localhost:7031/wards/W-01
```

The ingestion endpoint returned cleaned ward records, including ward `W-01`. An unknown ward returned HTTP 404.

### Alert-level and staffing checks

```bash
curl -i http://localhost:7032/alert-level
curl -i http://localhost:7033/schedule/W-01
curl -i http://localhost:7033/schedule/W-02
```

The alert-level endpoint returned `{"level":0}` during the check. The staffing endpoint returned a schedule for `W-01`, including its department, alert level, alert mode, and doctors on call. The `W-02` schedule endpoint was also checked successfully.

### Equipment-failure end-to-end test

Submit a failure report through the ward service:

```bash
curl -i -X POST http://localhost:7031/wards/W-01/equipment-failures \
  -H "Content-Type: application/json" \
  -d '{
    "wardId": "W-01",
    "equipment": "Ventilator",
    "description": "Test: ventilator failure in Ward W-01",
    "reportedAt": "2026-10-26T14:15:00Z"
  }'
```

The tested request returned HTTP `202 Accepted`.

Then retrieve alerts from the equipment alert service:

```bash
curl -i http://localhost:7034/alerts
```

The response included the submitted Ventilator failure report, confirming that the ward service published the message, the equipment alert service consumed it, and the alert was stored and returned.

The alert was also found in the JSON Lines storage file:

```bash
cat equipment-alert-service/data/equipment-alerts.jsonl
```

The file path is relative to the equipment alert service's working directory when the service uses its default storage location. The command above assumes you run it from the repository root.

### Automated tests

The ward service test suite passed under Java 21 during development: 29 tests, 0 failures, and 0 errors.

To run tests for a service, change into that service's directory and run:

```bash
mvn test
```

The successful end-to-end test verifies message delivery and storage during that run. Persistence after restarting the equipment alert service has not yet been verified and should be tested separately.

## Troubleshooting

### Java or Mockito / Byte Buddy compatibility errors

Use Java 21 for the verified setup and confirm the selected version:

```bash
java -version
mvn -version
```

### Port already in use

The service may already be running. Check the existing process and use its current endpoint instead of launching a duplicate instance.

### ActiveMQ is unavailable

From `common/`, check the broker:

```bash
docker compose ps
docker compose logs
```

Start it if needed:

```bash
docker compose up -d
```

### Ward not found

Check the ward ID and confirm that the ingestion service returns the expected ward:

```bash
curl http://localhost:7030/wards
```

### Equipment alert does not appear

Confirm that ActiveMQ and both the ward and equipment alert services are running. Then check the response from the equipment-failure POST request and inspect the equipment alert service logs and stored JSONL file.

## Stop the System

Stop each Java service using `Ctrl+C` in its terminal.

To stop the ActiveMQ broker, run:

```bash
cd common
docker compose down
```

This stops and removes the Compose containers and network. It does not delete the source code.