# Implementation Plan: Real-Time Voting & Game Page

**Branch**: `002-realtime-voting-page` | **Date**: 2026-03-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-realtime-voting-page/spec.md`

## Summary

Build a configurable, real-time voting and game page that supports toggle-button selection, text input, submit validation, player agreement flows, and automatic timeout resolution. The backend introduces WebSocket (STOMP over SockJS) to push selection updates in real-time, a new `GameEventConfig` model to capture all configuration options, and a `GameEventService` to manage per-player selection state and event lifecycle. The frontend adds a `GameEventPage` React component connected via a STOMP client, rendering the title, prompt, toggle buttons, optional text input, countdown timer, and other player statuses.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.2.3)  
**Primary Dependencies**: spring-boot-starter-web, spring-boot-starter-websocket (NEW), React 18, react-router-dom 6, @stomp/stompjs + sockjs-client (NEW frontend deps)  
**Storage**: In-memory ConcurrentHashMap (existing GameStore pattern)  
**Testing**: JUnit 5 (Jupiter) + Mockito (backend), manual browser testing (frontend)  
**Target Platform**: Web browser (mobile-first), Spring Boot embedded Tomcat  
**Project Type**: Web application (Spring Boot backend + React/Vite frontend)  
**Performance Goals**: Selection persistence <1s, real-time updates <2s latency  
**Constraints**: Must work on mobile browsers, no external database, player auth via existing X-Player-Code header  
**Scale/Scope**: Up to 52 concurrent players per game, 1 active event per game at a time

## Constitution Check (Pre-Design)

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Testability First**: **PASS** — `GameEventConfig` is a pure POJO with no dependencies. `GameEventService` methods (save selection, submit, check agreement, resolve) accept explicit parameters and return clear results. All business logic is unit-testable without a running WebSocket server. WebSocket controller is a thin delegation layer only.
- **Method Simplicity**: **PASS** — Selection saving, submit validation, agreement checking, and timeout resolution are separate methods each with single responsibility. The most complex method (agreement check) inspects submitted selections for equality — straightforward comparison logic, no deep nesting.
- **Separation of Concerns**: **PASS** — All game event logic lives in `GameEventService` (pure Java, no UI). The WebSocket controller only receives messages and delegates to the service. React components render state received via STOMP subscription without containing any game logic.
- **Explicit Over Implicit**: **PASS** — `GameEventConfig` makes all 10 configuration options explicit named fields. Methods like `saveSelection()`, `submitSelections()`, `checkAgreement()`, `resolveOnTimeout()` reveal intent. Return types are specific (e.g., event result object, not generic Object).

No principle violations identified.

## Constitution Check (Post-Design)

*Re-evaluated after Phase 1 design artifacts (data-model.md, contracts/) were generated.*

- **Testability First**: **PASS** — All entities are POJOs with no hidden dependencies. `GameEventService` depends only on injectable collaborators (`SimpMessagingTemplate`, `ScheduledExecutorService`, `GameStore`), all mockable. State transitions in `PlayerSelection` are testable via direct method calls. No static state.
- **Method Simplicity**: **PASS** — Each service method handles one concern: `saveSelection()` ~10 lines, `submitSelections()` ~15 lines, `checkAgreement()` ~15 lines, `resolveOnTimeout()` ~10 lines. No method exceeds 30 lines. Cyclomatic complexity ≤5 for all methods.
- **Separation of Concerns**: **PASS** — Business logic isolated in `GameEventService`. REST endpoints in `GameEventController` are thin routing. WebSocket message handlers are thin delegation. React components (`GameEventPage`, `ToggleButton`, `CountdownTimer`, `PlayerStatusList`) render state only. `websocket.js` handles connection lifecycle only.
- **Explicit Over Implicit**: **PASS** — 12 explicitly named fields in `GameEventConfig`. `SubmissionStatus` and `ResolutionType` are named enums. STOMP message `type` field explicitly distinguishes 4 message kinds. Error codes are explicit constants documented in contracts.

No violations. Gate passed.

## Project Structure

### Documentation (this feature)

```text
specs/002-realtime-voting-page/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (WebSocket message contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/heartless/
│   ├── model/
│   │   ├── GameEventConfig.java          # NEW — event configuration POJO
│   │   ├── PlayerSelection.java          # NEW — per-player selection state
│   │   └── GameEventResult.java          # NEW — resolved event outcome
│   ├── service/
│   │   └── GameEventService.java         # NEW — event lifecycle management
│   ├── controller/
│   │   └── GameEventController.java      # NEW — REST + WebSocket endpoints
│   └── config/
│       └── WebSocketConfig.java          # NEW — STOMP/SockJS configuration
├── src/test/java/com/heartless/
│   ├── model/
│   │   ├── GameEventConfigTest.java      # NEW
│   │   └── PlayerSelectionTest.java      # NEW
│   └── service/
│       └── GameEventServiceTest.java     # NEW
└── frontend/
    ├── src/
    │   ├── pages/
    │   │   └── GameEventPage.jsx         # NEW — main voting/game page
    │   ├── components/
    │   │   ├── ToggleButton.jsx          # NEW — selectable option button
    │   │   ├── CountdownTimer.jsx        # NEW — time remaining display
    │   │   └── PlayerStatusList.jsx      # NEW — other players' statuses
    │   └── services/
    │       ├── api.js                    # MODIFIED — add event endpoints
    │       └── websocket.js              # NEW — STOMP client wrapper
    └── package.json                      # MODIFIED — add @stomp/stompjs, sockjs-client
```

**Structure Decision**: Follows the existing web application structure with backend/ containing both Spring Boot Java sources and the React frontend. New files are added alongside existing patterns (models in model/, services in service/, etc.). A new `config/` package is created for the WebSocket configuration class, consistent with Spring Boot conventions.

## Complexity Tracking

No constitution violations; table not required.
