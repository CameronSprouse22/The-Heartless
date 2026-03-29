# Quickstart: Real-Time Voting & Game Page

**Feature**: 002-realtime-voting-page  
**Date**: 2026-03-28

---

## Prerequisites

- Java 17 (JDK installed and on PATH)
- Maven (via the frontend-maven-plugin or system install)
- Node.js 20+ (installed by frontend-maven-plugin, or manually for frontend dev)

## Build & Run

### Full Build (backend + frontend)

```bash
cd backend
mvn clean package -DskipTests
java -jar target/the-heartless-0.1.0-SNAPSHOT.jar
```

App runs at `http://localhost:8080`. Frontend is bundled into static resources.

### Frontend Development (hot reload)

Terminal 1 — Start backend:
```bash
cd backend
mvn spring-boot:run
```

Terminal 2 — Start Vite dev server:
```bash
cd backend/frontend
npm install
npm run dev
```

Frontend at `http://localhost:5173`, proxied to backend on 8080 for `/api` and `/ws`.

## Testing the Game Event Page

### 1. Create a test game with players

```bash
# Create game with test players
curl -X POST http://localhost:8080/api/games/test-setup
# Response: { "gameCode": "XYZW12", "players": [...] }
```

### 2. Create a game event

```bash
curl -X POST http://localhost:8080/api/games/XYZW12/event \
  -H "Content-Type: application/json" \
  -H "X-Player-Code: <vip-player-code>" \
  -d '{
    "title": "Choose a Destination",
    "prompt": "Where should the group travel next?",
    "listOfItems": ["Forest", "Cave", "River"],
    "singleAnswer": true,
    "showOthersSelections": true,
    "minNumberSelectedToSubmit": 1,
    "maxNumberSelectedToSubmit": 0,
    "endTime": 1743210000000,
    "inputString": false,
    "playersMustAgree": false
  }'
```

### 3. Open the game event page

Navigate to `http://localhost:5173/event/XYZW12` (or `http://localhost:8080/event/XYZW12` for production build).

The page will:
- Display the title and prompt
- Show toggle buttons for Forest, Cave, River
- Connect via WebSocket to receive real-time updates
- Show a countdown timer based on endTime

### 4. Verify real-time updates

Open two browser windows logged in as different players. When one player selects "Forest", the other should see the update within 2 seconds (if ShowOthersSelections is true).

### 5. Verify timeout resolution

Create an event with `endTime` set to 30 seconds from now. Wait for the timer to expire. Both players should see the EVENT_RESOLVED message.

## Key Files

| File | Purpose |
|---|---|
| `backend/src/main/java/com/heartless/model/GameEventConfig.java` | Event configuration POJO |
| `backend/src/main/java/com/heartless/model/PlayerSelection.java` | Per-player selection state |
| `backend/src/main/java/com/heartless/model/GameEventResult.java` | Resolved event outcome |
| `backend/src/main/java/com/heartless/service/GameEventService.java` | Event lifecycle management |
| `backend/src/main/java/com/heartless/controller/GameEventController.java` | REST + WebSocket endpoints |
| `backend/src/main/java/com/heartless/config/WebSocketConfig.java` | STOMP/SockJS configuration |
| `backend/frontend/src/pages/GameEventPage.jsx` | Main game event React page |
| `backend/frontend/src/services/websocket.js` | STOMP client wrapper |

## Running Tests

```bash
cd backend
mvn test
```

Tests for this feature:
- `GameEventConfigTest.java` — Config validation
- `PlayerSelectionTest.java` — State transitions
- `GameEventServiceTest.java` — Event lifecycle, agreement checks, timeout
