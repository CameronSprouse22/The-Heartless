# Data Model: Vote Page User Selection State

**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31

---

## Entities

### UserSelectionsState

**File**: `backend/src/main/java/com/heartless/model/UserSelectionsState.java`

Represents one player's in-progress interaction state on a vote page during a single active vote event. Scoped to the current event — never persisted across events.

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `selectedItems` | `ArrayList<String>` | empty list | Ordered list of selected player IDs. Synchronized for thread safety. |
| `textFieldInput` | `String` | `""` | Current text field value. Empty string (never null). |
| `submitPressed` | `boolean` | `false` | Irreversible: once set to `true`, setter refuses to revert. |

**Validation rules**:
- `selectedItems` must never be null (use empty list instead)
- `textFieldInput` must never be null (use empty string instead)
- `submitPressed` transitions: `false → true` only; `true → true` is a no-op; `true → false` is rejected silently

**State transitions**:
```
Initial state:    selectedItems=[], textFieldInput="", submitPressed=false
After select:     selectedItems=[playerId], ...
After deselect:   selectedItems=[], ...
After text input: ..., textFieldInput="some text", ...
After submit:     ..., submitPressed=true  (TERMINAL — no further changes honoured for submitPressed)
```

**Methods**:
- `addSelectedItem(String playerId)` — adds to list if not already present
- `removeSelectedItem(String playerId)` — removes from list if present
- `setSelectedItems(List<String> items)` — replaces full list (for bulk update from WebSocket payload)
- `getSelectedItems()` — returns copy of list (defensive copy)
- `setTextFieldInput(String text)` — replaces; stores `""` if null passed
- `getTextFieldInput()`
- `setSubmitPressed(boolean value)` — no-op if `submitPressed` already `true`
- `isSubmitPressed()`

---

### GameObject (modified)

**File**: `backend/src/main/java/com/heartless/model/GameObject.java`

Adds a new field to hold per-player selection state for the active vote event.

**New field**:

| Field | Type | Default |
|-------|------|---------|
| `selectionStateMap` | `Map<String, UserSelectionsState>` | empty `HashMap` (initialized in constructor) |

**New methods**:
- `initSelectionStates(List<String> playerIds)` — clears the map and populates a fresh `UserSelectionsState` for each player ID. Called at the start of each vote event.
- `getSelectionState(String playerId)` — returns the `UserSelectionsState` for the given player; returns `null` if not initialized (caller must guard).
- `getSelectionStateMap()` — returns the full map (used by event implementations for `getUsersSelections()`).

---

### EventObjectInterface (modified)

**File**: `backend/src/main/java/com/heartless/event/EventObjectInterface.java`

`getGame()` is replaced by `getUsersSelections()`.

| Method | Before | After |
|--------|--------|-------|
| `getGame()` | `GameObject getGame()` | **REMOVED** |
| `getUsersSelections()` | *(absent)* | `ArrayList<UserSelectionsState> getUsersSelections()` |

All 10 event implementations (`VoteEvent`, `MurderEvent`, `TieBreakEvent`, `PreVoteEvent`, `VoteRevealEvent`, `RevelMurderEvent`, `RecruitEvent`, `MiniGameEvent`, `LobbyEvent`, `TestingEvent`) implement:
```java
@Override
public ArrayList<UserSelectionsState> getUsersSelections() {
    return new ArrayList<>(game.getSelectionStateMap().values());
}
```

---

## Relationships

```
GameObject  1──────────────────* UserSelectionsState
             selectionStateMap    (playerId → state)
             (scoped to active event, reset per event)

EventObjectInterface
  getUsersSelections() ──delegates──> GameObject.getSelectionStateMap().values()
```

---

## Data Flow

### Selection toggle (Banish or Murder)

```
Browser click
  → toggleSelect() in BanishVotePage/MurderVotePage
  → send() via WebSocket: /app/games/{gameCode}/banish-selection or /murder-selection
  → VoteController @MessageMapping handler
      → gameStore.getGame(gameCode).getSelectionState(playerId).setSelectedItems(...)
      → messagingTemplate.convertAndSend(topic, broadcast)   ← broadcast to other clients
```

### Text field change

```
Browser onChange
  → send() via WebSocket: /app/games/{gameCode}/vote-text
  → VoteController @MessageMapping handler
      → gameStore.getGame(gameCode).getSelectionState(playerId).setTextFieldInput(text)
      (no broadcast needed unless listening UI panels require it)
```

### Submit pressed

```
Browser click Submit
  → castBanishVote() or castMurderVote() REST POST
  → VotingService.castBanishVote() / castMurderVote()
      → existing vote recording logic
      → game.getSelectionState(playerId).setSubmitPressed(true)   ← NEW
```

### Reading selections (for event/game-thread logic)

```
Via EventObjectInterface:
  event.getUsersSelections()
  → new ArrayList<>(game.getSelectionStateMap().values())
  → [UserSelectionsState(player1), UserSelectionsState(player2), ...]
```
