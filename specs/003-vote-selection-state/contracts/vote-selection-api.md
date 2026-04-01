# API & WebSocket Contracts: Vote Page User Selection State

**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31

This document defines the interface contracts exposed by the backend as a result of this feature: new and modified WebSocket message schemas and the unchanged REST endpoints that now additionally update `UserSelectionsState`.

---

## WebSocket Contracts (STOMP over SockJS)

### 1. Banish Selection Update (NEW handler)

**Direction**: Client → Server
**Destination**: `/app/games/{gameCode}/banish-selection`
**Trigger**: Player clicks a candidate button on the Banish vote page (select or deselect)

**Inbound payload** (sent by frontend — no change to frontend code):
```json
{
  "targetId": "player-uuid-or-null",
  "targetName": "Player Name or null"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `targetId` | `String \| null` | yes | Player ID of the selected candidate; `null` if deselecting |
| `targetName` | `String \| null` | yes | Display name; `null` if deselecting |

**Backend behaviour** (NEW):
1. Look up `GameObject` via `gameCode`.
2. Resolve `playerId` from STOMP session attributes.
3. Call `game.getSelectionState(playerId).setSelectedItems(targetId != null ? List.of(targetId) : List.of())`.
4. Broadcast to topic (existing behaviour, unchanged).

**Broadcast to topic**: `/topic/games/{gameCode}/banish-vote`
```json
{
  "type": "BANISH_SELECTION_UPDATE",
  "voterId": "player-uuid",
  "voterName": "Player Name",
  "targetId": "player-uuid-or-null",
  "targetName": "Player Name or null"
}
```

---

### 2. Murder Selection Update (MODIFIED handler — adds persistence)

**Direction**: Client → Server
**Destination**: `/app/games/{gameCode}/murder-selection`
**Trigger**: Player toggles a candidate on the Murder vote page

**Inbound payload** (unchanged):
```json
{
  "targetIds": ["player-uuid-1", "player-uuid-2"],
  "targetNames": ["Name 1", "Name 2"]
}
```

**Backend behaviour** (adds persistence to existing broadcast):
1. Existing: resolve player, build broadcast map, send to topic.
2. **NEW**: Call `game.getSelectionState(playerId).setSelectedItems(targetIds)`.

**Broadcast to topic**: `/topic/games/{gameCode}/murder-vote` — schema unchanged.

---

### 3. Vote Text Field Update (NEW handler)

**Direction**: Client → Server
**Destination**: `/app/games/{gameCode}/vote-text`
**Trigger**: Player types in a text field on a vote page

**Inbound payload**:
```json
{
  "text": "current field value"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `text` | `String` | yes | Full current value of the text field. Empty string if cleared. |

**Backend behaviour**:
1. Resolve `playerId` from STOMP session attributes.
2. Call `game.getSelectionState(playerId).setTextFieldInput(text)`.
3. No broadcast (text field is private per player unless a future requirement adds it).

---

## REST Endpoint Contracts (MODIFIED — side effects only)

These endpoints are **unchanged in request/response schema**. The only addition is that they now also set `SubmitPressed = true` in `UserSelectionsState` as a side effect.

### POST `/api/games/{gameCode}/vote/banish`

**Existing contract** (unchanged):
- Header: `X-Player-Code: {playerCode}`
- Body: `{ "targetPlayerId": "uuid" }`
- Response: `{ "voteRecorded": true, "castingPlayerId": "uuid", "targetPlayerId": "uuid" }`

**New side effect**: `game.getSelectionState(voterId).setSubmitPressed(true)`

---

### POST `/api/games/{gameCode}/vote/murder`

**Existing contract** (unchanged):
- Header: `X-Player-Code: {playerCode}`
- Body: `{ "targetPlayerIds": ["uuid1", "uuid2"] }`
- Response: `{ "voteRecorded": true, "castingPlayerId": "uuid", "targetPlayerIds": ["uuid1"] }`

**New side effect**: `game.getSelectionState(voterId).setSubmitPressed(true)`

---

## Java Interface Contract

### `EventObjectInterface`

```java
package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.UserSelectionsState;
import java.util.ArrayList;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean checkEndConditions();

    /**
     * Returns the current selection state for all players in this event.
     * Each entry reflects the player's real-time selections, text input,
     * and whether they have pressed Submit.
     *
     * @return list of per-player selection states; empty if no vote event is active
     */
    ArrayList<UserSelectionsState> getUsersSelections();

    void execute();

    /**
     * Returns a GameState snapshot for this event.
     */
    GameState getGameState();
}
```

---

## `UserSelectionsState` Public API

```java
package com.heartless.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures one player's real-time interaction state on a vote page.
 * Scoped to the current active vote event in the enclosing {@link GameObject}.
 *
 * <p>Thread-safe: all mutating methods are synchronized.
 */
public class UserSelectionsState {

    /** Selected candidate player IDs in selection order. */
    public synchronized void addSelectedItem(String playerId) { ... }
    public synchronized void removeSelectedItem(String playerId) { ... }
    public synchronized void setSelectedItems(List<String> items) { ... }
    public synchronized ArrayList<String> getSelectedItems() { ... } // returns defensive copy

    /** Current free-text field value. Never null; empty string if no input. */
    public synchronized void setTextFieldInput(String text) { ... }
    public synchronized String getTextFieldInput() { ... }

    /**
     * Records that the player has pressed Submit.
     * Once set to {@code true}, this method is a no-op for subsequent calls.
     */
    public synchronized void setSubmitPressed(boolean value) { ... }
    public synchronized boolean isSubmitPressed() { ... }
}
```
