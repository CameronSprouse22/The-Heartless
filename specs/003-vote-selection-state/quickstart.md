# Quickstart: Vote Page User Selection State

**Branch**: `003-vote-selection-state` | **Date**: 2026-03-31

## What This Feature Does

Every time a player interacts with a vote page — clicking a candidate, typing in a text field, or pressing Submit — their current state is persisted in real-time on the backend. This lets game logic inspect every player's selections at any moment, not just after they submit.

## How to Build & Run

```bash
# Backend
cd backend
mvn clean install
mvn spring-boot:run

# Frontend (separate terminal)
cd backend/frontend
npm install
npm run dev
# → Vite dev server at http://localhost:5173 (proxies /api to :8080)
```

## How to Test This Feature Manually

1. Open the app and click **"Create Test Game"** → opens Test Dashboard.
2. Click **"Open All Players"** (allow popups first) to open all 7 player tabs.
3. From the VIP tab, start the game and navigate to a vote round.
4. In two different player tabs, click different candidates on the **Banish vote page**.
5. **Without pressing Submit**, observe the "Other Players' Selections" panel shows live updates.
6. In the backend logs or via a debugger watch, observe `GameObject.selectionStateMap` updating on each click.
7. Press **Submit** in one player's tab — that player's `submitPressed` should be `true` in the backend state.
8. After all players submit, verify the vote result uses the correct selections.

## Key Files Changed

| File | What Changed |
|------|-------------|
| `model/UserSelectionsState.java` | **New** — the POJO holding per-player vote interaction state |
| `model/GameObject.java` | **Modified** — `selectionStateMap` field + `initSelectionStates()`, `getSelectionState()`, `getSelectionStateMap()` |
| `event/EventObjectInterface.java` | **Modified** — `getGame()` → `getUsersSelections()` |
| `event/VoteEvent.java` (and 9 others) | **Modified** — implement `getUsersSelections()` |
| `controller/VoteController.java` | **Modified** — banish-selection handler added; both selection handlers persist to `GameObject`; vote-text handler added |
| `service/VotingService.java` | **Modified** — `castBanishVote()` and `castMurderVote()` set `submitPressed = true` |

## Running Tests

```bash
cd backend
mvn test
```

All existing tests must continue to pass. New tests in `UserSelectionsStateTest.java` cover:
- `setSubmitPressed(true)` then `setSubmitPressed(false)` → remains `true`
- `addSelectedItem` / `removeSelectedItem` round-trips
- `setSelectedItems(null)` is handled gracefully (stored as empty list)
- `setTextFieldInput(null)` is handled gracefully (stored as empty string)
- `getSelectedItems()` returns a defensive copy (mutations don't affect internal state)

## Architecture at a Glance

```
BanishVotePage (React)
  └─ toggle click
       └─ WebSocket send: /app/games/{code}/banish-selection
            └─ VoteController @MessageMapping
                 ├─ game.getSelectionState(playerId).setSelectedItems([id])   ← PERSIST
                 └─ messagingTemplate.send("/topic/.../banish-vote", broadcast) ← BROADCAST

VotingService.castBanishVote()
  └─ records Vote object (existing)
  └─ game.getSelectionState(voterId).setSubmitPressed(true)                   ← NEW

GameThread / EventObjectInterface
  └─ event.getUsersSelections()
       └─ new ArrayList<>(game.getSelectionStateMap().values())
```
