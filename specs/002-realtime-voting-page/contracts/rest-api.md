# REST API Contract: Game Event Endpoints

**Feature**: 002-realtime-voting-page  
**Base Path**: `/api/games/{gameCode}/event`  
**Authentication**: `X-Player-Code` header (existing mechanism)

---

## POST /api/games/{gameCode}/event

**Purpose**: Create a new game event (voting/trivia page) for a game.

**Request**:
```json
{
  "title": "Choose a Destination",
  "prompt": "Where should the group travel next?",
  "listOfItems": ["Forest", "Cave", "River"],
  "singleAnswer": true,
  "showOthersSelections": true,
  "minNumberSelectedToSubmit": 1,
  "maxNumberSelectedToSubmit": 0,
  "endTime": 1743206400000,
  "inputString": false,
  "playersMustAgree": false
}
```

**Response 201** (Created):
```json
{
  "eventId": "evt-abc123",
  "gameCode": "XYZW12",
  "title": "Choose a Destination",
  "status": "ACTIVE"
}
```

**Response 400** (Bad Request): Invalid config (e.g., blank title, min > listOfItems size)
```json
{
  "error": "minNumberSelectedToSubmit (5) exceeds number of items (3)"
}
```

**Response 404**: Game not found  
**Response 409**: An event is already active for this game

---

## GET /api/games/{gameCode}/event

**Purpose**: Get the current active event's configuration and the requesting player's selection state.

**Response 200**:
```json
{
  "eventId": "evt-abc123",
  "config": {
    "title": "Choose a Destination",
    "prompt": "Where should the group travel next?",
    "listOfItems": ["Forest", "Cave", "River"],
    "singleAnswer": true,
    "showOthersSelections": true,
    "minNumberSelectedToSubmit": 1,
    "maxNumberSelectedToSubmit": 0,
    "endTime": 1743206400000,
    "inputString": false,
    "playersMustAgree": false
  },
  "mySelection": {
    "selectedItems": ["Forest"],
    "textInput": null,
    "submissionStatus": "SELECTED"
  },
  "resolved": false,
  "result": null
}
```

**Response 200** (when resolved):
```json
{
  "eventId": "evt-abc123",
  "config": { ... },
  "mySelection": { ... },
  "resolved": true,
  "result": {
    "resolutionType": "TIMEOUT",
    "resolvedAt": 1743206400000,
    "finalSelections": {
      "player-001": { "playerName": "Alice", "selectedItems": ["Forest"], "submissionStatus": "SUBMITTED" },
      "player-002": { "playerName": "Bob", "selectedItems": [], "submissionStatus": "NONE" }
    }
  }
}
```

**Response 404**: No active or recent event for this game

---

## GET /api/games/{gameCode}/event/players

**Purpose**: Get all players' current selection state (for ShowOthersSelections). Also used for initial page load to hydrate other players' statuses.

**Response 200**:
```json
{
  "players": [
    {
      "playerId": "player-001",
      "playerName": "Alice",
      "selectedItems": ["Forest"],
      "submissionStatus": "SELECTED"
    },
    {
      "playerId": "player-002",
      "playerName": "Bob",
      "selectedItems": [],
      "submissionStatus": "NONE"
    }
  ]
}
```

**Response 403**: ShowOthersSelections is false (data not available)  
**Response 404**: No active event

---

## Notes

- All endpoints require the `X-Player-Code` header for authentication
- The `prompt` field is `null` (not empty string) when no prompt is configured
- `maxNumberSelectedToSubmit` of `0` means no upper limit
- `endTime` is epoch milliseconds (Java `System.currentTimeMillis()` format)
