# Data Model: Real-Time Voting & Game Page

**Feature**: 002-realtime-voting-page  
**Date**: 2026-03-28  
**Source**: [spec.md](spec.md) Key Entities + [research.md](research.md) decisions

---

## Entity: GameEventConfig

**Purpose**: Immutable configuration defining a single game/vote event instance.

| Field | Type | Constraints | Description |
|---|---|---|---|
| eventId | String | Non-null, unique per game | Unique identifier for this event instance |
| gameCode | String | Non-null | Game this event belongs to |
| title | String | Non-null, non-empty | Display title at top of page |
| prompt | String | Nullable | Optional instructional text below title; null means do not render |
| listOfItems | List\<String\> | Non-null, may be empty | Items displayed as toggle buttons |
| singleAnswer | boolean | — | If true, only one item selectable at a time |
| showOthersSelections | boolean | — | If true, other players' selections visible in real-time |
| minNumberSelectedToSubmit | int | ≥ 0 | Submit disabled if fewer than this many selected |
| maxNumberSelectedToSubmit | int | ≥ 0 (0 = no limit) | Submit disabled if more than this many selected; 0 means unlimited |
| endTime | long | Epoch millis, > 0 | When the event auto-resolves |
| inputString | boolean | — | If true, a text input field is rendered and required for submit |
| playersMustAgree | boolean | — | If true, all players must submit matching selections |

**Validation Rules**:
- `title` must not be blank
- `listOfItems` must not be null (empty list is valid — renders no buttons)
- `minNumberSelectedToSubmit` must be ≤ `listOfItems.size()` (if listOfItems is non-empty)
- `maxNumberSelectedToSubmit` must be ≥ `minNumberSelectedToSubmit` (when max > 0)
- `endTime` must be in the future at creation time

**Relationships**: 
- Belongs to one game (via `gameCode`)
- Referenced by all `PlayerSelection` entries for this event
- Referenced by `GameEventResult` when resolved

---

## Entity: PlayerSelection

**Purpose**: Tracks a single player's current state within an active game event.

| Field | Type | Constraints | Description |
|---|---|---|---|
| playerId | String | Non-null | Player who owns this selection |
| playerName | String | Non-null | Display name for status indicators |
| selectedItems | Set\<String\> | Non-null (may be empty) | Currently selected item labels from listOfItems |
| textInput | String | Nullable | Text field value; null if inputString is false or not yet entered |
| submissionStatus | SubmissionStatus | Non-null | Current submission state |

**SubmissionStatus Enum Values**:
| Value | Meaning |
|---|---|
| NONE | Player has not selected anything yet |
| SELECTED | Player has made selection(s) but not submitted |
| SUBMITTED | Player has finalized and submitted |

**State Transitions**:
```
NONE → SELECTED          (player taps a toggle button)
SELECTED → SELECTED      (player changes selection)
SELECTED → SUBMITTED     (player taps Submit)
SUBMITTED → SELECTED     (player taps "Cancel Submit" — only when playersMustAgree=true)
SUBMITTED → NONE         (disagreement reset — all players' statuses reset)
Any → frozen             (event resolved — no further transitions)
```

**Validation Rules**:
- `selectedItems` values must be members of `GameEventConfig.listOfItems`
- Transition to SUBMITTED requires: selection count within min/max range AND (if inputString true) textInput is non-blank
- Transition from SUBMITTED to SELECTED is only allowed when `playersMustAgree` is true

**Relationships**:
- Belongs to one `GameEventConfig` (via gameCode + eventId)
- Belongs to one Player (via playerId)

---

## Entity: GameEventState

**Purpose**: Server-side aggregate holding the full state of an active game event — the config plus all player selections.

| Field | Type | Constraints | Description |
|---|---|---|---|
| config | GameEventConfig | Non-null | The event configuration |
| selections | Map\<String, PlayerSelection\> | Non-null | playerId → current selection state |
| resolved | boolean | — | Whether the event has been resolved |
| result | GameEventResult | Nullable | Populated once resolved |
| timeoutFuture | ScheduledFuture\<?\> | Nullable | Handle to cancel the timeout task if event resolves early |

**Concurrency**: All mutations to `selections` and `resolved` must be synchronized on this object to prevent race conditions during agreement checks and timeout resolution.

**Relationships**:
- Contains one `GameEventConfig`
- Contains 0..N `PlayerSelection` entries (one per player in the game)
- Produces one `GameEventResult` when resolved

---

## Entity: GameEventResult

**Purpose**: Immutable record of a resolved event's outcome.

| Field | Type | Constraints | Description |
|---|---|---|---|
| eventId | String | Non-null | Which event was resolved |
| gameCode | String | Non-null | Which game |
| finalSelections | Map\<String, PlayerSelection\> | Non-null | Snapshot of each player's final state |
| resolutionType | ResolutionType | Non-null | How the event was resolved |
| resolvedAt | long | Epoch millis | When the event was resolved |

**ResolutionType Enum Values**:
| Value | Meaning |
|---|---|
| ALL_SUBMITTED | All players submitted (playersMustAgree=false) |
| AGREEMENT_REACHED | All players submitted matching selections (playersMustAgree=true) |
| TIMEOUT | EndTime reached; resolved using saved state |

**Relationships**:
- Belongs to one `GameEventState`
- Contains snapshot of all `PlayerSelection` entries at resolution time

---

## Entity Relationship Diagram

```
┌──────────────────┐
│  GameEventConfig │
│  (immutable)     │
│                  │
│  eventId (PK)    │
│  gameCode (FK)   │
│  title           │
│  prompt          │
│  listOfItems     │
│  singleAnswer    │
│  showOthers...   │
│  minSelected     │
│  maxSelected     │
│  endTime         │
│  inputString     │
│  playersMustAgree│
└────────┬─────────┘
         │ 1
         │
         ▼ 1
┌──────────────────┐       ┌──────────────────┐
│  GameEventState  │──────▶│  GameEventResult │
│  (mutable)       │  0..1 │  (immutable)     │
│                  │       │                  │
│  config          │       │  eventId         │
│  selections{}    │       │  gameCode        │
│  resolved        │       │  finalSelections │
│  result          │       │  resolutionType  │
│  timeoutFuture   │       │  resolvedAt      │
└────────┬─────────┘       └──────────────────┘
         │ 1
         │
         ▼ 0..N
┌──────────────────┐
│ PlayerSelection  │
│  (mutable)       │
│                  │
│  playerId (PK)   │
│  playerName      │
│  selectedItems{} │
│  textInput       │
│  submissionStatus│
└──────────────────┘
```

---

## Notes

- `GameEventState` is NOT persisted to a database — it lives in a `ConcurrentHashMap` in `GameEventService`, consistent with the existing `GameStore` pattern.
- `GameEventConfig` and `GameEventResult` are effectively immutable once created. `PlayerSelection` and `GameEventState` are mutable during the event lifecycle.
- The `timeoutFuture` field in `GameEventState` is an implementation detail for cancellation — it is not serialized or sent to clients.
