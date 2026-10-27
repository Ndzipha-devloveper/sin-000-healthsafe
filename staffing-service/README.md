# StaffingServiceApp

## Overview

Gives the on-call schedule for doctors, based on a ward and the hospital alert level.

Part of the [HealthSafe](../README.md) project. This is an independent Maven module with no parent POM.

**How it works.** For each request, this service calls two other services over HTTP:

1. `ward-service` (`../ward-service`) to check that the ward exists.
2. `alert-level-service` (`../alert-level-service`) to read the current Emergency Status (0–8).

Then it works out how many doctors must be on call. See the [Integration contracts](../README.md#integration-contracts) in the root README for the endpoint shapes.

**Messaging (Stage 3).** The staffing service publishes staffing events to the ActiveMQ topic `staffing-events-topic`. The ward service subscribes to the topic and updates its local staffing store when events are received. The broker URL and topic name are configured in `co.wethinkcode.healthsafe.mq.MqConfig`. See [`../common/`](../common) for shared messaging documentation.

The topic subscriber is non-durable, so events published while the ward service is disconnected may be missed.

## Requirements

- Java 17 or newer
- Maven 3.8 or newer

## Endpoints

| Method | Path | What it does |
|---|---|---|
| GET | `/health` | Returns `OK` |
| GET | `/schedule/{wardId}` | Returns the on-call schedule for one ward |

Example:

```bash
curl http://localhost:7033/schedule/W-01
```

```json
{
  "wardId": "W-01",
  "department": "Cardiology",
  "alertLevel": 4,
  "alertMode": "ELEVATED",
  "doctorsOnCall": 3
}
```

### Status codes

| Code | When |
|---|---|
| 200 | The schedule was built |
| 400 | The ward ID is invalid (it must contain 1–20 letters, digits or dashes) |
| 404 | `ward-service` says the ward does not exist. `alert-level-service` is not called. |
| 503 | `ward-service` or `alert-level-service` is down, too slow, returned an error, or sent invalid data |

For 503 responses, calls to downstream services use timeouts: 2 seconds to connect, 3 seconds to read, and 5 seconds in total. A slow service cannot freeze this one.

## Schedule rule (assumption)

The task does not specify how to size a schedule, so this implementation uses the following rule:

```text
doctors on call = base doctors for the department + extra doctors for the alert level
```

| Department | Base doctors |
|---|---:|
| ICU | 3 |
| Maternity, Cardiology, Paediatrics, Oncology | 2 |
| Radiology | 1 |
| Unknown or missing department | 1 |

| Alert level | Mode | Extra doctors |
|---|---|---:|
| 0–2 | NORMAL | +0 |
| 3–5 | ELEVATED | +1 |
| 6–7 | CRITICAL | +2 |
| 8 | CODE_BLUE | +3 |

Example: a Cardiology ward (2 base doctors) at alert level 4 (+1) needs 3 doctors.

### Other choices I made

- Ward IDs are trimmed and converted to uppercase, so `w-01` finds `W-01`.
- The ward is checked first. If it does not exist, the alert level is never requested.
- Nothing is cached. Every request reads the latest ward and alert level.
- A ward with an unknown department (which the ingestion service could not clean) still gets a schedule, using 1 base doctor.

## Project structure

```text
staffing-service/
├── pom.xml
└── src/
    ├── main/java/co/wethinkcode/healthsafe/
    │   ├── StaffingServiceApp.java              routes and status codes
    │   ├── WardClient.java                      HTTP call to ward-service
    │   ├── AlertLevelClient.java                HTTP call to alert-level-service
    │   ├── ScheduleCalculator.java              the schedule rule
    │   ├── Ward.java, Schedule.java             data records
    │   ├── WardNotFoundException.java           ward-service answered 404
    │   ├── DownstreamUnavailableException.java  another service is down or slow
    │   ├── HttpClients.java                     shared timeouts
    │   ├── StaffingEventPublisher.java           publishes staffing events
    │   └── mq/MqConfig.java                     broker settings
    └── test/java/co/wethinkcode/healthsafe/
        ├── ScheduleCalculatorTest.java
        ├── WardClientTest.java
        ├── AlertLevelClientTest.java
        └── StaffingServiceAppTest.java
```

## Build

```bash
cd staffing-service
mvn package
```

## Run

This service depends on three other services. Start each service in its own terminal, running commands from the project root (`sin-000-healthsafe`).

| Order | Service | Port | Why staffing needs it |
|---|---|---:|---|
| 1 | `ingestion-service` | 7030 | Provides clean ward data to `ward-service` |
| 2 | `ward-service` | 7031 | Tells staffing whether a ward exists |
| 3 | `alert-level-service` | 7032 | Provides the current alert level |
| 4 | `staffing-service` | 7033 | This service |

**Terminal 1: ingestion-service**

```bash
cd ingestion-service
mvn package -DskipTests
java -jar target/ingestion-service.jar
```

**Terminal 2: ward-service**

```bash
cd ward-service
mvn package -DskipTests
java -jar target/ward-service.jar
```

**Terminal 3: alert-level-service**

```bash
cd alert-level-service
mvn package -DskipTests
java -jar target/alert-level-service.jar
```

**Terminal 4: staffing-service**

```bash
cd staffing-service
mvn package -DskipTests
java -jar target/staffing-service.jar
```

Keep all four terminals open. `-DskipTests` only skips the tests during packaging.

### Check that everything is running

Open a fifth terminal. Each command should return `OK`:

```bash
curl http://localhost:7030/health
curl http://localhost:7031/health
curl http://localhost:7032/health
curl http://localhost:7033/health
```

If a command says `Connection refused`, that service is not running. Check its terminal for an error message.

## Test

Run the automated tests:

```bash
cd staffing-service
mvn test
```

The tests cover the schedule rule, downstream HTTP clients, unavailable or slow services, and endpoint status codes.

> On very new Java versions, such as Java 26, Mockito may need `-Dnet.bytebuddy.experimental=true`. Check the Surefire configuration in `pom.xml` to confirm this setting is present.

### Manual check

With all four services running:

```bash
curl -i http://localhost:7033/schedule/W-01
curl -i http://localhost:7033/schedule/W-99
curl -i http://localhost:7033/schedule/bad_id
```

Expected results:

- `W-01`: `200` with a schedule, assuming the ward exists.
- `W-99`: `404` if the ward does not exist.
- `bad_id`: `400` because the ID contains an underscore.

Now test what happens when a service is down:

1. Stop `alert-level-service` by pressing `Ctrl+C` in its terminal.
2. Run `curl -i http://localhost:7033/schedule/W-01` again.
3. You should receive `503` because staffing cannot read the alert level.
4. Restart `alert-level-service` and retry the request.

### Common problems

| Problem | What it means | What to do |
|---|---|---|
| `Connection refused` | A required service is not running | Start the service |
| `Address already in use` | Another process is using the port | Stop the old process or find it with `lsof -i :7033` |
| `503` from `/schedule/...` | `ward-service` or `alert-level-service` is unavailable or slow | Check that both services are running |
| Staffing events are not received | The subscriber may be disconnected or using a different topic or broker | Check `MqConfig`, broker availability, and subscriber startup |