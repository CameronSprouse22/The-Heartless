# Tasks: Real-Time Voting & Game Page

**Input**: Design documents from `/specs/002-realtime-voting-page/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Per The Heartless Constitution (Principle I: Testability First), ALL tasks involving method implementation MUST include corresponding unit tests. Tests MUST be written BEFORE implementation (Test-Driven Development).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/` (Java), `backend/frontend/src/` (React)
- **Tests**: `backend/src/test/java/com/heartless/`
- **Config**: `backend/src/main/java/com/heartless/config/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add dependencies, configure WebSocket infrastructure, and wire routing

- [X] T001 Add `spring-boot-starter-websocket` dependency to `backend/pom.xml`
- [X] T002 [P] Add `@stomp/stompjs` and `sockjs-client` dependencies to `backend/frontend/package.json`
- [X] T003 [P] Add `/ws` proxy entry with `ws: true` to `backend/frontend/vite.config.js`
- [X] T004 [P] Create WebSocketConfig with STOMP endpoint `/ws`, SockJS fallback, app destination prefix `/app`, and ChannelInterceptor for `X-Player-Code` authentication in `backend/src/main/java/com/heartless/config/WebSocketConfig.java`
- [X] T005 [P] Add `/event/:gameCode` route pointing to GameEventPage in `backend/frontend/src/App.jsx`

**Checkpoint**: WebSocket infrastructure is configured, dependencies installed, routing wired

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create all shared model classes, enums, and utility modules that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 [P] Create `SubmissionStatus` enum (NONE, SELECTED, SUBMITTED) in `backend/src/main/java/com/heartless/model/SubmissionStatus.java`
- [X] T007 [P] Create `ResolutionType` enum (ALL_SUBMITTED, AGREEMENT_REACHED, TIMEOUT) in `backend/src/main/java/com/heartless/model/ResolutionType.java`
- [X] T008 [P] Create `GameEventConfig` immutable model with 12 fields (eventId, gameCode, title, prompt, listOfItems, singleAnswer, showOthersSelections, minNumberSelectedToSubmit, maxNumberSelectedToSubmit, endTime, inputString, playersMustAgree) and validation (blank title, min > list size, max < min when max > 0) in `backend/src/main/java/com/heartless/model/GameEventConfig.java`
- [X] T009 [P] Create `PlayerSelection` model with fields (playerId, playerName, selectedItems, textInput, submissionStatus) and state transition methods (select, submit, cancelSubmit, resetSubmission) in `backend/src/main/java/com/heartless/model/PlayerSelection.java`
- [X] T010 [P] Create `GameEventResult` immutable model with fields (eventId, gameCode, finalSelections, resolutionType, resolvedAt) in `backend/src/main/java/com/heartless/model/GameEventResult.java`
- [X] T011 Create `GameEventState` aggregate holding config, selections map, resolved flag, result, and timeoutFuture with synchronized mutation methods in `backend/src/main/java/com/heartless/model/GameEventState.java`
- [X] T012 [P] Write `GameEventConfigTest` covering construction, validation rules (blank title, min > list size, max < min), and immutability in `backend/src/test/java/com/heartless/model/GameEventConfigTest.java`
- [X] T013 [P] Write `PlayerSelectionTest` covering state transitions (NONE→SELECTED→SUBMITTED, SUBMITTED→SELECTED cancel, disagreement reset), invalid transitions, and selectedItems validation in `backend/src/test/java/com/heartless/model/PlayerSelectionTest.java`
- [X] T014 [P] Create `websocket.js` STOMP client wrapper with connect (passing X-Player-Code header), disconnect, subscribe, and send methods in `backend/frontend/src/services/websocket.js`

**Checkpoint**: Foundation ready — all models, enums, tests passing, WebSocket client utility created. User story implementation can now begin.

---

## Phase 3: User Story 1 — Player Selects Options on Game/Vote Page (Priority: P1) 🎯 MVP

**Goal**: Player opens the event page, sees title/prompt/toggle buttons, selects options, and selections are immediately persisted on the server via WebSocket.

**Independent Test**: Open the page with an active event, tap toggle buttons, verify selections are persisted on the server. SingleAnswer mode allows only one selection; multi-select allows many.

**Covers**: FR-001, FR-002, FR-003, FR-005, FR-006, FR-007, FR-026

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T015 [US1] Write `GameEventServiceTest` tests for `createEvent()` (valid config creates event, duplicate active event returns 409, game not found returns 404) and `saveSelection()` (persists selection, singleAnswer enforces one item, multi-select allows multiple, re-select overwrites) in `backend/src/test/java/com/heartless/service/GameEventServiceTest.java`

### Implementation for User Story 1

- [X] T016 [US1] Implement `GameEventService` with constructor injecting `GameStore` and `SimpMessagingTemplate`, a `ConcurrentHashMap<String, GameEventState>` for active events, `createEvent()` that validates config and initializes PlayerSelection entries for all game players, and `saveSelection()` that validates selected items against listOfItems and persists to the selections map. Also implement `getEventState()` for retrieving current state in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T017 [US1] Implement `GameEventController` with REST `POST /api/games/{gameCode}/event` (create event from request body) and `GET /api/games/{gameCode}/event` (return config + requesting player's selection via X-Player-Code header) in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T018 [US1] Add WebSocket `@MessageMapping("/games/{gameCode}/event/select")` handler in GameEventController that extracts player identity from STOMP session, calls `saveSelection()`, and if `showOthersSelections` is true, broadcasts `SELECTION_UPDATE` message to `/topic/games/{gameCode}/event` in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T019 [US1] Add `createEvent(gameCode, config)` and `getEventState(gameCode, playerCode)` functions to `backend/frontend/src/services/api.js`
- [X] T020 [P] [US1] Create `ToggleButton` component accepting props (label, selected, onClick, disabled) with selected/unselected styling in `backend/frontend/src/components/ToggleButton.jsx`
- [X] T021 [US1] Create `GameEventPage` component that fetches event config via REST on mount, connects to STOMP topic `/topic/games/{gameCode}/event`, renders title, conditional prompt, ToggleButton list from listOfItems, handles SingleAnswer vs multi-select toggle logic, and sends selection via STOMP `/app/games/{gameCode}/event/select` on each change. Restore saved selections from REST GET response on mount in `backend/frontend/src/pages/GameEventPage.jsx`

**Checkpoint**: Player can open the event page, see the title/prompt/buttons, select options, and selections persist on the server. SingleAnswer mode works correctly. Page restores state on refresh.

---

## Phase 4: User Story 2 — Submit Button Validation and Controls (Priority: P1)

**Goal**: Player sees a submit button that enables/disables based on min/max selection count and optional text input. Submitting finalizes the player's selections.

**Independent Test**: Configure events with various min/max and inputString settings; verify submit button enables/disables correctly. Submit and verify status changes to SUBMITTED on the server.

**Covers**: FR-004, FR-008, FR-009, FR-010, FR-011

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T022 [US2] Add tests to `GameEventServiceTest` for `submitSelections()`: validates min count not met (rejects), max count exceeded (rejects), inputString required but text empty (rejects), valid submit changes status to SUBMITTED, already-submitted player re-submitting is rejected in `backend/src/test/java/com/heartless/service/GameEventServiceTest.java`

### Implementation for User Story 2

- [X] T023 [US2] Implement `submitSelections()` in `GameEventService` that validates selection count against min/max config, validates textInput if inputString is true, sets PlayerSelection status to SUBMITTED, and broadcasts `SUBMISSION_UPDATE` via STOMP in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T024 [US2] Add WebSocket `@MessageMapping("/games/{gameCode}/event/submit")` handler in GameEventController that calls `submitSelections()` and sends error to `/user/queue/errors` on validation failure in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T025 [US2] Add submit button to `GameEventPage` with client-side validation (disabled when selection count outside min/max range or when inputString is true and text input is empty). Render text input field when `config.inputString` is true. On submit, send STOMP message to `/app/games/{gameCode}/event/submit` in `backend/frontend/src/pages/GameEventPage.jsx`

**Checkpoint**: Submit button correctly enables/disables based on all validation rules. Text input renders when configured. Submitting finalizes selections on the server.

---

## Phase 5: User Story 3 — Real-Time Visibility of Other Players' Selections (Priority: P2)

**Goal**: When ShowOthersSelections is enabled, all players see each other's current selections updating in real-time via WebSocket.

**Independent Test**: Open the page in two browsers as different players. When one player selects an option, the other player's screen updates within 2 seconds showing the selection.

**Covers**: FR-012, FR-013, FR-014, FR-015

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T026 [US3] Add tests to `GameEventServiceTest` for broadcast behavior: `saveSelection()` triggers STOMP broadcast when showOthersSelections is true, does NOT broadcast when showOthersSelections is false. Test `getPlayerSelections()` returns all players' current states in `backend/src/test/java/com/heartless/service/GameEventServiceTest.java`

### Implementation for User Story 3

- [X] T027 [US3] Implement `getPlayerSelections()` in `GameEventService` returning all players' names, selectedItems, and submissionStatus for the active event in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T028 [US3] Add REST `GET /api/games/{gameCode}/event/players` endpoint in GameEventController that returns all player selection states (used for initial page load to populate existing selections) in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T029 [P] [US3] Create `PlayerStatusList` component accepting players array (name, selectedItems, submissionStatus) and rendering each player's name with their current selection status in `backend/frontend/src/components/PlayerStatusList.jsx`
- [X] T030 [US3] Integrate `PlayerStatusList` into `GameEventPage`: fetch initial player states from REST GET /players on mount, update player states on `SELECTION_UPDATE` and `SUBMISSION_UPDATE` STOMP messages, conditionally render only when `config.showOthersSelections` is true in `backend/frontend/src/pages/GameEventPage.jsx`

**Checkpoint**: Players see real-time updates of others' selections when ShowOthersSelections is enabled. Initial load shows existing selections. Updates hidden when ShowOthersSelections is false.

---

## Phase 6: User Story 4 — Player Agreement Flow (Priority: P2)

**Goal**: When PlayersMustAgree is enabled, all players must submit matching selections. Disagreement resets all submissions. Players can cancel their submission.

**Independent Test**: Two players submit matching selections — event resolves. Two players submit different selections — disagreement message shown, submissions reset. A player cancels their submission — status reverts.

**Covers**: FR-016, FR-017, FR-018, FR-019, FR-020, FR-021

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T031 [US4] Add tests to `GameEventServiceTest` for agreement flow: all players submit same selections → event resolved with AGREEMENT_REACHED, all players submit different selections → disagreement resets all to SELECTED and broadcasts DISAGREEMENT, `cancelSubmit()` reverts SUBMITTED to SELECTED (only when playersMustAgree is true, rejects when false), cancelSubmit when not in SUBMITTED state is rejected in `backend/src/test/java/com/heartless/service/GameEventServiceTest.java`

### Implementation for User Story 4

- [X] T032 [US4] Implement `checkAgreement()` (compare all submitted selectedItems sets for equality, resolve event if match, reset all submissions and broadcast DISAGREEMENT if mismatch) and `cancelSubmit()` (revert SUBMITTED→SELECTED, broadcast SUBMISSION_UPDATE) in `GameEventService`. Wire `checkAgreement()` to trigger after each `submitSelections()` call when playersMustAgree is true and all players have submitted in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T033 [US4] Add WebSocket `@MessageMapping("/games/{gameCode}/event/cancel-submit")` handler in GameEventController that calls `cancelSubmit()` and sends error to `/user/queue/errors` if cancel is not allowed in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T034 [US4] Update `GameEventPage` to: show "Cancel Submit" button (instead of Submit) when playersMustAgree is true and player status is SUBMITTED, handle `DISAGREEMENT` STOMP message by displaying a disagreement notification and resetting local submission state, show "[name] selected" / "[name] submitted" status indicators in PlayerStatusList when playersMustAgree is true, handle `EVENT_RESOLVED` message by displaying the result and freezing the UI in `backend/frontend/src/pages/GameEventPage.jsx`

**Checkpoint**: Agreement flow works end-to-end. Matching submissions resolve the event. Mismatched submissions show disagreement and reset. Cancel submit works. Status indicators show selected/submitted.

---

## Phase 7: User Story 5 — Automatic Resolution on Timeout (Priority: P3)

**Goal**: Event auto-resolves when EndTime is reached, using each player's last saved selections. A countdown timer is visible to all players.

**Independent Test**: Create an event with a short EndTime. Some players select, some don't. After timeout, the event resolves with saved selections. Abstaining players are treated as having empty selections.

**Covers**: FR-022, FR-023, FR-024, FR-025

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T035 [US5] Add tests to `GameEventServiceTest` for timeout: `resolveOnTimeout()` builds result with ALL current saved selections, players with no selections treated as abstaining (empty selectedItems), resolved event rejects further saveSelection/submitSelections calls, timeout cancels if event resolves normally before EndTime, overrides playersMustAgree when time expires in `backend/src/test/java/com/heartless/service/GameEventServiceTest.java`

### Implementation for User Story 5

- [X] T036 [US5] Implement `resolveOnTimeout()` and timeout scheduling in `GameEventService`: schedule a `ScheduledExecutorService` task at event creation time for EndTime, on fire call `resolveOnTimeout()` which sets resolved=true, builds `GameEventResult` with TIMEOUT resolution type, broadcasts `EVENT_RESOLVED` via STOMP. Normal resolution cancels the scheduled task via `timeoutFuture.cancel()`. All mutation methods (saveSelection, submit, cancelSubmit) check `resolved` flag and reject if true in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T037 [P] [US5] Create `CountdownTimer` component accepting endTime (epoch ms) as prop, computing and displaying remaining time (minutes:seconds), updating every second via setInterval, showing "Time's up" when expired in `backend/frontend/src/components/CountdownTimer.jsx`
- [X] T038 [US5] Integrate `CountdownTimer` into `GameEventPage` passing `config.endTime`. Handle `EVENT_RESOLVED` STOMP message by displaying the result summary and disabling all toggle buttons, submit button, and text input (freeze UI). If endTime is in the past on page load, render the resolved state immediately in `backend/frontend/src/pages/GameEventPage.jsx`

**Checkpoint**: Countdown timer displays remaining time. Event auto-resolves on timeout. Resolved events freeze the UI. Players who didn't select are treated as abstaining.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Edge case handling, error resilience, and validation

- [X] T039 [P] Add WebSocket error handling: send validation errors to `/user/queue/errors` with error codes (INVALID_SELECTION, TOO_MANY_SELECTED, EVENT_RESOLVED, SUBMIT_PRECONDITIONS_NOT_MET, CANCEL_NOT_ALLOWED, NOT_AUTHENTICATED) as defined in the STOMP contract in `backend/src/main/java/com/heartless/controller/GameEventController.java`
- [X] T040 [P] Handle edge cases in `GameEventService`: empty listOfItems (no buttons, submit always disabled), endTime in the past at creation (immediately resolve), re-select after submit implicitly cancels submission (reverts to SELECTED) in `backend/src/main/java/com/heartless/service/GameEventService.java`
- [X] T041 [P] Handle frontend reconnection: on WebSocket disconnect and reconnect, re-fetch event state via REST GET to restore selections and re-subscribe to STOMP topic in `backend/frontend/src/pages/GameEventPage.jsx`
- [X] T042 Run `quickstart.md` validation: start backend, create an event via REST, open the event page, test selection/submit/agreement/timeout flows end-to-end

**Checkpoint**: All edge cases handled. Error messages match STOMP contract. Reconnection restores state. Quickstart scenarios pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) — MVP delivery target
- **User Story 2 (Phase 4)**: Depends on US1 (Phase 3) — submit extends the select flow
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) — broadcast infrastructure only, can start after Phase 2 if US1 service exists
- **User Story 4 (Phase 6)**: Depends on US2 (Phase 4) — agreement extends the submit flow
- **User Story 5 (Phase 7)**: Depends on US1 (Phase 3) — timeout resolves saved selections
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — no user story dependencies
- **US2 (P1)**: Depends on US1 — submit builds on the selection saving mechanism
- **US3 (P2)**: Can start after Phase 2 in parallel with US2, but shares GameEventService file with US1 — recommend sequential after US1
- **US4 (P2)**: Depends on US2 — agreement logic wraps the submit flow
- **US5 (P3)**: Depends on US1 — timeout uses saved selections; can proceed in parallel with US3/US4

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before controllers/endpoints
- Backend before frontend for each story
- Core implementation before integration

### Parallel Opportunities

- All Setup tasks (T001–T005) marked [P] can run in parallel
- All model/enum tasks (T006–T010, T012–T014) marked [P] can run in parallel
- Within US1: T020 (ToggleButton) can run in parallel with backend tasks (T016–T018)
- Within US3: T029 (PlayerStatusList) can run in parallel with backend tasks (T027–T028)
- Within US5: T037 (CountdownTimer) can run in parallel with backend tasks (T036)
- All Polish tasks (T039–T041) marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Phase 3, Batch 1 — Tests:
Task T015: Write GameEventServiceTest for createEvent and saveSelection

# Phase 3, Batch 2 — Backend implementation (after tests fail):
Task T016: Implement GameEventService (createEvent, saveSelection, getEventState)

# Phase 3, Batch 3 — Controller + Frontend utility (after service):
Task T017: REST endpoints POST and GET /event
Task T018: WebSocket @MessageMapping for /select
Task T019: Add REST functions to api.js
Task T020: Create ToggleButton component [P — can run with T017-T019]

# Phase 3, Batch 4 — Page assembly:
Task T021: Create GameEventPage (wires everything together)
```

---

## Parallel Example: User Story 5

```bash
# Phase 7, Batch 1 — Tests:
Task T035: Write timeout resolution tests

# Phase 7, Batch 2 — Backend + Frontend component in parallel:
Task T036: Implement resolveOnTimeout and scheduling
Task T037: Create CountdownTimer component [P — different file, no dependency on T036]

# Phase 7, Batch 3 — Integration:
Task T038: Integrate CountdownTimer and EVENT_RESOLVED into GameEventPage
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (5 tasks)
2. Complete Phase 2: Foundational (9 tasks)
3. Complete Phase 3: User Story 1 (7 tasks)
4. **STOP and VALIDATE**: Player can open page, see title/prompt/buttons, select options, selections persist. Page restores state on refresh.
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready (14 tasks)
2. Add User Story 1 → Test independently → Demo (MVP: select options) (+7 = 21 tasks)
3. Add User Story 2 → Test independently → Demo (submit with validation) (+4 = 25 tasks)
4. Add User Story 3 → Test independently → Demo (real-time visibility) (+5 = 30 tasks)
5. Add User Story 4 → Test independently → Demo (player agreement) (+4 = 34 tasks)
6. Add User Story 5 → Test independently → Demo (timeout resolution) (+4 = 38 tasks)
7. Polish → Final validation (+4 = 42 tasks)

Each story adds value without breaking previous stories.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Tests written FIRST per Constitution Principle I: Testability First
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- MaxNumberSelectedToSubmit of 0 means no upper limit — handle in both backend validation and frontend button state
- GameEventState is NOT persisted — lives in ConcurrentHashMap per existing GameStore pattern
