# Research: Vote Page User Selection State

**Phase 0 — All NEEDS CLARIFICATION resolved**
**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31

---

## Decision 1: Where to store `UserSelectionsState` per player

**Decision**: Store a `Map<String, UserSelectionsState>` directly on `GameObject` (keyed by player ID), scoped to the current active vote event. The map is initialized when a vote event begins (via `GameObject.initSelectionStates(playerIds)`) and cleared when the event ends.

**Rationale**: `GameObject` is already the single authoritative in-memory state container for all game data (players, rounds, events, menu state). Adding selection state here is consistent with the existing pattern and avoids introducing a new service or secondary store. The map is accessible from WebSocket handlers (via `GameStore.getGame()`) and from event implementations (via `getGame()` → now returning the game through `getUsersSelections()`).

**Alternatives considered**:
- *Dedicated `SelectionStateService` bean*: Rejected — adds indirection for data that naturally belongs to the game object. Would also require the new service to be injected wherever the map is needed.
- *Storing inside each `Vote` object*: Rejected — `Vote` represents a final committed vote; selection state is pre-commit. Conflating them would create ambiguity about what constitutes a "vote."

---

## Decision 2: Lifecycle of the selection state map

**Decision**: `GameObject.initSelectionStates(List<String> playerIds)` clears and repopulates the map at the start of each vote event. The map is not cleared when the event ends — it is overwritten by the next `initSelectionStates()` call. This means after a vote event, the last selection state is readable by the game thread for result resolution.

**Rationale**: Leaving state readable after the event ends satisfies the spec assumption that "when a vote event ends while `SubmitPressed` is `false`, their `SelectedItems` is used as their final answer." Clearing on next init is simpler than explicitly clearing on event end (which would require hooking into the event teardown path).

**Alternatives considered**:
- *Auto-clear on event end*: Rejected — prevents reading state in the post-event window for result calculation.
- *Append to history (list of maps)*: Rejected — over-engineering; no requirement to keep historical selection states.

---

## Decision 3: How `EventObjectInterface.getUsersSelections()` is implemented

**Decision**: `getUsersSelections()` replaces `getGame()` in the interface contract. Each event implementation holds a reference to `GameObject` (already the case). `getUsersSelections()` is implemented as:
```java
public ArrayList<UserSelectionsState> getUsersSelections() {
    return new ArrayList<>(game.getSelectionStateMap().values());
}
```
This is a one-liner in each of the 10 event classes, making it trivially compliant with the Method Simplicity principle (≤20 lines, single responsibility).

**Note on `getGame()` callers**: A search of the codebase shows `getGame()` through the `EventObjectInterface` reference is **never called outside the event implementations themselves** — all callers use `GameObject` directly (from `GameStore`, `GameService`, `VotingService`, etc.). Removing `getGame()` from the interface is therefore safe with zero impact on existing call sites.

**Rationale**: Replacing (not adding alongside) `getGame()` keeps the interface clean. Callers that need `GameObject` get it directly from `GameStore`, not through the event interface.

**Alternatives considered**:
- *Keep `getGame()` and add `getUsersSelections()`*: Rejected per spec requirement. Also unnecessary since no external caller uses `getGame()` via the interface reference.
- *Return `Map<String, UserSelectionsState>` instead of `ArrayList`*: Rejected — spec explicitly specifies `ArrayList<UserSelectionsState>`.

---

## Decision 4: How selection messages are persisted (the missing backend step)

**Decision**: The two existing WebSocket `@MessageMapping` handlers (`/games/{gameCode}/murder-selection` in `VoteController` and the banish-selection handler that **does not yet exist**) are updated to:
1. Persist the selection change to `GameObject.getSelectionStateMap().get(playerId).setSelectedItems(...)`.
2. Broadcast to the topic (as currently done for murder; to be implemented for banish).

A new `@MessageMapping` for `/games/{gameCode}/banish-selection` is added to `VoteController` (currently **missing from the backend** — the frontend sends to this destination but no handler processes it on the server).

**Rationale**: The frontend already sends WebSocket messages for both banish and murder selections on every toggle. The backend only had a handler for murder selection — this was the gap. Adding the banish handler and persistence to both handlers closes the loop with no frontend changes required.

**Alternatives considered**:
- *Add a REST endpoint for each interaction*: Rejected — the WebSocket path is already wired for selection broadcasts. Adding REST would duplicate the channel and increase per-interaction latency.
- *Persist on poll rather than on WebSocket*: Rejected — would require the frontend to include selection state in every poll response, coupling concerns.

---

## Decision 5: `SubmitPressed` irreversibility enforcement

**Decision**: The setter on `UserSelectionsState` is:
```java
public void setSubmitPressed(boolean submitPressed) {
    if (this.submitPressed) return; // once true, never revert
    this.submitPressed = submitPressed;
}
```
This enforces the spec constraint at the object level.

**Rationale**: Spec FR-004 requires `SubmitPressed` to never reset to `false` once set. Encoding this in the setter means no caller can accidentally revert it.

**Alternatives considered**:
- *Enforce in the calling service only*: Rejected — defensive programming at the model level is safer and explicit (Principle IV).

---

## Decision 6: Thread safety

**Decision**: `UserSelectionsState` fields are mutated from STOMP handler threads (spring WebSocket executor pool). `ArrayList<String>` for `SelectedItems` will use a `synchronized` block or be replaced with `Collections.synchronizedList()` since multiple threads could theoretically update the same player's state (e.g., rapid clicks). `TextFieldInput` and `SubmitPressed` are single scalars — their setters are fine as `synchronized` methods.

**Rationale**: Lightweight approach consistent with the existing codebase pattern (`banishVotes` and `murderVotes` in `VotingService` use `Collections.synchronizedList()`).

**Alternatives considered**:
- *`CopyOnWriteArrayList`*: Viable but heavier; for small lists (≤15 players) synchronized list is sufficient.
- *Ignoring thread safety*: Rejected — WebSocket handlers run on a thread pool; concurrent updates are realistic during rapid user interactions.
