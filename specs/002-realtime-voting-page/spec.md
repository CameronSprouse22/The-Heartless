# Feature Specification: Real-Time Voting & Game Page

**Feature Branch**: `002-realtime-voting-page`  
**Created**: 2026-03-28  
**Status**: Draft  
**Input**: User description: "Create a page for voting and trivia games with WebSocket real-time updates, configurable toggle buttons, text input, player agreement logic, and auto-resolution on timeout."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Player Selects Options on Game/Vote Page (Priority: P1)

A player navigates to the game/vote page during an active event. They see a title at the top, an optional prompt explaining what to do, and a set of toggle buttons representing the available choices. The player taps one or more buttons to make their selection. Their selection is immediately saved on the server so it persists even if they lose connectivity or time runs out.

**Why this priority**: Without the ability to render the page and capture selections, no other voting or game functionality can work. This is the foundational interaction for the entire feature.

**Independent Test**: Can be fully tested by opening the page with a configured event, selecting toggle buttons, and verifying the selection is persisted on the server. Delivers the core ability to capture player choices.

**Acceptance Scenarios**:

1. **Given** an active event with a Title "Choose a Destination", **When** the player opens the page, **Then** they see the title "Choose a Destination" displayed prominently at the top.
2. **Given** an active event with a non-null Prompt, **When** the player opens the page, **Then** they see the prompt text displayed below the title.
3. **Given** an active event with a null Prompt, **When** the player opens the page, **Then** no prompt element is rendered (no empty space or placeholder).
4. **Given** an event with ListOfItems ["Forest", "Cave", "River"], **When** the player views the page, **Then** they see three toggle buttons labeled "Forest", "Cave", and "River".
5. **Given** SingleAnswer is true and the player has selected "Forest", **When** they tap "Cave", **Then** "Forest" is deselected and "Cave" becomes the only selected item.
6. **Given** SingleAnswer is false, **When** the player taps "Forest" and then "Cave", **Then** both "Forest" and "Cave" are selected simultaneously.
7. **Given** the player selects an option, **When** the selection is made, **Then** it is immediately saved on the server without requiring a submit action.

---

### User Story 2 - Submit Button Validation and Controls (Priority: P1)

After making their selection, the player sees a submit button whose enabled/disabled state depends on the configured minimum and maximum selection count, and optionally on whether a text input field has been filled. The player can only submit when all validation rules are satisfied.

**Why this priority**: The submit flow with validation is essential to completing the core interaction loop. Without it, selections cannot be finalized and the game/vote cannot resolve.

**Independent Test**: Can be tested by configuring events with various min/max selection counts and InputString settings, then verifying the submit button's enabled/disabled state for each combination.

**Acceptance Scenarios**:

1. **Given** MinNumberSelectedToSubmit is 2 and the player has selected 1 item, **When** they view the submit button, **Then** it is disabled.
2. **Given** MinNumberSelectedToSubmit is 2 and the player has selected 2 items, **When** they view the submit button, **Then** it is enabled.
3. **Given** MaxNumberSelectedToSubmit is 3 and the player has selected 4 items, **When** they view the submit button, **Then** it is disabled.
4. **Given** InputString is true and the text input is empty, **When** the player views the submit button, **Then** it is disabled regardless of how many items are selected.
5. **Given** InputString is true and the text input has content and selection count is within min/max range, **When** the player views the submit button, **Then** it is enabled.
6. **Given** InputString is false, **When** the player opens the page, **Then** no text input field is rendered.
7. **Given** all validation rules are satisfied, **When** the player taps submit, **Then** their final selections (and text input if applicable) are submitted to the server.

---

### User Story 3 - Real-Time Visibility of Other Players' Selections (Priority: P2)

When ShowOthersSelections is enabled, a player can see what other players have selected in real-time as those players make their choices. Each other player's name is displayed alongside their current selection status. Updates arrive instantly without the player needing to refresh.

**Why this priority**: Real-time visibility is a key engagement feature that differentiates this from a static form. It drives social dynamics in voting and trivia. However, the core selection/submit flow must work first.

**Independent Test**: Can be tested by having two or more players on the page simultaneously, with one making a selection and checking that the other player's screen updates within seconds.

**Acceptance Scenarios**:

1. **Given** ShowOthersSelections is true, **When** Player A selects "Forest", **Then** Player B's screen updates in real-time to show "Player A: Forest".
2. **Given** ShowOthersSelections is true and Player A changes their selection from "Forest" to "Cave", **When** the update is pushed, **Then** Player B sees "Player A: Cave" (replacing the previous selection).
3. **Given** ShowOthersSelections is false, **When** other players make selections, **Then** the current player sees no information about others' choices.
4. **Given** ShowOthersSelections is true, **When** the player opens the page, **Then** they see the current state of all other players' selections (including those made before they arrived).

---

### User Story 4 - Player Agreement Flow (Priority: P2)

When PlayersMustAgree is enabled, every player must agree on the same selection(s) before the event can close. After a player submits, their submit button changes to "Cancel Submit" allowing them to retract. Other players see submission status indicators ("[playername] selected" when they have selections, "[playername] submitted" when they have finalized). The last player to submit closes the event. If players have submitted different selections, a disagreement message is displayed and all submissions are reset.

**Why this priority**: Agreement mechanics enable collaborative decision-making which is a core game dynamic. It builds directly on the submit flow from Story 2 and the real-time updates from Story 3.

**Independent Test**: Can be tested by having 2+ players submit matching selections (verifying event closes) and then again with mismatched selections (verifying disagreement message appears and submissions reset).

**Acceptance Scenarios**:

1. **Given** PlayersMustAgree is true and a player has submitted, **When** they view the submit area, **Then** the button reads "Cancel Submit".
2. **Given** PlayersMustAgree is true and a player taps "Cancel Submit", **When** the action completes, **Then** their submission is retracted and other players see their status revert to "[playername] selected".
3. **Given** PlayersMustAgree is true and Player A has made a selection but not submitted, **When** Player B views the page, **Then** Player B sees "Player A selected".
4. **Given** PlayersMustAgree is true and Player A has submitted, **When** Player B views the page, **Then** Player B sees "Player A submitted".
5. **Given** PlayersMustAgree is true and all players have submitted with the same selection(s), **When** the last player submits, **Then** the event closes and the agreed-upon result is recorded.
6. **Given** PlayersMustAgree is true and all players have submitted but with different selections, **When** the system detects the mismatch, **Then** a disagreement message is displayed to all players and all submissions are reset (players must re-select and re-submit).
7. **Given** PlayersMustAgree is false, **When** a player submits, **Then** their submission is final and they do not see a "Cancel Submit" button or other players' submission statuses.

---

### User Story 5 - Automatic Resolution on Timeout (Priority: P3)

Each event has an end time. When the timer expires, the system automatically resolves the event using whatever selections players have saved on the server at that moment. Players who have not selected anything are treated as abstaining. A countdown or time indicator is visible to all players.

**Why this priority**: Timeout resolution prevents the game from stalling indefinitely. It is critical for game flow but depends on the selection persistence (Story 1) already working correctly.

**Independent Test**: Can be tested by creating an event with a short EndTime, having some players select and others not, and verifying the event auto-resolves with the saved state after time expires.

**Acceptance Scenarios**:

1. **Given** an active event with an EndTime, **When** the player opens the page, **Then** they see a time indicator showing how much time remains.
2. **Given** the EndTime has been reached, **When** the timer expires, **Then** the system automatically resolves the event using the current saved selections from all players.
3. **Given** a player has not made any selection when time runs out, **When** the event resolves, **Then** that player is treated as abstaining and the event resolves without their input.
4. **Given** PlayersMustAgree is true and time runs out before all players have submitted, **When** the event auto-resolves, **Then** the system uses whatever selections each player had saved at that moment (agreement requirement is waived on timeout).
5. **Given** the event has been resolved (by timeout or normal completion), **When** a player views the page, **Then** they see the result and can no longer change their selections.

---

### Edge Cases

- What happens when a player loses connectivity mid-selection? Their last saved selection on the server persists; when they reconnect they see their saved state restored.
- What happens when two players submit at the exact same moment in PlayersMustAgree mode? The server processes submissions sequentially; the last one processed triggers the agreement check.
- What happens when the EndTime is in the past when the page loads? The event is immediately resolved and the player sees the result screen.
- What happens when ListOfItems is empty? The page renders the title and prompt (if any) but no toggle buttons and the submit button remains disabled.
- What happens when MinNumberSelectedToSubmit is greater than the number of items in ListOfItems? The submit button can never be enabled; this is a configuration error that should be caught at event creation.
- What happens when a player refreshes the page mid-event? Their saved selections are reloaded from the server and the page restores to their current state.
- What happens when MaxNumberSelectedToSubmit is 0 or null? No maximum limit is enforced; the player can select any number of items above the minimum.

## Requirements *(mandatory)*

### Functional Requirements

**Page Rendering**
- **FR-001**: System MUST display a configurable title at the top of the game/vote page
- **FR-002**: System MUST display a prompt below the title when the Prompt value is non-null, and MUST NOT render the prompt element when Prompt is null
- **FR-003**: System MUST render one toggle button for each item in ListOfItems, displaying the item text as the button label
- **FR-004**: System MUST render a text input field when InputString is true, and MUST NOT render it when InputString is false

**Selection Behavior**
- **FR-005**: System MUST allow only one item to be selected at a time when SingleAnswer is true (selecting a new item deselects the previous one)
- **FR-006**: System MUST allow multiple items to be selected simultaneously when SingleAnswer is false
- **FR-007**: System MUST persist each player's current selection(s) to the server immediately when a selection changes (without requiring a submit action)

**Submit Validation**
- **FR-008**: System MUST disable the submit button when fewer than MinNumberSelectedToSubmit items are selected
- **FR-009**: System MUST disable the submit button when more than MaxNumberSelectedToSubmit items are selected
- **FR-010**: System MUST disable the submit button when InputString is true and the text input field is empty
- **FR-011**: System MUST enable the submit button only when all validation rules (selection count and text input) are satisfied simultaneously

**Real-Time Updates**
- **FR-012**: System MUST push selection updates to all connected players in real-time using a persistent connection when ShowOthersSelections is true
- **FR-013**: System MUST display each other player's name alongside their current selection(s) when ShowOthersSelections is true
- **FR-014**: System MUST NOT reveal any other player's selection information when ShowOthersSelections is false
- **FR-015**: System MUST show the current state of all players' selections when a player first connects (including selections made before they connected)

**Player Agreement**
- **FR-016**: System MUST change the submit button text to "Cancel Submit" after a player submits when PlayersMustAgree is true
- **FR-017**: System MUST allow a player to retract their submission by tapping "Cancel Submit" when PlayersMustAgree is true
- **FR-018**: System MUST display "[playername] selected" for players who have made a selection but not submitted, and "[playername] submitted" for players who have finalized, when PlayersMustAgree is true
- **FR-019**: System MUST close the event and record the result when all players have submitted matching selections under PlayersMustAgree mode
- **FR-020**: System MUST display a disagreement message and reset all submissions when all players have submitted but their selections do not match under PlayersMustAgree mode
- **FR-021**: System MUST NOT show player agreement status indicators or "Cancel Submit" flow when PlayersMustAgree is false

**Timeout and Auto-Resolution**
- **FR-022**: System MUST display a visible time indicator showing remaining time based on EndTime
- **FR-023**: System MUST automatically resolve the event using each player's last saved selection(s) when EndTime is reached
- **FR-024**: System MUST treat players with no saved selections as abstaining when auto-resolving on timeout
- **FR-025**: System MUST prevent further selection changes after the event has been resolved (by timeout or normal completion)

**State Persistence**
- **FR-026**: System MUST restore a player's previously saved selections when they reconnect or refresh the page during an active event
- **FR-027**: System MUST maintain all player selections on the server so the event can be resolved at any time regardless of player connectivity

### Key Entities

- **GameEvent Configuration**: The set of parameters that define a game/vote instance — Title (string), Prompt (nullable string), ListOfItems (list of strings), SingleAnswer (boolean), ShowOthersSelections (boolean), MinNumberSelectedToSubmit (integer), MaxNumberSelectedToSubmit (integer), EndTime (epoch timestamp), InputString (boolean), PlayersMustAgree (boolean)
- **Player Selection**: A player's current state within an event — which items are selected, optional text input value, and submission status (not submitted, submitted, retracted)
- **Event Result**: The outcome of a resolved event — the final selections from each player, how the event was resolved (all submitted, agreement reached, timeout), and timestamp of resolution
- **Player Status Indicator**: A real-time display element showing another player's name and their current state (selected items, submitted/not submitted)

## Assumptions

- **Existing authentication**: Players are already authenticated via the existing player code mechanism from the core game feature; no new authentication is needed for this page
- **Single active event per game**: Only one game/vote event is active at a time within a game session
- **MaxNumberSelectedToSubmit default**: When MaxNumberSelectedToSubmit is 0 or null, no upper limit is enforced on selections
- **Disagreement reset scope**: When PlayersMustAgree detects a mismatch, only the submission status is reset — players keep their selections but must re-submit
- **Timeout overrides agreement**: If PlayersMustAgree is true but time runs out, the system resolves using saved selections without requiring agreement
- **Real-time latency**: Selection updates are delivered to other players within 2 seconds under normal network conditions
- **Mobile-first design**: The page continues the existing mobile-optimized approach from the core game feature

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Players can view the game/vote page with all configured elements (title, prompt, buttons, input) rendered correctly within 2 seconds of navigation
- **SC-002**: Player selections are persisted on the server within 1 second of tapping a toggle button, ensuring no data loss on disconnection
- **SC-003**: Other players' selection updates appear on-screen within 2 seconds of the originating player's action when ShowOthersSelections is enabled
- **SC-004**: 95% of players can complete a selection and submit within 30 seconds on their first attempt without guidance
- **SC-005**: Events auto-resolve correctly within 1 second of EndTime being reached, using the last saved server-side state for all players
- **SC-006**: In PlayersMustAgree mode, disagreement detection and submission reset completes within 2 seconds of the last player submitting
- **SC-007**: The page correctly handles all configuration combinations (SingleAnswer, InputString, ShowOthersSelections, PlayersMustAgree) with zero rendering errors
- **SC-008**: Players returning to the page after a refresh or reconnection see their previously saved selections restored with 100% accuracy
