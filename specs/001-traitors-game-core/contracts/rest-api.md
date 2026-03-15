# REST API Contracts: Traitors Game Core

**Branch**: `001-traitors-game-core` | **Date**: 2026-03-13  
**Base URL**: `/api`  
**Content-Type**: `application/json`

---

## Authentication

All endpoints (except game join) require a `X-Player-Code` header containing the player's unique code.  
The join endpoint uses the game code from the URL path.

---

## Game Management

### POST /api/games

Create a new game. The calling player becomes the VIP.

**Request**:
```json
{
  "playerName": "string (1-50 chars, required)"
}
```

**Response** `201 Created`:
```json
{
  "gameId": 1,
  "gameCode": "XKCD42",
  "playerCode": "uuid-string",
  "player": {
    "id": "uuid-string",
    "name": "HostPlayer",
    "status": "ACTIVE"
  }
}
```

**Errors**:
- `400 Bad Request` — Invalid or missing playerName

---

### GET /api/games/{gameCode}

Get current game state (lobby, status, player list).

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "gameId": 1,
  "gameCode": "XKCD42",
  "isGameActive": true,
  "gameStatus": "INIT",
  "currentStage": "IN_START",
  "round": -1,
  "currentTask": null,
  "playerCount": 5,
  "players": [
    {
      "id": "uuid-1",
      "name": "Player1",
      "status": "ACTIVE",
      "isDead": false
    }
  ],
  "startGameTime": null,
  "endGameTime": null
}
```

**Note**: `isTraitor` and `card` are NEVER included in the player list response (role and card are private per player).

**Errors**:
- `404 Not Found` — Invalid gameCode
- `403 Forbidden` — Player not in this game

---

### POST /api/games/{gameCode}/start

VIP triggers game start. Only callable by the VIP player.

**Headers**: `X-Player-Code: {vipPlayerCode}`

**Response** `200 OK`:
```json
{
  "gameStatus": "START",
  "currentStage": "NORMAL_ROUNDS",
  "playerCount": 8,
  "message": "Game started! Roles have been assigned."
}
```

**Errors**:
- `400 Bad Request` — Fewer than 4 active players
- `403 Forbidden` — Caller is not the VIP
- `409 Conflict` — Game already started

---

## Player & Invitation

### POST /api/games/{gameCode}/invite

Send an invitation to a player via email or SMS.

**Headers**: `X-Player-Code: {vipPlayerCode}`

**Request**:
```json
{
  "name": "string (1-50 chars, required)",
  "contact": "string (email or phone, required)"
}
```

**Response** `201 Created`:
```json
{
  "playerId": "uuid-string",
  "name": "InvitedPlayer",
  "contact": "player@example.com",
  "contactType": "EMAIL",
  "status": "PENDING",
  "invitationSent": true
}
```

**Errors**:
- `400 Bad Request` — Invalid contact format or missing fields
- `403 Forbidden` — Caller is not the VIP
- `409 Conflict` — Player with this contact already invited

---

### GET /api/join/{gameCode}

Player clicks invitation link. Returns join confirmation page data.

**Response** `200 OK`:
```json
{
  "gameCode": "XKCD42",
  "gameName": "The Heartless Game",
  "hostName": "VIPPlayer",
  "playerCount": 3,
  "status": "INIT"
}
```

**Errors**:
- `404 Not Found` — Invalid gameCode
- `410 Gone` — Game already started or ended

---

### POST /api/join/{gameCode}

Confirm joining a game. Returns player credentials.

**Request**:
```json
{
  "name": "string (1-50 chars, required)",
  "contact": "string (email or phone used in invitation, required)"
}
```

**Response** `200 OK`:
```json
{
  "playerCode": "uuid-string",
  "player": {
    "id": "uuid-string",
    "name": "JoinedPlayer",
    "status": "ACTIVE"
  },
  "gameCode": "XKCD42"
}
```

**Errors**:
- `404 Not Found` — Invalid gameCode or no matching invitation
- `409 Conflict` — Player already joined
- `410 Gone` — Game already started or ended

---

## Player Info

### GET /api/games/{gameCode}/me

Get current player's private info (role, card, items).

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "id": "uuid-string",
  "name": "Player1",
  "status": "ACTIVE",
  "isDead": false,
  "isTraitor": true,
  "card": {
    "suit": "HEART",
    "number": "QUEEN",
    "imageUrl": "/cards/heart-queen.png"
  },
  "items": [
    {
      "name": "Shield",
      "usable": true
    }
  ]
}
```

**Errors**:
- `403 Forbidden` — Invalid player code
- `404 Not Found` — Game not found

---

## Chat

### POST /api/games/{gameCode}/chat/{channel}/messages

Send a message to a chat channel.

**Headers**: `X-Player-Code: {playerCode}`

**Path Parameters**:
- `channel`: One of `all`, `traitors`, `dead`, `individual`

**Request**:
```json
{
  "text": "string (1-500 chars, required)",
  "recipientId": "string (required only for 'individual' channel)"
}
```

**Response** `201 Created`:
```json
{
  "messageId": "uuid-string",
  "channel": "all",
  "senderId": "uuid-sender",
  "senderName": "Player1",
  "text": "Hello everyone!",
  "timestamp": "2026-03-13T15:30:00Z"
}
```

**Errors**:
- `400 Bad Request` — Empty text, or missing recipientId for individual channel
- `403 Forbidden` — Player not eligible for this channel (e.g., non-traitor accessing traitors channel, dead player accessing all chat)
- `404 Not Found` — Game or recipient not found

---

### GET /api/games/{gameCode}/chat/{channel}/messages

Retrieve messages from a chat channel (polling endpoint).

**Headers**: `X-Player-Code: {playerCode}`

**Query Parameters**:
- `since` (optional): ISO-8601 timestamp. Only return messages after this time.

**Response** `200 OK`:
```json
{
  "channel": "all",
  "messages": [
    {
      "messageId": "uuid-string",
      "senderId": "uuid-sender",
      "senderName": "Player1",
      "text": "Hello everyone!",
      "timestamp": "2026-03-13T15:30:00Z"
    }
  ]
}
```

**Channel Access Rules**:
| Channel      | Eligible Players                          |
|--------------|-------------------------------------------|
| `all`        | All alive players                         |
| `traitors`   | Traitors only                             |
| `dead`       | Dead players only                         |
| `individual` | Any two alive players (filtered by query) |

**Errors**:
- `403 Forbidden` — Player not eligible for channel

---

## Voting

### GET /api/games/{gameCode}/vote/banish

Get the banish vote options for the current player.

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "voteType": "BANISH",
  "votingEnabled": true,
  "candidates": [
    {
      "id": "uuid-1",
      "name": "Player2"
    },
    {
      "id": "uuid-2",
      "name": "Player3"
    }
  ],
  "existingVote": null
}
```

**Note**: The requesting player is excluded from the candidates list.

**Errors**:
- `403 Forbidden` — Player not eligible to vote (dead, or not in game)
- `409 Conflict` — Not currently in a voting phase

---

### POST /api/games/{gameCode}/vote/banish

Cast a banish vote.

**Headers**: `X-Player-Code: {playerCode}`

**Request**:
```json
{
  "targetPlayerId": "uuid-string (required)"
}
```

**Response** `200 OK`:
```json
{
  "voteRecorded": true,
  "castingPlayerId": "uuid-voter",
  "targetPlayerId": "uuid-target"
}
```

**Errors**:
- `400 Bad Request` — Target player is self, dead, or not in game
- `403 Forbidden` — Player not eligible to vote
- `409 Conflict` — Not in voting phase, or vote already cast

---

### GET /api/games/{gameCode}/vote/murder

Get murder vote options (traitors only).

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "voteType": "MURDER",
  "votingEnabled": true,
  "candidates": [
    {
      "id": "uuid-1",
      "name": "FaithfulPlayer1"
    },
    {
      "id": "uuid-2",
      "name": "FaithfulPlayer2"
    }
  ],
  "existingVotes": []
}
```

**Note**: Only non-traitor, alive players appear as candidates. Traitors are excluded.

**Errors**:
- `403 Forbidden` — Player is not a traitor
- `409 Conflict` — Not in murder vote phase

---

### POST /api/games/{gameCode}/vote/murder

Cast a murder vote (traitors only).

**Headers**: `X-Player-Code: {playerCode}`

**Request**:
```json
{
  "targetPlayerIds": ["uuid-1", "uuid-2"]
}
```

**Note**: Multiple targets allowed per spec (to be reworked later).

**Response** `200 OK`:
```json
{
  "voteRecorded": true,
  "castingPlayerId": "uuid-traitor",
  "targetPlayerIds": ["uuid-1", "uuid-2"]
}
```

**Errors**:
- `400 Bad Request` — Target includes self, a traitor, or dead player
- `403 Forbidden` — Player is not a traitor
- `409 Conflict` — Not in murder vote phase

---

## Game State (Round Info)

### GET /api/games/{gameCode}/round

Get current round information.

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "roundNumber": 3,
  "phase": "VOTING",
  "murderRevealed": true,
  "miniGamePlayed": true,
  "miniGameWinner": {
    "id": "uuid-1",
    "name": "Player2"
  },
  "banishVoteResult": null,
  "murderResult": null
}
```

**Note**: Vote results and murder outcomes are only included after the reveal phase.

**Errors**:
- `403 Forbidden` — Player not in game
- `404 Not Found` — No active round (game in INIT or OVER)

---

## Menu State

### GET /api/games/{gameCode}/menu

Get the menu state for the current player (which buttons are enabled/visible).

**Headers**: `X-Player-Code: {playerCode}`

**Response** `200 OK`:
```json
{
  "gameStatus": "START",
  "playerName": "Player1",
  "isDead": false,
  "isTraitor": true,
  "menuItems": [
    { "id": "traitor-chat", "label": "Traitor Chat", "enabled": true, "visible": true },
    { "id": "all-chat", "label": "All Chat", "enabled": true, "visible": true },
    { "id": "banish-vote", "label": "Banish Vote", "enabled": false, "visible": true },
    { "id": "murder-vote", "label": "Murder Vote", "enabled": false, "visible": true },
    { "id": "individual-chat", "label": "Individual Chat", "enabled": true, "visible": true },
    { "id": "actions", "label": "Actions", "enabled": false, "visible": true },
    { "id": "game-logs", "label": "Game Logs", "enabled": false, "visible": true }
  ]
}
```

**Visibility Rules**:
- Traitor Chat: `visible` only if player `isTraitor`
- Murder Vote: `visible` only if player `isTraitor`
- All Chat: `visible` only if player is alive
- Dead Players Chat: `visible` only if player `isDead`
- Actions, Game Logs: always `visible`, always `enabled: false` (stubs)

**Errors**:
- `403 Forbidden` — Invalid player code
- `404 Not Found` — Game not found
