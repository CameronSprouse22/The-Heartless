# Tasks: Traitors Game Core

**Input**: Design documents from `/specs/001-traitors-game-core/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/rest-api.md, quickstart.md

**Tests**: Per The Heartless Constitution (Principle I: Testability First), ALL tasks involving method implementation MUST include corresponding unit tests. Tests MUST be written BEFORE implementation (Test-Driven Development).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/main/java/com/heartless/` for Java, `backend/frontend/src/` for React
- **Tests**: `backend/src/test/java/com/heartless/` mirroring source packages
- **Config**: `backend/src/main/resources/`, `backend/frontend/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, build configuration, and application boilerplate

- [X] T001 Create Maven project with pom.xml including Spring Boot 3.2 parent, spring-boot-starter-web, spring-boot-starter-mail, spring-boot-starter-test, mockito-core, and frontend-maven-plugin 1.15.1 configuration in backend/pom.xml
- [X] T002 [P] Create Spring Boot entry point with @SpringBootApplication annotation in backend/src/main/java/com/heartless/TheHeartlessApplication.java
- [X] T003 [P] Create application.properties with server port, mail configuration placeholders, and game defaults in backend/src/main/resources/application.properties
- [X] T004 [P] Initialize React Vite project with react-router-dom dependency, package.json, vite.config.js (API proxy to port 8080), main.jsx, and App.jsx with route shell in backend/frontend/
- [X] T005 [P] Create SpaController to forward non-API routes to index.html for React Router support in backend/src/main/java/com/heartless/controller/SpaController.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain models, interfaces, and shared infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 [P] Create all enum types per data-model.md (GameStatusEnum, GameStageEnum, CardSuit, CardNumber, MurderResultEnum, PlayerStatusEnum) in backend/src/main/java/com/heartless/model/enums/
- [X] T007 [P] Create Card model as immutable value object with equals/hashCode based on suit+number per data-model.md in backend/src/main/java/com/heartless/model/Card.java
- [X] T008 [P] Create Player model with all fields (id, name, email, phone, status, isDead, isTraitor, items, card) and validation per data-model.md in backend/src/main/java/com/heartless/model/Player.java
- [X] T009 [P] Create Vote model with castingPlayer/receivingPlayer and no-self-vote validation per data-model.md in backend/src/main/java/com/heartless/model/Vote.java
- [X] T010 [P] Create VoteResultObject model with voteList, getWinner(), getTally(), isTie() derived methods per data-model.md in backend/src/main/java/com/heartless/model/VoteResultObject.java
- [X] T011 Create GameObject model with all fields and state management (INIT→START→END→OVER transitions) per data-model.md in backend/src/main/java/com/heartless/model/GameObject.java
- [X] T012 [P] Create RoundObject model with all round tracking fields per data-model.md in backend/src/main/java/com/heartless/model/RoundObject.java
- [X] T013 [P] Create EventObjectInterface with checkStartConditions, checkEndConditions, getGame, execute methods in backend/src/main/java/com/heartless/event/EventObjectInterface.java
- [X] T014 [P] Create ConditionInterface with checkGameConditions(GameObject) method in backend/src/main/java/com/heartless/operation/condition/ConditionInterface.java
- [X] T015 [P] Create ChannelObjectInterface with getEligiblePlayers, sendMessage, getMessages methods in backend/src/main/java/com/heartless/operation/channel/ChannelObjectInterface.java
- [X] T016 [P] Create ItemsInterface with getName, use, isUsable methods in backend/src/main/java/com/heartless/operation/item/ItemsInterface.java
- [X] T017 [P] Create MessageSenderInterface with send(to, subject, body) method in backend/src/main/java/com/heartless/messaging/MessageSenderInterface.java
- [X] T018 Create GameStore @Service wrapping ConcurrentHashMap with getGame, putGame, removeGame, containsGame per research.md Topic 2 in backend/src/main/java/com/heartless/service/GameStore.java
- [X] T019 Write unit tests for Card (equality, immutability), Player (validation, state transitions), Vote (no-self-vote), VoteResultObject (winner, tally, tie), GameObject (state transitions), and GameStore (CRUD, thread safety) in backend/src/test/java/com/heartless/model/ and backend/src/test/java/com/heartless/service/GameStoreTest.java
- [X] T020 [P] Create frontend API service with base URL config, fetchJson helper, error handling, and placeholder method signatures for all endpoints in backend/frontend/src/services/api.js

**Checkpoint**: Foundation ready — all models compile, all interfaces defined, GameStore functional with passing tests. User story implementation can now begin in parallel.

---

## Phase 3: User Story 1 — VIP Creates Game and Invites Players (Priority: P1) 🎯 MVP

**Goal**: A VIP can create a new game (generating a unique code) and send invitations via email or SMS to other players.

**Independent Test**: Create a game via POST /api/games → get back gameCode and playerCode → invite a player via POST /api/games/{gameCode}/invite → verify invitation is queued.

### Tests for User Story 1 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T021 [P] [US1] Write unit tests for GameService.createGame (game code generation, VIP as first player, GameStore persistence) in backend/src/test/java/com/heartless/service/GameServiceTest.java
- [X] T022 [P] [US1] Write unit tests for InvitationService.sendInvitation (email dispatch, SMS dispatch, contact type detection) in backend/src/test/java/com/heartless/service/InvitationServiceTest.java

### Implementation for User Story 1

- [X] T023 [P] [US1] Create EmailMessageSender @Service stub implementing MessageSenderInterface (logs instead of sending) in backend/src/main/java/com/heartless/messaging/EmailMessageSender.java
- [X] T024 [P] [US1] Create SmsMessageSender @Service stub implementing MessageSenderInterface (logs instead of sending) in backend/src/main/java/com/heartless/messaging/SmsMessageSender.java
- [X] T025 [US1] Create InvitationService @Service with sendInvitation dispatching to email or SMS based on contact format per research.md Topic 3 in backend/src/main/java/com/heartless/service/InvitationService.java
- [X] T026 [US1] Create GameService @Service with createGame (generate 6-char code, create GameObject, add VIP player, store in GameStore) and invitePlayer methods in backend/src/main/java/com/heartless/service/GameService.java
- [X] T027 [US1] Create GameController @RestController with POST /api/games and POST /api/games/{gameCode}/invite endpoints per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/GameController.java
- [X] T028 [US1] Create LoginPage.jsx with create game form (player name input, submit) and invite player form (name, contact input) in backend/frontend/src/pages/LoginPage.jsx
- [X] T029 [US1] Add createGame() and invitePlayer() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: VIP can create a game and invite players. API returns game code and player code. Invitations are logged (stub senders).

---

## Phase 4: User Story 2 — Invited Player Joins the Game (Priority: P1)

**Goal**: An invited player clicks a join link, confirms joining, and is added to the game's player list with ACTIVE status.

**Independent Test**: With a game already created (US1), GET /api/join/{gameCode} returns join page data → POST /api/join/{gameCode} with matching name/contact returns playerCode and ACTIVE status.

### Tests for User Story 2 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T030 [P] [US2] Write unit tests for PlayerService (join success, duplicate rejection, invalid code, already-started game) in backend/src/test/java/com/heartless/service/PlayerServiceTest.java

### Implementation for User Story 2

- [X] T031 [US2] Create PlayerService @Service with joinGame (match invitation, activate player, duplicate detection) and getPlayerInfo (private role/card/items) in backend/src/main/java/com/heartless/service/PlayerService.java
- [X] T032 [US2] Create PlayerController @RestController with GET /api/join/{gameCode}, POST /api/join/{gameCode}, and GET /api/games/{gameCode}/me endpoints per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/PlayerController.java
- [X] T033 [US2] Update LoginPage.jsx with join game flow (enter game code or click link, confirm name/contact, receive playerCode) in backend/frontend/src/pages/LoginPage.jsx
- [X] T034 [US2] Add getJoinInfo(), joinGame(), and getPlayerInfo() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: Full invitation flow works end-to-end: VIP creates game → invites player → player joins via code → both visible in player list.

---

## Phase 5: User Story 3 — Lobby Readiness and Game Start (Priority: P2)

**Goal**: VIP sees all joined players in a lobby view and can trigger game start when minimum player count (4) is reached. All accepted players transition to the active game.

**Independent Test**: Create game, add 4+ players (via US1/US2) → GET /api/games/{gameCode} shows all players → POST /api/games/{gameCode}/start transitions status to START.

### Tests for User Story 3 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T035 [P] [US3] Write unit tests for GameService.startGame (min player validation, VIP-only check, status transition, already-started rejection) in backend/src/test/java/com/heartless/service/GameServiceTest.java

### Implementation for User Story 3

- [X] T036 [US3] Add getGameState() and startGame() methods to GameService with minimum 4-player validation and VIP authorization in backend/src/main/java/com/heartless/service/GameService.java
- [X] T037 [US3] Add GET /api/games/{gameCode} and POST /api/games/{gameCode}/start endpoints to GameController per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/GameController.java
- [X] T038 [P] [US3] Create PlayerList.jsx component displaying player names and statuses in backend/frontend/src/components/PlayerList.jsx
- [X] T039 [US3] Create LobbyPage.jsx with player list, player count, and VIP start-game button (disabled below 4 players) in backend/frontend/src/pages/LobbyPage.jsx
- [X] T040 [US3] Add getGameState() and startGame() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: Lobby displays all players with status. VIP can start game with 4+ players. Game transitions from INIT to START.

---

## Phase 6: User Story 4 — Traitor Selection and Card Assignment (Priority: P2)

**Goal**: When the game starts, traitors are selected based on player count (1 per 5, min 1) and every player receives a unique random card from a standard 52-card deck.

**Independent Test**: Call startGame with 10 players → verify 2 traitors selected → verify all 10 players have unique card assignments → verify traitor flag set privately.

### Tests for User Story 4 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T041 [P] [US4] Write unit tests for TraitorSelectionService (calculateTraitorCount for various player counts, selectTraitorIndices with seeded Random for determinism) in backend/src/test/java/com/heartless/service/TraitorSelectionServiceTest.java
- [X] T042 [P] [US4] Write unit tests for CardAssignmentService (deck has 52 unique cards, assignments match player count, no duplicate cards) in backend/src/test/java/com/heartless/service/CardAssignmentServiceTest.java

### Implementation for User Story 4

- [X] T043 [P] [US4] Create TraitorSelectionService with calculateTraitorCount(int) and selectTraitorIndices(int, Random) per research.md Topic 6 in backend/src/main/java/com/heartless/service/TraitorSelectionService.java
- [X] T044 [P] [US4] Create CardAssignmentService with generateShuffledDeck() and assignCards(List<Player>) per research.md Topic 5 in backend/src/main/java/com/heartless/service/CardAssignmentService.java
- [X] T045 [US4] Integrate TraitorSelectionService and CardAssignmentService into GameService.startGame() — select traitors, assign cards, set player flags in backend/src/main/java/com/heartless/service/GameService.java

**Checkpoint**: Game start assigns roles and cards. TraitorSelectionService and CardAssignmentService pass all tests with deterministic Random seeds.

---

## Phase 7: User Story 5 — Player Menu and Chat System (Priority: P3)

**Goal**: Each player sees a mobile-optimized menu with role-based button visibility. Players can send and receive messages in channel-based chat (All, Traitors, Dead, Individual) with their card image as background.

**Independent Test**: GET /api/games/{gameCode}/menu as traitor → traitor-chat visible. POST /api/games/{gameCode}/chat/all/messages → GET returns message. Faithful player cannot access traitors channel (403).

### Tests for User Story 5 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T046 [P] [US5] Write unit tests for channel implementations (AllPlayersChannel filters alive, TraitorsChannel filters traitors only, DeadPlayersChannel filters dead only, NonTraitorsChannel filters faithful) in backend/src/test/java/com/heartless/operation/channel/
- [X] T047 [P] [US5] Write unit tests for ChatService (send to valid channel, reject unauthorized access, retrieve messages with since filter) in backend/src/test/java/com/heartless/service/ChatServiceTest.java

### Implementation for User Story 5

- [X] T048 [P] [US5] Create AllPlayersChannel filtering alive players in backend/src/main/java/com/heartless/operation/channel/AllPlayersChannel.java
- [X] T049 [P] [US5] Create TraitorsChannel filtering traitor players only in backend/src/main/java/com/heartless/operation/channel/TraitorsChannel.java
- [X] T050 [P] [US5] Create DeadPlayersChannel filtering dead players only in backend/src/main/java/com/heartless/operation/channel/DeadPlayersChannel.java
- [X] T051 [P] [US5] Create NonTraitorsChannel filtering non-traitor players in backend/src/main/java/com/heartless/operation/channel/NonTraitorsChannel.java
- [X] T052 [US5] Create ChatService @Service with channel routing, message storage, access validation, and since-based retrieval in backend/src/main/java/com/heartless/service/ChatService.java
- [X] T053 [US5] Create ChatSenderObject delegating message delivery to appropriate channel in backend/src/main/java/com/heartless/service/ChatSenderObject.java
- [X] T054 [US5] Create ChatController @RestController with POST/GET /api/games/{gameCode}/chat/{channel}/messages per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/ChatController.java
- [X] T055 [US5] Add GET /api/games/{gameCode}/menu endpoint with role-based visibility rules to GameController per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/GameController.java
- [X] T056 [P] [US5] Create GameStatusBar.jsx component showing game status, round number, and current task in backend/frontend/src/components/GameStatusBar.jsx
- [X] T057 [P] [US5] Create ChatWindow.jsx reusable component with message list, input field, card-image background, and polling in backend/frontend/src/components/ChatWindow.jsx
- [X] T058 [US5] Create MenuPage.jsx with role-based button visibility (traitor chat for traitors, murder vote for traitors, dead chat for dead) per contracts/rest-api.md visibility rules in backend/frontend/src/pages/MenuPage.jsx
- [X] T059 [P] [US5] Create TraitorChatPage.jsx using ChatWindow for traitors channel in backend/frontend/src/pages/TraitorChatPage.jsx
- [X] T060 [P] [US5] Create AllChatPage.jsx using ChatWindow for all-players channel in backend/frontend/src/pages/AllChatPage.jsx
- [X] T061 [P] [US5] Create IndividualChatPage.jsx using ChatWindow with player-select dropdown for individual channel in backend/frontend/src/pages/IndividualChatPage.jsx
- [X] T062 [US5] Add getMenu(), sendChatMessage(), getChatMessages() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: Menu shows correct buttons per role. All four chat channels enforce access control. Messages persist and poll correctly.

---

## Phase 8: User Story 6 — Banish Vote (Priority: P3)

**Goal**: During the voting phase, all alive players can cast a banish vote against one other player. The voting screen excludes the voter and records one vote per player per round.

**Independent Test**: GET /api/games/{gameCode}/vote/banish → candidate list excludes self. POST vote → vote recorded. Second POST → rejected (already voted).

### Tests for User Story 6 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T063 [P] [US6] Write unit tests for VotingService banish logic (cast vote, self-vote rejection, duplicate vote rejection, dead player exclusion, vote tally) in backend/src/test/java/com/heartless/service/VotingServiceTest.java

### Implementation for User Story 6

- [X] T064 [US6] Create VotingService @Service with getBanishCandidates (exclude self, dead), castBanishVote (validate, record), and getBanishResult (tally) in backend/src/main/java/com/heartless/service/VotingService.java
- [X] T065 [US6] Create VoteController @RestController with GET/POST /api/games/{gameCode}/vote/banish per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/VoteController.java
- [X] T066 [P] [US6] Create VoteCard.jsx component displaying player name with selectable card style in backend/frontend/src/components/VoteCard.jsx
- [X] T067 [US6] Create BanishVotePage.jsx with candidate list (VoteCard components), single selection, and submit button in backend/frontend/src/pages/BanishVotePage.jsx
- [X] T068 [US6] Add getBanishCandidates() and castBanishVote() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: Banish vote screen shows correct candidates. Vote recorded and tallied. Duplicate and self-votes rejected.

---

## Phase 9: User Story 7 — Murder Vote (Priority: P3)

**Goal**: During the murder phase, only traitors can cast murder votes. The target list shows only alive non-traitor players. The murder result determines who is eliminated.

**Independent Test**: GET /api/games/{gameCode}/vote/murder as traitor → only non-traitor candidates shown. POST vote → recorded. GET as non-traitor → 403 Forbidden.

### Tests for User Story 7 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T069 [P] [US7] Write unit tests for VotingService murder logic (traitor-only access, non-traitor targets only, vote recording, non-traitor rejection) in backend/src/test/java/com/heartless/service/VotingServiceTest.java

### Implementation for User Story 7

- [X] T070 [US7] Add getMurderCandidates (exclude traitors, dead), castMurderVote (validate traitor-only), and getMurderResult to VotingService in backend/src/main/java/com/heartless/service/VotingService.java
- [X] T071 [US7] Add GET/POST /api/games/{gameCode}/vote/murder endpoints to VoteController per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/VoteController.java
- [X] T072 [US7] Create MurderVotePage.jsx with traitor-only access guard, non-traitor candidate list, and multi-select submission in backend/frontend/src/pages/MurderVotePage.jsx
- [X] T073 [US7] Add getMurderCandidates() and castMurderVote() API calls to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: Murder vote accessible only to traitors. Correct candidate filtering. Votes recorded and tallied.

---

## Phase 10: User Story 8 — Game Thread and Round Lifecycle (Priority: P4)

**Goal**: GameThread orchestrates the full Init→Start→End→Over lifecycle. Each round executes events in order (murder reveal → mini-game → pre-vote → vote → tie-break → vote reveal → recruit → murder). GameCriteriaObject evaluates continuation conditions. All event and item classes exist as stubs.

**Independent Test**: Create GameThread with mock events → call gameInit() → verify INIT status and LobbyEvent triggered → call gameStart() → verify rounds loop with events in order → trigger end criteria → verify gameEnd() runs final sequence.

### Tests for User Story 8 (MANDATORY per Constitution)

> **Write these tests FIRST, ensure they FAIL before implementation**

- [X] T074 [P] [US8] Write unit tests for GameCriteriaObject (checkGameConditions with various player/traitor counts, checkEndConditions) in backend/src/test/java/com/heartless/gamethread/GameCriteriaObjectTest.java
- [X] T075 [P] [US8] Write unit tests for GameThread lifecycle (gameInit sets INIT, gameStart loops, gameEnd transitions to OVER) with mock events in backend/src/test/java/com/heartless/gamethread/GameThreadTest.java

### Implementation for User Story 8

- [X] T076 [US8] Create GameCriteriaObject with checkGameConditions (all traitors banished? traitors outnumber faithful?) and checkEndConditions in backend/src/main/java/com/heartless/gamethread/GameCriteriaObject.java
- [X] T077 [US8] Create GameThread with gameInit, gameStart (round loop with event sequence), and gameEnd lifecycle methods in backend/src/main/java/com/heartless/gamethread/GameThread.java
- [X] T078 [P] [US8] Create all event stubs implementing EventObjectInterface (LobbyEvent, VoteEvent, MurderEvent, MiniGameEvent, TieBreakEvent, VoteRevealEvent, RecruitEvent, RevelMurderEvent, PreVoteEvent) with stub execute() bodies in backend/src/main/java/com/heartless/event/
- [X] T079 [P] [US8] Create condition implementations (NumberOfPlayerCondition, TimeCondition) implementing ConditionInterface in backend/src/main/java/com/heartless/operation/condition/
- [X] T080 [P] [US8] Create item stubs implementing ItemsInterface (Shield, Knife, Tether, Duel, Lease) with stub use()/isUsable() bodies in backend/src/main/java/com/heartless/operation/item/
- [X] T081 [US8] Add GET /api/games/{gameCode}/round endpoint returning current round info to GameController per contracts/rest-api.md in backend/src/main/java/com/heartless/controller/GameController.java
- [X] T082 [P] [US8] Create ActionsPage.jsx stub with "Coming Soon" placeholder in backend/frontend/src/pages/ActionsPage.jsx
- [X] T083 [P] [US8] Create GameLogsPage.jsx stub with "Coming Soon" placeholder in backend/frontend/src/pages/GameLogsPage.jsx
- [X] T084 [US8] Add getRoundInfo() API call to frontend API service in backend/frontend/src/services/api.js

**Checkpoint**: GameThread lifecycle orchestrates events in correct order. All events, conditions, and items compile as stubs. Criteria evaluation works for common end-game scenarios.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, routing, styling, and validation across all user stories

- [X] T085 [P] Finalize React Router configuration with all page routes in backend/frontend/src/App.jsx
- [X] T086 [P] Add mobile-first responsive CSS styling in backend/frontend/src/ across all pages and components
- [X] T087 Run full Maven build (mvn clean package) and verify single executable JAR output
- [X] T088 Run quickstart.md validation — start server and execute all curl verification commands
- [X] T089 Constitution compliance review — verify all methods ≤30 lines, cyclomatic complexity ≤5, zero Spring imports in model/gamethread/event/operation packages

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Foundational (Phase 2)
- **US2 (Phase 4)**: Depends on US1 (Phase 3) — needs game creation to test joining
- **US3 (Phase 5)**: Depends on US2 (Phase 4) — needs players in lobby to test start
- **US4 (Phase 6)**: Depends on US3 (Phase 5) — traitor/card assignment triggers on game start
- **US5 (Phase 7)**: Depends on Foundational (Phase 2) — can run in parallel with US1-US4 if interfaces are available
- **US6 (Phase 8)**: Depends on Foundational (Phase 2) — voting logic is independent
- **US7 (Phase 9)**: Depends on US6 (Phase 8) — extends VotingService with murder logic
- **US8 (Phase 10)**: Depends on Foundational (Phase 2) — GameThread uses interfaces, independent of specific implementations
- **Polish (Phase 11)**: Depends on all user stories being complete

### User Story Dependencies

```
Phase 1 (Setup)
    │
Phase 2 (Foundational) ──── BLOCKS ALL ────┐
    │                                        │
Phase 3 (US1: Create/Invite) ←──────── Phase 7 (US5: Menu/Chat)
    │                                        │
Phase 4 (US2: Join)                    Phase 8 (US6: Banish Vote)
    │                                        │
Phase 5 (US3: Lobby/Start)            Phase 9 (US7: Murder Vote)
    │                                        │
Phase 6 (US4: Traitors/Cards)         Phase 10 (US8: GameThread)
    │                                        │
    └────────────── Phase 11 (Polish) ───────┘
```

### Within Each User Story

1. Tests MUST be written and FAIL before implementation
2. Models/entities before services
3. Services before controllers/endpoints
4. Backend before frontend (API must exist for frontend to call)
5. Core implementation before integration
6. Story complete before moving to next priority (unless parallel track)

### Parallel Opportunities

**Phase 2 parallelism** (all [P] tasks can run simultaneously):
- T006-T017 are all independent model/interface definitions
- T020 (frontend API) is independent of backend models

**US1-US4 sequential track** vs **US5-US8 parallel track**:
- After Phase 2, two independent tracks can proceed:
  - **Track A**: US1 → US2 → US3 → US4 (game creation → join → lobby → roles)
  - **Track B**: US5, US6, US7, US8 (chat, voting, lifecycle — all use interfaces from Phase 2)
- US5-US8 backend services can be developed against interfaces without needing US1-US4 implementations

**Frontend parallelism within each story**:
- All [P]-marked frontend pages/components within a story can be built simultaneously

---

## Parallel Example: User Story 1

```bash
# Launch tests first (parallel):
T021: "Unit tests for GameService.createGame"
T022: "Unit tests for InvitationService.sendInvitation"

# Launch message senders (parallel, after tests exist):
T023: "EmailMessageSender stub"
T024: "SmsMessageSender stub"

# Sequential (depend on senders):
T025: "InvitationService (depends on T023, T024)"
T026: "GameService (depends on T018 GameStore)"
T027: "GameController (depends on T025, T026)"

# Frontend (can start after API exists):
T028: "LoginPage.jsx"
T029: "API calls in api.js"
```

## Parallel Example: User Story 5 (Chat)

```bash
# Launch tests first (parallel):
T046: "Unit tests for channel implementations"
T047: "Unit tests for ChatService"

# Launch all channels (parallel, different files):
T048: "AllPlayersChannel"
T049: "TraitorsChannel"
T050: "DeadPlayersChannel"
T051: "NonTraitorsChannel"

# Sequential (depend on channels):
T052: "ChatService (depends on T048-T051)"
T053: "ChatSenderObject (depends on T052)"
T054: "ChatController (depends on T052)"
T055: "Menu endpoint (depends on game state)"

# Frontend (parallel, after API exists):
T056: "GameStatusBar.jsx"
T057: "ChatWindow.jsx"
T058: "MenuPage.jsx"
T059-T061: "Chat pages (all parallel, use ChatWindow)"
T062: "API calls"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (create game + invite)
4. Complete Phase 4: User Story 2 (join game)
5. **STOP and VALIDATE**: VIP creates game → invites player → player joins → both in lobby
6. Deploy/demo if ready — this is a functioning invitation system

### Incremental Delivery

1. **Setup + Foundational** → Foundation compiles, GameStore works
2. **Add US1 + US2** → Invitation flow works end-to-end (MVP!)
3. **Add US3 + US4** → Lobby and game start with roles/cards
4. **Add US5** → Chat system operational with access control
5. **Add US6 + US7** → Voting mechanics functional
6. **Add US8** → Full game lifecycle orchestration
7. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers after Foundational is complete:

- **Developer A (Track A)**: US1 → US2 → US3 → US4 (game flow)
- **Developer B (Track B)**: US5 → US6 → US7 (chat + voting)
- **Developer C**: US8 (game thread + events — works against interfaces)
- Stories integrate at the end via Phase 11 Polish

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks in the same phase
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify tests fail before implementing (TDD per Constitution Principle I)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All game logic classes (model/, gamethread/, event/, operation/) must have zero Spring imports
- Stub methods should have clear `// TODO` comments describing intended behavior
- Frontend pages should handle loading, error, and empty states
