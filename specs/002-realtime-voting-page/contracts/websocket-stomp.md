# WebSocket (STOMP) Contract: Game Event Real-Time Messages

**Feature**: 002-realtime-voting-page  
**Endpoint**: `/ws` (SockJS)  
**Protocol**: STOMP 1.2 over SockJS  
**Authentication**: `X-Player-Code` header sent during STOMP CONNECT

---

## Connection

### CONNECT
```
CONNECT
X-Player-Code: <player-code>
accept-version: 1.2
heart-beat: 10000,10000
```

Server validates the player code and stores the player identity in the STOMP session.

---

## Subscriptions (Server → Client)

### SUBSCRIBE /topic/games/{gameCode}/event

**Purpose**: Receive all real-time updates for the active game event. All players in the game subscribe to this topic.

**Message Types** (distinguished by `type` field):

#### Type: SELECTION_UPDATE

Broadcast when any player changes their selection.

```json
{
  "type": "SELECTION_UPDATE",
  "playerId": "player-001",
  "playerName": "Alice",
  "selectedItems": ["Forest"],
  "submissionStatus": "SELECTED"
}
```

#### Type: SUBMISSION_UPDATE

Broadcast when a player submits or cancels submission (PlayersMustAgree mode).

```json
{
  "type": "SUBMISSION_UPDATE",
  "playerId": "player-001",
  "playerName": "Alice",
  "selectedItems": ["Forest"],
  "submissionStatus": "SUBMITTED"
}
```

#### Type: DISAGREEMENT

Broadcast when PlayersMustAgree detects mismatched submissions. All submission statuses are reset.

```json
{
  "type": "DISAGREEMENT",
  "message": "Players submitted different selections. Please re-select and try again."
}
```

#### Type: EVENT_RESOLVED

Broadcast when the event is resolved (by completion, agreement, or timeout).

```json
{
  "type": "EVENT_RESOLVED",
  "resolutionType": "AGREEMENT_REACHED",
  "resolvedAt": 1743206400000,
  "finalSelections": {
    "player-001": { "playerName": "Alice", "selectedItems": ["Forest"], "submissionStatus": "SUBMITTED" },
    "player-002": { "playerName": "Bob", "selectedItems": ["Forest"], "submissionStatus": "SUBMITTED" }
  }
}
```

---

## Client Messages (Client → Server)

### SEND /app/games/{gameCode}/event/select

**Purpose**: Player saves their current selection (toggle button change).

```json
{
  "selectedItems": ["Forest"],
  "textInput": null
}
```

**Server behavior**:
1. Validates `selectedItems` against `listOfItems`
2. If `singleAnswer` is true and multiple items sent, rejects with error
3. Saves selection to `PlayerSelection`
4. If `showOthersSelections` is true, broadcasts `SELECTION_UPDATE` to topic
5. If player was SUBMITTED, reverts to SELECTED (implicit cancel on re-select)

### SEND /app/games/{gameCode}/event/submit

**Purpose**: Player finalizes their selections.

```json
{}
```

(No body needed — server uses the player's already-saved selections.)

**Server behavior**:
1. Validates submit preconditions (selection count, text input if required)
2. Sets `submissionStatus` to SUBMITTED
3. Broadcasts `SUBMISSION_UPDATE` to topic
4. If `playersMustAgree` is true and all players have submitted:
   - If all selections match → resolves event, broadcasts `EVENT_RESOLVED`
   - If selections differ → resets all submissions, broadcasts `DISAGREEMENT`

### SEND /app/games/{gameCode}/event/cancel-submit

**Purpose**: Player retracts their submission (only when PlayersMustAgree is true).

```json
{}
```

**Server behavior**:
1. Validates `playersMustAgree` is true and player status is SUBMITTED
2. Sets `submissionStatus` to SELECTED
3. Broadcasts `SUBMISSION_UPDATE` to topic (status reverts to SELECTED)

---

## Error Handling

Errors are sent as STOMP ERROR frames or returned via a user-specific queue:

### SUBSCRIBE /user/queue/errors

```json
{
  "error": "Invalid selection: 'Mountain' is not in the list of items",
  "code": "INVALID_SELECTION"
}
```

**Error Codes**:
| Code | Meaning |
|---|---|
| INVALID_SELECTION | Selected item not in listOfItems |
| TOO_MANY_SELECTED | Exceeds singleAnswer constraint |
| EVENT_RESOLVED | Event already resolved, no changes allowed |
| SUBMIT_PRECONDITIONS_NOT_MET | Min/max or text input validation failed |
| CANCEL_NOT_ALLOWED | playersMustAgree is false or player not submitted |
| NOT_AUTHENTICATED | Invalid or missing player code |

---

## Message Flow Examples

### Basic Selection (ShowOthersSelections = true)
```
Alice → SEND /app/games/XYZW12/event/select {"selectedItems": ["Forest"]}
Server → BROADCAST /topic/games/XYZW12/event {"type": "SELECTION_UPDATE", "playerId": "p1", "playerName": "Alice", "selectedItems": ["Forest"], "submissionStatus": "SELECTED"}
```

### PlayersMustAgree Happy Path
```
Alice → SEND submit
Server → BROADCAST {"type": "SUBMISSION_UPDATE", ... "submissionStatus": "SUBMITTED"}
Bob   → SEND submit  (same selection as Alice)
Server → BROADCAST {"type": "EVENT_RESOLVED", "resolutionType": "AGREEMENT_REACHED", ...}
```

### PlayersMustAgree Disagreement
```
Alice → SEND submit (selected: ["Forest"])
Bob   → SEND submit (selected: ["Cave"])
Server → BROADCAST {"type": "DISAGREEMENT", "message": "Players submitted different selections..."}
(All submission statuses reset to SELECTED)
```

### Timeout
```
(EndTime reached)
Server → BROADCAST {"type": "EVENT_RESOLVED", "resolutionType": "TIMEOUT", ...}
```
