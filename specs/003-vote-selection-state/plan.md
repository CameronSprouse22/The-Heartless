# Implementation Plan: Vote Page User Selection State

**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `specs/003-vote-selection-state/spec.md`

## Summary

Every user interaction on a vote page (candidate selection/deselection, text field input, submit press) is persisted in real-time to the backend. A new `UserSelectionsState` POJO holds per-player state for the current vote event. `GameObject` stores a `Map<String, UserSelectionsState>` (keyed by player ID). `EventObjectInterface` replaces `getGame()` with `getUsersSelections()`, returning the collection of all players' selection states. The existing WebSocket infrastructure (STOMP) is extended to persist incoming selection messages to `GameObject`, not just re-broadcast them to other clients.

## Technical Context

**Language/Version**: Java 21 (Spring Boot backend), React 18 / Vite (frontend)
**Primary Dependencies**: Spring Boot 3.x, Spring WebSocket (STOMP), Maven, React Router v6
**Storage**: In-memory (`ConcurrentHashMap` in `GameStore`; `GameObject` holds all live state — no database)
**Testing**: JUnit 5 (Jupiter), Mockito — `mvn test`
**Target Platform**: JVM server (localhost:8080), browser SPA (Vite dev server localhost:5173)
**Project Type**: Web service + SPA (REST + WebSocket backend, React frontend)
**Performance Goals**: Selection updates persisted synchronously within the same STOMP message handler invocation — no async delay
**Constraints**: No new runtime dependencies; in-memory only; must not break existing vote submission logic or WebSocket broadcasts; all existing tests must continue to pass
**Scale/Scope**: 4–15 concurrent players per game; one `UserSelectionsState` per player per active vote event; 10 event implementation classes updated

## Constitution Check

*Pre-design gate — checked against The Heartless Constitution v1.0.0*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Testability First** | ✅ PASS | `UserSelectionsState` is a pure POJO with predictable inputs/outputs. `getUsersSelections()` delegates to `GameObject` — easily unit-testable with a constructed `GameObject`. |
| **II. Method Simplicity** | ✅ PASS | Each new/modified method has a single responsibility: `addSelectedItem`, `removeSelectedItem`, `setTextFieldInput`, `setSubmitPressed` on the POJO; `getUsersSelections()` on the event (one-liner returning a list built from the stored map). |
| **III. Separation of Concerns** | ✅ PASS | `UserSelectionsState` is a pure Java POJO with no Spring, no UI references. The WebSocket handler (in `VoteController`) handles persistence; the broadcast to other clients remains separate. |
| **IV. Explicit Over Implicit** | ✅ PASS | Naming clearly expresses intent. `setSubmitPressed(true)` is irreversible (enforced by the setter refusing to reset to `false`). Javadoc required on all public APIs per constitution. |

**Post-design re-check**: Scheduled after Phase 1 design is complete. No anticipated violations.

## Project Structure

### Documentation (this feature)

```text
specs/003-vote-selection-state/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/           ← Phase 1 output
│   └── vote-selection-api.md
└── tasks.md             ← Phase 2 output (not created by /speckit.plan)
```

### Source Code (affected files)

```text
backend/
├── src/main/java/com/heartless/
│   ├── model/
│   │   └── UserSelectionsState.java         ← NEW: per-player vote interaction state
│   ├── model/
│   │   └── GameObject.java                  ← MODIFIED: add selectionStateMap field + accessors
│   ├── event/
│   │   ├── EventObjectInterface.java         ← MODIFIED: replace getGame() with getUsersSelections()
│   │   ├── VoteEvent.java                   ← MODIFIED: implement getUsersSelections()
│   │   ├── MurderEvent.java                 ← MODIFIED: implement getUsersSelections()
│   │   ├── TieBreakEvent.java               ← MODIFIED: implement getUsersSelections()
│   │   ├── PreVoteEvent.java                ← MODIFIED: implement getUsersSelections()
│   │   ├── VoteRevealEvent.java             ← MODIFIED: implement getUsersSelections()
│   │   ├── RevelMurderEvent.java            ← MODIFIED: implement getUsersSelections()
│   │   ├── RecruitEvent.java                ← MODIFIED: implement getUsersSelections()
│   │   ├── MiniGameEvent.java               ← MODIFIED: implement getUsersSelections()
│   │   ├── LobbyEvent.java                  ← MODIFIED: implement getUsersSelections()
│   │   └── TestingEvent.java                ← MODIFIED: implement getUsersSelections()
│   ├── controller/
│   │   └── VoteController.java              ← MODIFIED: persist selection + text + submit to GameObject
│   └── service/
│       └── VotingService.java               ← MODIFIED: set SubmitPressed=true on castBanishVote/castMurderVote
│
└── src/test/java/com/heartless/
    └── model/
        └── UserSelectionsStateTest.java      ← NEW: unit tests for the POJO

frontend/
└── src/
    └── services/
        └── api.js                           ← MODIFIED: add updateVoteSelection() and updateVoteTextField() API calls if REST endpoint added
    (BanishVotePage.jsx and MurderVotePage.jsx: no frontend changes needed — they already send WebSocket messages for selections)
```

**Structure Decision**: Web application (Option 2 from template). Changes are concentrated in backend model/event/controller. Frontend already sends the correct WebSocket messages for selections — the gap is purely backend persistence.

## Complexity Tracking

*No constitution violations — no entries required.*
