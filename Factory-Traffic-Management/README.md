## Factory Traffic Management System

An event-driven traffic control system for internal factory roads built with Spring Boot, Docker, and MySQL. The application receives vehicle detection events, maintains traffic queues, controls traffic signals safely, and handles manual overrides, emergencies, and device failures.

## System Architecture & Services

- **Traffic Management Application**: Core logic for traffic state machine, vehicle queue scheduling, and REST APIs. Serves the frontend dashboard (4000).
- **Traffic Database**: Dedicated MySQL container for persisting junction configurations, vehicle queues, and audit logs (3307).
- **Docker Network**: Shared bridge network allowing containers to resolve each other by name.

## Prerequisites & Docker Network Setup

Before spinning up containers individually, ensure a shared Docker network named `traffic-net` exists so the containers can resolve each other by name:
`docker network create traffic-net`

## Configuration & Environment Setup

Below are the complete Docker run configurations for all services and infrastructure dependencies.

## 1. Databases

## traffic_db

- Container Name: traffic_db
- Image: mysql:latest
- Bind Ports: 3307:3306
- Bind Mounts: `[PATH_TO_YOUR_PROJECT]\Factory-Traffic-Management\db:/var/lib/mysql`
- Run Options: --network traffic-net

## Environment Variables:

| Variable | Value |
| --- | --- |
| MYSQL_DATABASE | traffic_db |
| MYSQL_ROOT_PASSWORD | |

## 2. Traffic Management Application

- Container Name: traffic_managment
- Dockerfile Path: Dockerfile
- Bind Ports: 4000:8080 *(Note: Tomcat runs internally on 8080)*
- Run Options: --network traffic-net

## Environment Variables:

| Variable | Value |
| --- | --- |
| SPRING_DATASOURCE_URL | jdbc:mysql://traffic_db:3306/traffic_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true |
| SPRING_DATASOURCE_USERNAME | root |
| SPRING_DATASOURCE_PASSWORD |  |
| SPRING_JPA_HIBERNATE_DDL_AUTO | update |
| SPRING_JPA_OPEN_IN_VIEW | false |

## Local Development Execution Order

To avoid service connection retries or boot-up failures, initialize and run the containers in the following order:

- 1. Create shared network: `docker network create traffic-net`
- 2. Database instances (traffic_db)
- 3. Spring Boot Application (traffic_managment)

---

## 3. API Documentation

The system exposes the following main REST endpoints:
*   `POST /api/sensor-events`: Accepts `VEHICLE_ARRIVED` and `VEHICLE_CLEARED` events from sensors.
*   `GET /api/junctions/A/status`: Returns current mode, phase, desired/actual signals, and queues.
*   `GET /api/junctions/A/history`: Returns the audit log of recent events.
*   `POST /api/junctions/A/commands`: Accepts `MANUAL_GREEN_REQUEST` and `RETURN_TO_AUTOMATIC`.
*   `POST /api/controller-events`: Simulates ACK messages from the physical hardware.
*   `POST /api/admin/set-controller/A?status=OFFLINE`: Simulates controller failure.

---

## 4. Traffic-Control Algorithm

The `TrafficScheduler` uses a weighted scoring algorithm to determine the next green phase when the light is `ALL_RED`:
*   **Queue Size:** +10 points per vehicle waiting.
*   **Vehicle Priority:** Emergency (+1000), Truck (+100), Forklift (+50), Employee (+10).
*   **Wait Time:** +0.5 points per second waiting.
*   **Starvation Protection:** If a vehicle waits longer than **120 seconds**, it receives a massive **+500 point boost**. This ensures low-priority vehicles are not blocked indefinitely.
*   **Hysteresis:** A 1.2x multiplier is applied to the currently active green side to prevent rapid "flapping".
*   **Empty Intersection Fallback:** If all queues are `0`, the system alternates normally (`North/South` -> `East/West`).

## 5. Traffic-State Transitions

*   **Safety First:** A green phase will never transition directly into a conflicting green phase.
*   **Sequence:** `GREEN (10s) -> YELLOW (4s) -> ALL_RED (2s) -> NEXT_GREEN`.
*   **Desired vs. Actual:** The system tracks both what the backend *wants* the light to be (`desiredSignals`) and what the physical controller *confirms* it to be (`actualSignals`).

## 6. Instructions for Demonstrating Main Scenarios

1.  **Normal Traffic:** Use the UI to simulate a `TRUCK` arriving at `NORTH`. Watch the queue update and the system serve the North side.
2.  **Emergency Preemption:** While `NORTH/SOUTH` is Green, simulate an `EMERGENCY` vehicle arriving at `EAST`. Watch the safe transition (`Yellow -> All Red -> East Green`).
3.  **Manual Override:** Select `WEST` and click "Manual GREEN request". The system will safely transition to West. Click "Return to Automatic" to resume normal scheduling.
4.  **Controller Failure:** Click "Simulate controller OFFLINE". The system will freeze safely and not execute any transitions.
5.  **Duplicate Events (Idempotency):** Send the exact same `eventId` to `/api/sensor-events` twice. The second event will be ignored.

---

## 7. Assumptions / Questions / Requirement Issues

- **Empty Intersection Behavior**: The PDF does not specify what happens when there are no vehicles. *Decision:* Implemented a fallback alternating cycle (North/South -> East/West) when all queues are empty so the system behaves like a standard traffic light rather than freezing.
- **Starvation Protection**: A vehicle waiting longer than 120 seconds receives a +500 point boost in the scheduler, guaranteeing it gets the green light next.
- **Out-of-Order Clear Events**: If a `VEHICLE_CLEARED` event arrives without a corresponding arrival, the system tolerates it. It logs an anomaly but keeps the queue at 0 (never negative).
- **Backend Restart Recovery**: The database stores `pendingCommandId` and `pendingCommandExpiresAt`. On restart, the system defaults to `ALL_RED` for safety and re-evaluates queues rather than blindly trusting the last requested state.
- **Manual Override Expiry**: Manual override automatically expires after 10 minutes and returns the junction to `AUTOMATIC` mode to prevent an admin from accidentally locking the junction indefinitely.

## 8. Major Architectural Decisions

- **Domain-Driven Design (DDD)**: Core traffic logic (`TrafficScheduler`, `Phase`) is strictly separated from HTTP controllers and DB entities. This ensures the engine is testable and portable.
- **State Machine & Safety**: Transitions are modeled explicitly (`GREEN -> YELLOW -> ALL_RED -> NEXT_GREEN`). Conflicting phases can *never* receive a green light simultaneously.
- **Optimistic Locking**: The `Junction` entity uses `@Version` to handle concurrent requests, preventing race conditions when multiple vehicles arrive at the exact same millisecond.
- **Controller Port/Adapter**: Physical controller communication is abstracted behind a `TrafficControllerPort` interface, allowing a simulated REST adapter to be swapped for MQTT later without changing core logic.

## 9. AI / Tool Usage

- AI-assisted coding tools were used to debug Docker/MySQL connection string issues, generate boilerplate code, and review the architecture for concurrency safety. All logic and design decisions were reviewed, understood, and modified by the candidate.
