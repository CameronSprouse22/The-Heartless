# Tasks: Vote Page User Selection State

**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31
**Input**: Design documents from `specs/003-vote-selection-state/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Per The Heartless Constitution (Principle I: Testability First), ALL tasks involving method implementation include unit tests. TDD: write tests first, confirm they fail, then implement.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no incomplete task dependencies)
- **[Story]**: User story label — US1/US2/US3/US4 (maps to spec.md priorities P1–P4)
- Exact file paths are included in all task descriptions

## Path Convention

`backend/src/main/java/com/heartless/` → application source  
`backend/src/test/java/com/heartless/` → test source  
All paths are relative to repository root.

---

## Phase 1: Setup

**Purpose**: Confirm the project compiles cleanly on the feature branch before making changes.

- [X] T001 Verify clean build by running `mvn clean install` in `backend/` and confirming zero errors and all pre-existing tests pass

**Checkpoint**: Project builds and all existing tests pass — changes can begin.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the core `UserSelectionsState` POJO and extend `GameObject` — these are required by ALL user stories and MUST be complete before any story phase begins.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 Write `UserSelectionsStateTest.java` with failing TDD tests covering: `addSelectedItem`/`removeSelectedItem` round-trips, `setSelectedItems(null)` stores empty list, `setTextFieldInput(null)` stores empty string, `getSelectedItems()` returns defensive copy, `setSubmitPressed(true)` then `setSubmitPressed(false)` remains `true`, concurrent safety smoke check — in `backend/src/test/java/com/heartless/model/UserSelectionsStateTest.java`
- [X] T003 [P] Create `UserSelectionsState.java` POJO with synchronized fields `selectedItems` (ArrayList), `textFieldInput` (String, default ""), `submitPressed` (boolean, default false), and public API: `addSelectedItem(String)`, `removeSelectedItem(String)`, `setSelectedItems(List<String>)`, `getSelectedItems()` (defensive copy), `setTextFieldInput(String)`, `getTextFieldInput()`, `setSubmitPressed(boolean)` (irreversible once true), `isSubmitPressed()` — include Javadoc on all public methods per Constitution Principle IV — in `backend/src/main/java/com/heartless/model/UserSelectionsState.java`
- [X] T004 Add `selectionStateMap` field (`Map<String, UserSelectionsState>`, initialized as empty `HashMap` in constructor) and three new methods — `initSelectionStates(List<String> playerIds)` (clears map, populates one fresh `UserSelectionsState` per player ID), `getSelectionState(String playerId)` (returns state for player; returns `null` if not initialized), `getSelectionStateMap()` (returns the full map) — to `backend/src/main/java/com/heartless/model/GameObject.java`
- [X] T005 Run `mvn test` in `backend/` to confirm T002 tests now pass against T003 implementation, GameObject tests still pass, and zero regressions exist

**Checkpoint**: `UserSelectionsState` POJO tested and implemented, `GameObject` extended — all user story phases can now begin.

---

## Phase 3: User Story 1 - Real-Time Selection Saved to Backend (Priority: P1) 🎯 MVP

**Goal**: Every candidate selection/deselection on Banish or Murder vote pages is immediately persisted to the player's `UserSelectionsState.selectedItems` in the backend.

**Independent Test**: Open a vote page as a player via TestDashboard, click a candidate button, then query `GameObject.getSelectionState(playerId).getSelectedItems()` in the debugger — the selected candidate ID should appear without having pressed Submit. Toggle off — it should disappear.

### Implementation for User Story 1

- [X] T006 [US1] Add `game.initSelectionStates(playerIds)` call at the start of the vote round in `backend/src/main/java/com/heartless/event/VoteEvent.java` (inside the method that initializes the vote phase, passing the list of active player IDs)
- [X] T007 [US1] Add new `@MessageMapping("/games/{gameCode}/banish-selection")` handler to `VoteController.java` that: (1) looks up `GameObject` via `gameCode`, (2) resolves `playerId` from the STOMP session or message payload, (3) calls `game.getSelectionState(playerId).setSelectedItems(targetId != null ? List.of(targetId) : List.of())`, (4) broadcasts to `/topic/games/{gameCode}/banish-vote` — in `backend/src/main/java/com/heartless/controller/VoteController.java`
- [X] T008 [US1] Add persistence step to the existing murder-selection `@MessageMapping` handler in `VoteController.java`: after resolving `playerId`, call `game.getSelectionState(playerId).setSelectedItems(targetIds)` before broadcasting — in `backend/src/main/java/com/heartless/controller/VoteController.java`

**Checkpoint**: US1 complete — banish and murder selections are persisted in real-time. Verifiable independently via TestDashboard + debugger.

---

## Phase 4: User Story 2 - Text Field Input Saved to Backend (Priority: P2)

**Goal**: Every keystroke in a vote page text field is immediately reflected in `UserSelectionsState.textFieldInput` in the backend.

**Independent Test**: Open a vote page with a text field, type several characters — query `GameObject.getSelectionState(playerId).getTextFieldInput()` and confirm it matches what the player typed. Clear the field — confirm it stores `""` (not null).

### Implementation for User Story 2

- [X] T009 [US2] Add new `@MessageMapping("/games/{gameCode}/vote-text")` handler to `VoteController.java` that: (1) looks up `GameObject` via `gameCode`, (2) resolves `playerId`, (3) calls `game.getSelectionState(playerId).setTextFieldInput(payload.getText())` — no broadcast required — in `backend/src/main/java/com/heartless/controller/VoteController.java`
- [X] T010 [US2] Add WebSocket `send()` call on `onChange` for any text field on vote pages in the frontend: send `{ "text": currentValue }` to `/app/games/{gameCode}/vote-text` whenever the field value changes — in the relevant vote page component(s) under `backend/frontend/src/pages/` (e.g., `BanishVotePage.jsx`, `MurderVotePage.jsx` — check if text fields exist and add send if missing)
- [ ] T011 [US2] Verify US2 end-to-end: type in vote page text field, confirm `textFieldInput` updates in backend state

**Checkpoint**: US2 complete — text field input is persisted in real-time. Independent from US1 for testing.

---

## Phase 5: User Story 3 - Submit State Tracked (Priority: P3)

**Goal**: When a player presses Submit on any vote page, `SubmitPressed` is set to `true` irreversibly in their `UserSelectionsState`.

**Independent Test**: Open a vote page, make a selection, press Submit — query `GameObject.getSelectionState(playerId).isSubmitPressed()` and confirm it is `true`. Confirm selections are still present. Confirm calling `setSubmitPressed(false)` has no effect.

### Implementation for User Story 3

- [X] T012 [US3] Add `game.getSelectionState(voterId).setSubmitPressed(true)` to `VotingService.castBanishVote()` immediately after the vote is recorded — in `backend/src/main/java/com/heartless/service/VotingService.java`
- [X] T013 [US3] Add `game.getSelectionState(voterId).setSubmitPressed(true)` to `VotingService.castMurderVote()` immediately after the vote is recorded — in `backend/src/main/java/com/heartless/service/VotingService.java`

**Checkpoint**: US3 complete — Submit tracking is persistent and irreversible. All three state fields (selectedItems, textFieldInput, submitPressed) are now fully operational.

---

## Phase 6: User Story 4 - EventObjectInterface Exposes Selection State (Priority: P4)

**Goal**: Replace `getGame()` with `getUsersSelections()` across the entire `EventObjectInterface` contract and all 10 implementing classes. Downstream callers get a typed list of each player's current vote state.

**Independent Test**: Compile the project with all event classes updated — zero compilation errors and all tests pass. Call `event.getUsersSelections()` on a running event — returns one `UserSelectionsState` per active player.

### Implementation for User Story 4

- [X] T014 [US4] Replace `getGame()` with `getUsersSelections()` (returning `ArrayList<UserSelectionsState>`) in `EventObjectInterface.java`, update import to include `UserSelectionsState` and `ArrayList` — in `backend/src/main/java/com/heartless/event/EventObjectInterface.java`
- [X] T015 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/VoteEvent.java`
- [X] T016 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/MurderEvent.java`
- [X] T017 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/TieBreakEvent.java`
- [X] T018 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/PreVoteEvent.java`
- [X] T019 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/VoteRevealEvent.java`
- [X] T020 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/RevelMurderEvent.java`
- [X] T021 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/RecruitEvent.java`
- [X] T022 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/MiniGameEvent.java`
- [X] T023 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/LobbyEvent.java`
- [X] T024 [P] [US4] Implement `getUsersSelections()` as `new ArrayList<>(game.getSelectionStateMap().values())` (removing `getGame()`) in `backend/src/main/java/com/heartless/event/TestingEvent.java`
- [X] T025 [US4] Run `mvn clean install` in `backend/` — confirm zero compilation errors, all existing tests pass, and SC-004/SC-005 success criteria are met

**Checkpoint**: US4 complete — `EventObjectInterface` contract updated across all 10 implementations. Full project compiles and all tests green.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, Javadoc completeness, Constitution re-check.

- [X] T026 [P] Verify all public methods on `UserSelectionsState` and new `GameObject` methods have Javadoc comments per Constitution Principle IV (Explicit Over Implicit) — add any missing docs in `backend/src/main/java/com/heartless/model/`
- [ ] T027 Run the full quickstart.md Manual Test — Create Test Game → Open All Players → navigate to vote round → toggle candidates → type text → press Submit → confirm all three state fields reflect correctly in backend
- [X] T028 Run `mvn test` final full suite pass in `backend/` — confirm all success criteria SC-001 through SC-005 are satisfied

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **Phase 3 (US1)**: Depends on Phase 2 — no other story dependencies
- **Phase 4 (US2)**: Depends on Phase 2 — no other story dependencies (can start in parallel with US1)
- **Phase 5 (US3)**: Depends on Phase 2 — no other story dependencies (can start in parallel with US1/US2)
- **Phase 6 (US4)**: Depends on Phase 2 — independent of US1/US2/US3 (can start in parallel)
- **Phase 7 (Polish)**: Depends on all phases complete

### User Story Dependencies

| Story | Depends On | Can Start When |
|-------|-----------|---------------|
| US1 (P1) | Phase 2 | `UserSelectionsState` + `GameObject` extension done |
| US2 (P2) | Phase 2 | Same — independent of US1 |
| US3 (P3) | Phase 2 | Same — independent of US1/US2 |
| US4 (P4) | Phase 2 | Same — independent of US1/US2/US3 |

### Within Each User Story

- T002 (write failing tests) → T003 (implement POJO, tests now pass) → T004 (extend GameObject)
- For US1: T006 (init in event) → T007 (banish handler) then T008 (murder handler persistence)
- For US4: T014 (interface change) → T015–T024 can all run in parallel (different files)

---

## Parallel Execution Examples

### Phase 2 — Foundational (example)

```
T002 (write tests)  ─────────────────────────────────► run against T003 impl
T003 (POJO impl) ────────────────────────────────────► compile, test
T004 (GameObject) ──────────────────────────────────► depends on T003 complete
```

T002 and T003 can be worked simultaneously (different files). T004 requires T003 done (type reference).

### Phase 6 — US4 Event Implementations (example: all in parallel)

```
T015 VoteEvent         ──────────────────────────────┐
T016 MurderEvent       ──────────────────────────────┤
T017 TieBreakEvent     ──────────────────────────────┤
T018 PreVoteEvent      ──────────────────────────────┼──► T025 compile check
T019 VoteRevealEvent   ──────────────────────────────┤
T020 RevelMurderEvent  ──────────────────────────────┤
T021 RecruitEvent      ──────────────────────────────┤
T022 MiniGameEvent     ──────────────────────────────┤
T023 LobbyEvent        ──────────────────────────────┤
T024 TestingEvent      ──────────────────────────────┘
```

After T014 (interface change), all 10 event files are independent — apply all in one pass.

---

## Implementation Strategy

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1) — proves the core selection persistence loop end-to-end.

**Incremental Delivery**:
1. **Sprint 1 (MVP)**: Phases 1–3 — selections persist in real-time, verifiable via debugger
2. **Sprint 2**: Phase 4 (US2, text fields) + Phase 5 (US3, submit tracking)
3. **Sprint 3**: Phase 6 (US4, interface refactor) — purely structural, zero UX impact

**Risk**: US4 (interface refactor) touches 11 files but each change is a trivial one-liner. Batch all 10 event files in a single commit after T014.

---

## Summary

| Metric | Count |
|--------|-------|
| Total tasks | 28 |
| US1 tasks | 3 (T006–T008) |
| US2 tasks | 3 (T009–T011) |
| US3 tasks | 2 (T012–T013) |
| US4 tasks | 12 (T014–T025) |
| Foundational tasks | 4 (T002–T005) |
| Polish tasks | 3 (T026–T028) |
| Parallelizable [P] tasks | 13 |
| New files created | 2 |
| Files modified | 14 |
| Suggested MVP | Phases 1–3 (US1): 6 tasks |
