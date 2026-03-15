# Implementation Plan: Traitors Game Core

**Branch**: `001-traitors-game-core` | **Date**: 2026-03-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-traitors-game-core/spec.md`

## Summary

Build the foundational skeleton for a Traitors-style social deduction game. The system uses a Java 17 / Spring Boot 3 backend with a React (Vite) frontend and Maven as the build tool. This phase creates all domain model classes, game lifecycle orchestration (GameThread), channel-based chat, voting mechanisms, player invitation via email/SMS, and mobile-optimized React pages — all with stub method bodies ready for future implementation. The architecture enforces strict separation between game logic (pure Java POJOs + services) and presentation (React UI + Spring REST controllers).

## Technical Context

**Language/Version**: Java 17 (LTS)  
**Primary Dependencies**: Spring Boot 3.2, Spring Web (REST controllers), Spring Mail (email), React 18 (Vite), Twilio SDK or similar (SMS — interface-based, swappable)  
**Storage**: In-memory for this phase (ConcurrentHashMap-based game store); database integration deferred  
**Testing**: JUnit 5 (Jupiter) + Mockito for backend; React Testing Library + Jest for frontend  
**Target Platform**: Mobile-first web browsers (responsive HTML served by Spring Boot, React SPA)  
**Project Type**: Web application (Spring Boot backend + React frontend)  
**Performance Goals**: Support up to 52 concurrent players per game session; sub-200ms API response times  
**Constraints**: Mobile-first responsive design; code-based player authentication (no OAuth); maximum 52 players (deck size); all game logic testable without UI  
**Scale/Scope**: Single game server; 4-52 players per game; ~7 React pages/screens; ~30 Java classes/interfaces

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with The Heartless Constitution principles:

- **Testability First**: ✅ PASS — All game logic resides in pure Java service classes with dependency injection. Domain objects are POJOs with no framework coupling. Every public method has clear inputs/outputs suitable for unit testing. Spring controllers are thin wrappers delegating to testable services. React components are presentational with testable hooks.
- **Method Simplicity**: ✅ PASS — GameThread methods (GameInit, GameStart, GameEnd) delegate to event objects. Each event is a focused class implementing EventObjectInterface. Voting, card assignment, and traitor selection are separate, single-responsibility methods.
- **Separation of Concerns**: ✅ PASS — Three-layer architecture: (1) Domain models (pure Java, zero framework imports), (2) Service layer (game logic, event processing), (3) Presentation layer (Spring controllers + React UI). Game logic is fully testable without Spring context or React.
- **Explicit Over Implicit**: ✅ PASS — Enums for game status, card suits, murder results, and game stages. Interfaces define contracts for events, conditions, channels, and items. Descriptive method names throughout (e.g., `selectTraitorsByPlayerCount()`, `assignUniqueCardToPlayer()`).

**Constitution Deviations (Justified)**:

| Deviation | Constitution Rule | Justification |
|-----------|-------------------|---------------|
| Spring Boot 3.2 dependency | "No runtime dependencies beyond standard Java library unless justified" | User explicitly requested Spring. A game server requires HTTP endpoints, WebSocket support, and email integration — Spring Boot is the standard Java solution for this. |
| React 18 frontend | "Pure HTML5/CSS3/JavaScript (no frameworks unless justified)" | User explicitly requested React. Mobile-optimized dynamic UI with real-time chat, conditional menu rendering, and multiple interactive screens justifies a component framework. |
| Twilio SDK (SMS) | "No runtime dependencies beyond standard Java library unless justified" | User requires SMS/text invitations. SMS delivery requires a third-party provider. The dependency is isolated behind an interface (MessageSenderInterface) allowing swappable implementations. |
| Maven build tool | Constitution says "Maven or Gradle" | User explicitly requested Maven. No conflict — constitution allows either. |

## Project Structure

### Documentation (this feature)

```text
specs/001-traitors-game-core/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (REST API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/heartless/
│   │   │   ├── TheHeartlessApplication.java
│   │   │   ├── model/
│   │   │   │   ├── Player.java
│   │   │   │   ├── Card.java
│   │   │   │   ├── GameObject.java
│   │   │   │   ├── RoundObject.java
│   │   │   │   ├── Vote.java
│   │   │   │   ├── VoteResultObject.java
│   │   │   │   └── enums/
│   │   │   │       ├── GameStatusEnum.java
│   │   │   │       ├── GameStageEnum.java
│   │   │   │       ├── CardSuit.java
│   │   │   │       ├── CardNumber.java
│   │   │   │       └── MurderResultEnum.java
│   │   │   ├── gamethread/
│   │   │   │   ├── GameThread.java
│   │   │   │   └── GameCriteriaObject.java
│   │   │   ├── event/
│   │   │   │   ├── EventObjectInterface.java
│   │   │   │   ├── LobbyEvent.java
│   │   │   │   ├── VoteEvent.java
│   │   │   │   ├── MurderEvent.java
│   │   │   │   ├── MiniGameEvent.java
│   │   │   │   ├── TieBreakEvent.java
│   │   │   │   ├── VoteRevealEvent.java
│   │   │   │   ├── RecruitEvent.java
│   │   │   │   ├── RevelMurderEvent.java
│   │   │   │   └── PreVoteEvent.java
│   │   │   ├── operation/
│   │   │   │   ├── condition/
│   │   │   │   │   ├── ConditionInterface.java
│   │   │   │   │   ├── NumberOfPlayerCondition.java
│   │   │   │   │   └── TimeCondition.java
│   │   │   │   ├── channel/
│   │   │   │   │   ├── ChannelObjectInterface.java
│   │   │   │   │   ├── AllPlayersChannel.java
│   │   │   │   │   ├── DeadPlayersChannel.java
│   │   │   │   │   ├── TraitorsChannel.java
│   │   │   │   │   └── NonTraitorsChannel.java
│   │   │   │   └── item/
│   │   │   │       ├── ItemsInterface.java
│   │   │   │       ├── Shield.java
│   │   │   │       ├── Knife.java
│   │   │   │       ├── Tether.java
│   │   │   │       ├── Duel.java
│   │   │   │       └── Lease.java
│   │   │   ├── service/
│   │   │   │   ├── GameService.java
│   │   │   │   ├── PlayerService.java
│   │   │   │   ├── InvitationService.java
│   │   │   │   ├── TraitorSelectionService.java
│   │   │   │   ├── CardAssignmentService.java
│   │   │   │   ├── VotingService.java
│   │   │   │   ├── ChatService.java
│   │   │   │   └── ChatSenderObject.java
│   │   │   ├── controller/
│   │   │   │   ├── GameController.java
│   │   │   │   ├── PlayerController.java
│   │   │   │   ├── ChatController.java
│   │   │   │   └── VoteController.java
│   │   │   └── messaging/
│   │   │       ├── MessageSenderInterface.java
│   │   │       ├── EmailMessageSender.java
│   │   │       └── SmsMessageSender.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/heartless/
│           ├── model/
│           ├── gamethread/
│           ├── event/
│           ├── operation/
│           ├── service/
│           └── controller/
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── public/
    │   └── cards/          # Card images (52 cards)
    └── src/
        ├── App.jsx
        ├── main.jsx
        ├── pages/
        │   ├── LoginPage.jsx
        │   ├── LobbyPage.jsx
        │   ├── MenuPage.jsx
        │   ├── TraitorChatPage.jsx
        │   ├── AllChatPage.jsx
        │   ├── IndividualChatPage.jsx
        │   ├── BanishVotePage.jsx
        │   ├── MurderVotePage.jsx
        │   ├── ActionsPage.jsx       # Stub
        │   └── GameLogsPage.jsx      # Stub
        ├── components/
        │   ├── ChatWindow.jsx
        │   ├── PlayerList.jsx
        │   ├── VoteCard.jsx
        │   └── GameStatusBar.jsx
        └── services/
            └── api.js
```

**Structure Decision**: Web application layout selected (Option 2 variant). Backend is a Spring Boot Maven project at `backend/`. Frontend is a React Vite app nested at `backend/frontend/` and served as static resources via Spring Boot in production. Test directories mirror source packages for clear test-to-source mapping.

## Post-Design Constitution Check

Re-evaluation after data-model.md and contracts/rest-api.md are complete:

- **Testability First**: ✅ PASS — All entities are pure POJOs with no framework coupling. Interfaces (EventObjectInterface, ConditionInterface, ChannelObjectInterface, ItemsInterface, MessageSenderInterface) define clear contracts enabling dependency injection and mocking. VoteResultObject derives getWinner/getTally/isTie deterministically from its vote list. GameThread accepts injectable dependencies (GameCriteriaObject, event list). REST endpoints are stateless request/response — unit-testable with mock services.
- **Method Simplicity**: ✅ PASS — Each interface has 2–4 focused methods. EventObjectInterface: checkStartConditions, checkEndConditions, execute, getGame. ConditionInterface: single method checkGameConditions. ItemsInterface: getName, use, isUsable. Game logic is decomposed into discrete Event implementations (LobbyEvent, VoteEvent, MurderEvent, etc.), keeping each event's execute() focused on one concern.
- **Separation of Concerns**: ✅ PASS — Domain layer (model/, gamethread/, event/, operation/) contains zero Spring or React references. Service layer wraps domain logic with Spring DI. Controller layer translates HTTP ↔ domain. ChannelObjectInterface separates chat routing from game logic. MessageSenderInterface separates notification delivery from domain.
- **Explicit Over Implicit**: ✅ PASS — 6 enums with documented values and transitions. All entity fields have explicit types, constraints, and validation rules. State transition diagrams documented for Player and GameObject. REST contracts specify exact JSON schemas, HTTP status codes, and error conditions. Method names are descriptive (checkStartConditions, getEligiblePlayers, checkGameConditions).

**New Deviations Found**: None — no additional deviations beyond the 4 justified in the pre-design check.

## Complexity Tracking

| Deviation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Spring Boot framework | HTTP server, REST APIs, email, DI container needed for multiplayer game server | Raw Java ServerSocket would require reimplementing routing, DI, email — massive scope increase |
| React framework | 10+ interactive screens with real-time updates, conditional rendering, card backgrounds | Vanilla JS would require manual DOM management, state synchronization, and routing — error-prone at this scale |
| Twilio/SMS dependency | User requires text message invitations | No standard Java library for SMS; abstracted behind interface to keep coupling minimal |
