# Data Model: Traitors Game Core

**Branch**: `001-traitors-game-core` | **Date**: 2026-03-13  
**Input**: [spec.md](spec.md) Key Entities + [research.md](research.md) decisions

---

## Enums

### GameStatusEnum

Tracks the overall lifecycle phase of a game.

| Value  | Description                                          |
|--------|------------------------------------------------------|
| `INIT` | Game created, lobby open, awaiting players           |
| `START`| Game in progress, rounds executing                   |
| `END`  | Final phase — end votes and reveals in progress      |
| `OVER` | Game complete, no further actions allowed             |

**Transitions**: `INIT → START → END → OVER`

### GameStageEnum

Tracks the current stage within the game loop.

| Value           | Description                                    |
|-----------------|------------------------------------------------|
| `IN_START`      | Pre-game / lobby phase                         |
| `NORMAL_ROUNDS` | Standard gameplay rounds                       |
| `FINAL_ROUND`   | Final round triggered by end criteria          |

### CardSuit

| Value     |
|-----------|
| `SPADE`   |
| `CLUB`    |
| `HEART`   |
| `DIAMOND` |

### CardNumber

| Value   | Display |
|---------|---------|
| `ACE`   | 1       |
| `TWO`   | 2       |
| `THREE` | 3       |
| `FOUR`  | 4       |
| `FIVE`  | 5       |
| `SIX`   | 6       |
| `SEVEN` | 7       |
| `EIGHT` | 8       |
| `NINE`  | 9       |
| `TEN`   | 10      |
| `JACK`  | 11      |
| `QUEEN` | 12      |
| `KING`  | 13      |

### MurderResultEnum

| Value       | Description                                    |
|-------------|------------------------------------------------|
| `SUCCESSFUL`| Murder carried out, player eliminated          |
| `BLOCKED`   | Murder prevented by a Shield item              |
| `DEFLECTED` | Murder redirected to another player            |

### PlayerStatusEnum

| Value          | Description                              |
|----------------|------------------------------------------|
| `PENDING`      | Invited but has not joined               |
| `ACTIVE`       | In game, connected                       |
| `DISCONNECTED` | In game but lost connection              |
| `REMOVED`      | Removed from game by VIP or system       |

---

## Entities

### Player

The participant in a game — both VIP (host) and invited players.

| Field       | Type               | Description                                           | Constraints                |
|-------------|--------------------|-------------------------------------------------------|----------------------------|
| `id`        | String             | Unique player identifier (UUID)                       | Not null, unique per game  |
| `name`      | String             | Display name                                          | Not null, 1-50 chars       |
| `email`     | String             | Email address (nullable if phone provided)            | Valid email format or null |
| `phone`     | String             | Phone number (nullable if email provided)             | E.164 format or null       |
| `status`    | PlayerStatusEnum   | Current player status                                 | Not null, default PENDING  |
| `isDead`    | boolean            | Whether player has been eliminated                    | Default false              |
| `isTraitor` | boolean            | Whether player is a traitor                           | Default false              |
| `items`     | List\<ItemsInterface\> | Current inventory of game items                   | Not null, empty list default|
| `card`      | Card               | Assigned playing card (visual identity)               | Null until card assignment |

**Validation Rules**:
- At least one of `email` or `phone` must be non-null (needed for invitation delivery)
- `id` is generated server-side upon creation
- `isTraitor` is set only during traitor selection and never exposed to other players via API

**State Transitions**:
```
PENDING → ACTIVE     (player accepts invitation)
ACTIVE → DISCONNECTED (connection lost)
DISCONNECTED → ACTIVE (player reconnects)
ACTIVE → REMOVED     (VIP removes or player leaves)
```

---

### Card

A playing card assigned to each player as their visual identity.

| Field    | Type       | Description          | Constraints          |
|----------|------------|----------------------|----------------------|
| `suit`   | CardSuit   | Card suit            | Not null             |
| `number` | CardNumber | Card number/face     | Not null             |

**Validation Rules**:
- A Card is an immutable value object (suit + number set at construction, never changed)
- 52 unique cards in a standard deck (4 suits × 13 numbers)
- Within a game, each player receives a unique card — no duplicates

**Identity**: Two Cards are equal if and only if they share the same `suit` AND `number`.

---

### GameObject

Central game state container. One per active game session.

| Field            | Type                    | Description                                  | Constraints                         |
|------------------|-------------------------|----------------------------------------------|-------------------------------------|
| `gameId`         | Long                    | Unique game identifier (auto-generated)      | Not null, unique                    |
| `gameIdCode`     | String                  | Human-readable join code (e.g., "XKCD42")    | Not null, unique, 6 alphanumeric    |
| `isGameActive`   | boolean                 | Whether the game is currently active          | Default true                        |
| `playerList`     | List\<Player\>          | All players in the game                      | Not null, min 4, max 52            |
| `roundList`      | List\<RoundObject\>     | Completed and current rounds                 | Not null, empty initially           |
| `currentStage`   | GameStageEnum           | Current game stage                           | Not null, default IN_START          |
| `round`          | int                     | Current round number                         | -1 at start, 0 at final, 1+ normal |
| `currentTask`    | String                  | Description of current game activity         | Nullable                            |
| `eventLog`       | List\<EventObjectInterface\> | History of all events executed          | Not null, empty initially           |
| `startGameTime`  | Long                    | Game start timestamp (epoch millis)          | Null until game starts              |
| `endGameTime`    | Long                    | Game end timestamp (epoch millis)            | Null until game ends                |
| `lastMurderId`   | String                  | Player ID of last murdered player            | Nullable                            |

**Validation Rules**:
- `gameIdCode` is generated as a 6-character uppercase alphanumeric string at game creation
- `playerList` must have between 4 and 52 players before transitioning from INIT to START
- `round` starts at -1 (lobby), increments to 1 on first normal round, set to 0 for finale

**State Transitions**:
```
Created (INIT, round=-1) → Started (START, round=1+) → Ending (END, round=0) → Finished (OVER)
```

---

### RoundObject

Captures all events and outcomes for a single game round.

| Field               | Type              | Description                                      | Constraints            |
|---------------------|-------------------|--------------------------------------------------|------------------------|
| `roundNumber`       | int               | Sequential round number                          | ≥ 1                   |
| `murderRevealed`    | boolean           | Whether the previous round's murder was revealed | Default false          |
| `miniGamePlayed`    | boolean           | Whether a mini-game was played this round        | Default false          |
| `miniGameWinner`    | Player            | Player who won the mini-game                     | Nullable               |
| `banishVoteResult`  | VoteResultObject  | Result of the banishment vote                    | Nullable until vote    |
| `playerBanished`    | Player            | Player banished this round                       | Nullable               |
| `murderVoteResult`  | VoteResultObject  | Result of the murder vote                        | Nullable until vote    |
| `murderResult`      | MurderResultEnum  | Outcome of the murder attempt                    | Nullable until resolved|
| `murderedPlayer`    | Player            | Player murdered this round                       | Nullable               |

**Validation Rules**:
- `roundNumber` must be unique within a game and sequential
- `playerBanished` is set after the banish vote is tallied
- `murderedPlayer` is set only if `murderResult` is SUCCESSFUL

---

### Vote

A single vote cast by one player targeting another.

| Field            | Type   | Description                     | Constraints            |
|------------------|--------|---------------------------------|------------------------|
| `castingPlayer`  | Player | The player who cast the vote    | Not null               |
| `receivingPlayer`| Player | The player being voted for      | Not null               |

**Validation Rules**:
- `castingPlayer` cannot equal `receivingPlayer` (no self-votes)
- For banish votes: `castingPlayer` must be alive and active
- For murder votes: `castingPlayer` must be a traitor; `receivingPlayer` must NOT be a traitor

---

### VoteResultObject

Aggregation of all votes for a single voting round.

| Field      | Type             | Description                | Constraints            |
|------------|------------------|----------------------------|------------------------|
| `voteList` | List\<Vote\>     | All cast votes             | Not null               |

**Derived Properties**:
- `getWinner()`: Player with the most votes (or null if tie)
- `getTally()`: Map\<Player, Integer\> of vote counts
- `isTie()`: Whether top two players have equal votes

---

### GameThread

Orchestrator that manages the full game lifecycle.

| Field                | Type                           | Description                                 |
|----------------------|--------------------------------|---------------------------------------------|
| `gameObject`         | GameObject                     | The game state being orchestrated           |
| `gameCriteriaObject` | GameCriteriaObject             | Conditions for continuing/ending the game   |
| `eventList`          | List\<EventObjectInterface\>   | Sequence of events in the current phase     |
| `gameStatusEnum`     | GameStatusEnum                 | Current lifecycle status                    |
| `statusString`       | String                         | Human-readable status message               |

**Methods**:
- `gameInit()`: Create GameObject, set status to INIT, run LobbyEvent
- `gameStart()`: Set status to START, loop rounds while criteria met
- `gameEnd()`: Set status to END, run final votes and reveals, set to OVER

---

### GameCriteriaObject

Evaluates whether the game should continue or end.

| Field    | Type               | Description                                 |
|----------|--------------------|---------------------------------------------|
| `game`   | GameObject         | Reference to the game being evaluated       |

**Methods**:
- `checkGameConditions(GameObject)`: Returns boolean — true if game should continue
- `checkEndConditions(GameObject)`: Returns boolean — true if end phase should continue

---

## Interfaces

### EventObjectInterface

Contract for all game events.

| Method             | Return   | Description                                         |
|--------------------|----------|-----------------------------------------------------|
| `checkStartConditions()` | boolean | Whether conditions are met to start this event |
| `checkEndConditions()`   | boolean | Whether conditions are met to end this event   |
| `getGame()`              | GameObject | The game this event belongs to              |
| `execute()`              | void    | Run the event logic                               |

**Implementations**: LobbyEvent, VoteEvent, MurderEvent, MiniGameEvent, TieBreakEvent, VoteRevealEvent, RecruitEvent, RevelMurderEvent, PreVoteEvent

---

### ConditionInterface

Contract for evaluating game conditions.

| Method                          | Return  | Description                                  |
|---------------------------------|---------|----------------------------------------------|
| `checkGameConditions(GameObject)` | boolean | Evaluate the condition against game state  |

**Implementations**: NumberOfPlayerCondition, TimeCondition

---

### ChannelObjectInterface

Contract for communication channels.

| Method                        | Return          | Description                                |
|-------------------------------|-----------------|--------------------------------------------|
| `getEligiblePlayers(GameObject)` | List\<Player\> | Players who can participate in this channel |
| `sendMessage(String, Player)` | void            | Send a message to the channel              |
| `getMessages()`               | List\<ChatMessage\> | Retrieve channel message history       |

**Implementations**: AllPlayersChannel, DeadPlayersChannel, TraitorsChannel, NonTraitorsChannel

---

### ItemsInterface

Contract for game items that affect gameplay.

| Method          | Return  | Description                                 |
|-----------------|---------|---------------------------------------------|
| `getName()`     | String  | Display name of the item                    |
| `use(Player, GameObject)` | void | Apply the item's effect             |
| `isUsable(Player, GameObject)` | boolean | Whether the item can be used now |

**Implementations**: Shield, Knife, Tether, Duel, Lease

---

### MessageSenderInterface

Contract for sending notifications (email/SMS).

| Method                                    | Return | Description                           |
|-------------------------------------------|--------|---------------------------------------|
| `send(String to, String subject, String body)` | void | Send a message to a recipient      |

**Implementations**: EmailMessageSender, SmsMessageSender

---

## Entity Relationships

```
GameThread 1──1 GameObject
GameThread 1──1 GameCriteriaObject
GameThread 1──* EventObjectInterface

GameObject 1──* Player
GameObject 1──* RoundObject
GameObject *──* EventObjectInterface (eventLog)

Player 1──1 Card (nullable until assigned)
Player 1──* ItemsInterface

RoundObject 1──1 VoteResultObject (banishVoteResult)
RoundObject 1──1 VoteResultObject (murderVoteResult)
RoundObject 0..1──1 Player (miniGameWinner)
RoundObject 0..1──1 Player (playerBanished)
RoundObject 0..1──1 Player (murderedPlayer)

VoteResultObject 1──* Vote
Vote 1──1 Player (castingPlayer)
Vote 1──1 Player (receivingPlayer)

ChannelObjectInterface ──> GameObject (filters players)
ChatSenderObject ──> ChannelObjectInterface (sends via channel)
InvitationService ──> MessageSenderInterface (sends via email/SMS)
```
