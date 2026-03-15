# Quickstart: Traitors Game Core

**Branch**: `001-traitors-game-core` | **Date**: 2026-03-13

---

## Prerequisites

- **Java 17+** (JDK) — verify with `java -version`
- **Maven 3.8+** — verify with `mvn -version`
- **Node.js 20+** (optional for frontend dev mode) — verify with `node -v`

> Note: The `frontend-maven-plugin` downloads its own Node.js for the Maven build, so Node.js is only required if you want to run the React dev server separately.

---

## Project Setup

### Clone and checkout feature branch
```bash
git clone <repository-url>
cd The_Heartless
git checkout 001-traitors-game-core
```

### Build everything (backend + frontend)
```bash
cd backend
mvn clean package
```
This will:
1. Compile Java sources
2. Run JUnit 5 tests
3. Install Node.js (via frontend-maven-plugin)
4. Run `npm install` and `npm run build` in `frontend/`
5. Copy React build output to `static/` resources
6. Package a single executable JAR

### Run the application
```bash
java -jar target/the-heartless-0.1.0-SNAPSHOT.jar
```

The server starts on `http://localhost:8080`. The React SPA is served at the root URL.

---

## Development Workflow

### Backend only (Spring Boot with hot reload)
```bash
cd backend
mvn spring-boot:run
```
API available at `http://localhost:8080/api/`

### Frontend only (React with Vite hot reload)
```bash
cd backend/frontend
npm install    # first time only
npm run dev
```
Vite dev server runs on `http://localhost:5173` and proxies `/api/*` to `http://localhost:8080`.

### Run tests

**Backend tests**:
```bash
cd backend
mvn test
```

**Frontend tests**:
```bash
cd backend/frontend
npm test
```

---

## Verifying the Setup

### 1. Create a game
```bash
curl -X POST http://localhost:8080/api/games \
  -H "Content-Type: application/json" \
  -d '{"playerName": "VIP"}'
```
Expected: `201 Created` with `gameCode` and `playerCode`

### 2. Invite a player
```bash
curl -X POST http://localhost:8080/api/games/{gameCode}/invite \
  -H "Content-Type: application/json" \
  -H "X-Player-Code: {vipPlayerCode}" \
  -d '{"name": "Player2", "contact": "player2@test.com"}'
```
Expected: `201 Created` with invitation details

### 3. Join a game
```bash
curl -X POST http://localhost:8080/api/join/{gameCode} \
  -H "Content-Type: application/json" \
  -d '{"name": "Player2", "contact": "player2@test.com"}'
```
Expected: `200 OK` with `playerCode`

### 4. Check game status
```bash
curl http://localhost:8080/api/games/{gameCode} \
  -H "X-Player-Code: {playerCode}"
```
Expected: `200 OK` with game state showing players

---

## Configuration

Application configuration is in `backend/src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Email (stub by default — configure for real email)
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=
spring.mail.password=

# SMS (stub by default — configure with Twilio credentials)
twilio.account-sid=
twilio.auth-token=
twilio.phone-number=

# Game defaults
game.min-players=4
game.max-players=52
game.traitor-ratio=5
game.chat-poll-interval-ms=3000
```

---

## Project Structure Overview

```
backend/
├── pom.xml                              # Maven build with Spring Boot + frontend-maven-plugin
├── frontend/                            # React (Vite) application
│   ├── src/pages/                       # LoginPage, LobbyPage, MenuPage, chat pages, vote pages
│   └── src/components/                  # ChatWindow, PlayerList, VoteCard, GameStatusBar
└── src/main/java/com/heartless/
    ├── model/                           # Pure Java POJOs (Player, Card, GameObject, etc.)
    ├── model/enums/                     # GameStatusEnum, CardSuit, CardNumber, etc.
    ├── gamethread/                      # GameThread, GameCriteriaObject (pure Java)
    ├── event/                           # EventObjectInterface + all event implementations (pure Java)
    ├── operation/condition/             # ConditionInterface + implementations (pure Java)
    ├── operation/channel/               # ChannelObjectInterface + implementations (pure Java)
    ├── operation/item/                  # ItemsInterface + implementations (pure Java)
    ├── service/                         # Spring @Service classes (GameService, ChatService, etc.)
    ├── controller/                      # Spring @RestController classes (thin REST wrappers)
    └── messaging/                       # MessageSenderInterface + Email/SMS implementations
```

**Key architectural rule**: Packages `model/`, `gamethread/`, `event/`, and `operation/` contain **zero Spring imports**. All game logic is testable with plain JUnit — no Spring context required.
