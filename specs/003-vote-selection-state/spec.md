# Feature Specification: Vote Page User Selection State

**Feature Branch**: `003-vote-selection-state`
**Created**: 2026-03-31
**Status**: Draft
**Input**: User description: "For the vote pages, every user action (selecting/unselecting a button, typing in a text field) is saved in real-time to a backend UserSelectionsState object with SelectedItems list, TextFieldInput string, and SubmitPressed boolean. EventObjectInterface.getGame() becomes getUsersSelections() returning list of UserSelectionsState. GameObject stores SelectionState per player."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Real-Time Selection Saved to Backend (Priority: P1)

A player is on a vote page (Banish or Murder). Each time they click a candidate button to select or deselect it, that selection is immediately persisted in the backend under their player's `UserSelectionsState.SelectedItems`. Other parts of the system can read the current selections for this player at any time without waiting for them to submit.

**Why this priority**: This is the core data-capture requirement. All other stories depend on a reliable, continuously-updated state being stored per player.

**Independent Test**: Open a vote page as a player, click a candidate button, then query the backend for that player's `UserSelectionsState` — the selected item should appear in `SelectedItems` without having pressed Submit.

**Acceptance Scenarios**:

1. **Given** a player is on the Banish vote page with candidates listed, **When** they click a candidate button, **Then** that candidate's identifier appears in `SelectedItems` in the backend within one interaction cycle.
2. **Given** a player has a candidate selected, **When** they click that same candidate again to deselect it, **Then** the candidate is removed from `SelectedItems` in the backend.
3. **Given** a Murder vote page allows multiple selections, **When** a player selects two candidates, **Then** both identifiers appear in `SelectedItems`.

---

### User Story 2 - Text Field Input Saved to Backend (Priority: P2)

While on a vote page that includes a free-text input field, every change a player makes to the text field is reflected in the backend under their `UserSelectionsState.TextFieldInput`. The stored value always matches what the player currently sees in the text field.

**Why this priority**: Text input persistence ensures no data is lost if the page reloads or the game host inspects state mid-vote.

**Independent Test**: Open a vote page with a text field, type several characters, then query the backend state — `TextFieldInput` should match the current field value.

**Acceptance Scenarios**:

1. **Given** a player is on a vote page with a text field, **When** they type any characters, **Then** `TextFieldInput` in the backend matches the full current string.
2. **Given** a player has typed text, **When** they clear the field, **Then** `TextFieldInput` is stored as an empty string (not null).

---

### User Story 3 - Submit State Tracked (Priority: P3)

When a player presses the Submit button on a vote page, `SubmitPressed` is set to `true` in their `UserSelectionsState` and remains `true` for the rest of the event. This flag is readable by the backend at any time to know who has committed their vote versus who is still deciding.

**Why this priority**: Submit tracking is essential for game logic (e.g., knowing when all players have committed) but depends on selections already being saved (P1, P2).

**Independent Test**: Load a vote page, make a selection, press Submit, then query the backend — `SubmitPressed` must be `true` and the selections unchanged.

**Acceptance Scenarios**:

1. **Given** a player has made selections, **When** they press the Submit button, **Then** `SubmitPressed` is set to `true` in the backend.
2. **Given** `SubmitPressed` is `true`, **When** the vote page re-reads state, **Then** the flag remains `true` and is not reset to `false`.

---

### User Story 4 - EventObjectInterface Exposes Selection State (Priority: P4)

The `EventObjectInterface` no longer exposes `getGame()`. Instead it exposes `getUsersSelections()`, which returns all active players' `UserSelectionsState` objects for the current event. Callers use this to inspect what every player has selected, typed, and whether they have submitted.

**Why this priority**: This is a structural contract change that builds on state being populated (P1–P3). It affects every implementing class and downstream consumer.

**Independent Test**: Compile the project with all `EventObjectInterface` implementations updated; a call to `getUsersSelections()` on a running event returns one entry per active player with accurate state.

**Acceptance Scenarios**:

1. **Given** an active game event, **When** `getUsersSelections()` is called, **Then** it returns one `UserSelectionsState` per active player in the event.
2. **Given** a player has interacted with the vote page, **When** `getUsersSelections()` is called, **Then** that player's entry reflects their current `SelectedItems`, `TextFieldInput`, and `SubmitPressed`.
3. **Given** the refactor is applied, **When** the project is compiled and tests are run, **Then** there are no compilation errors and all existing tests pass.

---

### Edge Cases

- What happens when a player navigates away from the vote page mid-vote? Their last persisted state remains in the backend; no rollback occurs.
- What happens if rapid interactions arrive out of order? The last write wins for single-value fields (`TextFieldInput`); list operations (select/deselect) are applied as discrete add/remove operations.
- What happens when a vote event ends but `SubmitPressed` is still `false` for some players? Their current `SelectedItems` is treated as their final answer (assumption — see Assumptions).
- What happens if `SelectedItems` is empty when Submit is pressed? This is valid; `SubmitPressed` becomes `true` with an empty list.
- What happens if there is no text field on a vote page? `TextFieldInput` remains an empty string; no updates for it are sent.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The backend MUST maintain a `UserSelectionsState` object per player per active vote event, containing `SelectedItems` (list of selected candidate identifiers), `TextFieldInput` (current text field value), and `SubmitPressed` (boolean, defaults `false`).
- **FR-002**: Every time a player selects or deselects a candidate on a vote page, the frontend MUST notify the backend and the backend MUST update that player's `SelectedItems` immediately.
- **FR-003**: Every time a player changes the text in a vote page text field, the frontend MUST notify the backend and the backend MUST update that player's `TextFieldInput` to the current value.
- **FR-004**: When a player presses Submit on a vote page, the backend MUST set `SubmitPressed` to `true` for that player. Once `true`, this flag MUST NOT be reset to `false`.
- **FR-005**: `GameObject` MUST store and provide access to the `UserSelectionsState` for each player scoped to the current active vote event.
- **FR-006**: `EventObjectInterface` MUST replace the `getGame()` method with `getUsersSelections()`, returning the collection of `UserSelectionsState` objects for all players in the current event.
- **FR-007**: All existing implementations of `EventObjectInterface` MUST be updated to implement `getUsersSelections()`.
- **FR-008**: The backend MUST initialize an empty `UserSelectionsState` (empty list, empty string, `false`) for each player when a vote event begins.

### Key Entities

- **UserSelectionsState**: Represents one player's in-progress vote page state for a single event. Fields: `SelectedItems` (list of selected candidate identifiers, ordered by selection time), `TextFieldInput` (free-text input string, defaults empty), `SubmitPressed` (boolean, irreversibly set to `true` on submit).
- **GameObject**: The running game container. Gains a map of player ID → `UserSelectionsState` scoped to the current active event, cleared or archived when the event ends.
- **EventObjectInterface**: The contract all game events implement. `getGame()` is replaced by `getUsersSelections()` returning the per-player selection state collection for the event.

## Assumptions

- The Banish vote page uses single-selection (one item in `SelectedItems` at a time); the Murder vote page may allow multiple selections. Both are handled by the same list structure.
- "Items" in `SelectedItems` are player IDs (strings), consistent with the existing vote system.
- Real-time updates will be sent over the existing WebSocket connection already in use on vote pages.
- When a vote event ends while a player's `SubmitPressed` is `false`, their current `SelectedItems` is used as their final answer by game logic.
- If a vote page has no text field, `TextFieldInput` is simply ignored and never sent; no behavioral change is required for pages without text fields.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A player's selection change on a vote page is reflected in the backend state within the same interaction, with no additional user action required.
- **SC-002**: After any vote event concludes, every player's `UserSelectionsState` accurately reflects their last interaction state before Submit or event end.
- **SC-003**: `SubmitPressed` is `true` for every player who clicked Submit during a vote, and `false` for every player who did not.
- **SC-004**: The project compiles without errors and all pre-existing tests pass after `EventObjectInterface` is refactored from `getGame()` to `getUsersSelections()`.
- **SC-005**: Game logic that previously relied on `getGame()` through the interface continues to function correctly after the refactor.