# Feature Specification: Traitors Game Core

**Feature Branch**: `001-traitors-game-core`  
**Created**: 2026-03-13  
**Status**: Draft  
**Input**: User description: "Create a game similar to Traitors with Java, Spring, React, and Maven. Includes player invitations via text/email, lobby system, traitor selection, voting, chat systems, and full game thread management with stub methods and classes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - VIP Creates Game and Invites Players (Priority: P1)

A VIP (host player) opens the application and creates a new game. The system generates a unique game code. The VIP enters player contact information (email addresses or phone numbers) and sends invitations. Each invited player receives a text message or email containing a link with the game code. The VIP can see which players have been invited and track their invitation status.

**Why this priority**: Without the ability to create a game and invite players, no other functionality can exist. This is the foundational entry point for the entire application.

**Independent Test**: Can be fully tested by creating a game, generating a code, and verifying invitations are queued for delivery. Delivers the core ability to start a game session.

**Acceptance Scenarios**:

1. **Given** the VIP opens the application, **When** they select "Create Game", **Then** a new game is created with a unique game code and the VIP is registered as the host player.
2. **Given** a game exists with a valid code, **When** the VIP enters a player's email address and sends an invitation, **Then** the system queues an email containing a join link with the game code.
3. **Given** a game exists with a valid code, **When** the VIP enters a player's phone number and sends an invitation, **Then** the system queues a text message containing a join link with the game code.
4. **Given** the VIP has sent invitations, **When** they view the game lobby, **Then** they see a list of all invited players with their current status (pending, accepted).

---

### User Story 2 - Invited Player Joins the Game (Priority: P1)

An invited player receives a text or email with a game link. They click the link, which opens the application and directs them to the Spring server to confirm joining. The player is added to the game's player list and the lobby updates for all participants.

**Why this priority**: Equally critical as Story 1 — players must be able to join for the game to function. This completes the two-sided invitation flow.

**Independent Test**: Can be tested by simulating a player clicking a join link with a valid game code and verifying they are added to the game's player list.

**Acceptance Scenarios**:

1. **Given** a player has received an invitation with a valid game code link, **When** they click the link, **Then** they are directed to the Spring server join confirmation page.
2. **Given** a player is on the join confirmation page, **When** they confirm joining, **Then** they are added to the game's player list with an active status.
3. **Given** a player uses an invalid or expired game code, **When** they attempt to join, **Then** the system displays an error message and does not add them to any game.
4. **Given** a player has already joined a game, **When** they click the same join link again, **Then** the system recognizes them and does not create a duplicate entry.

---

### User Story 3 - Lobby Readiness and Game Start (Priority: P2)

Once all invited players have accepted (or the VIP decides to proceed with current players), the VIP triggers the game start. All players receive a notification (via the lobby event) that the game is proceeding. The system transitions from the lobby phase to the game initialization phase.

**Why this priority**: This bridges the invitation flow to actual gameplay. Without lobby management and game-start triggering, the game cannot progress beyond gathering players.

**Independent Test**: Can be tested by having a set of players in a lobby, triggering start, and verifying all players receive the proceed notification and game state transitions to "Start".

**Acceptance Scenarios**:

1. **Given** the VIP has at least the minimum required number of players in the lobby, **When** the VIP confirms to start the game, **Then** a LobbyEvent is triggered and all players receive a message to proceed.
2. **Given** not all invited players have accepted, **When** the VIP chooses to proceed anyway, **Then** only accepted players are included in the game and uninvited/pending players are excluded.
3. **Given** the game has fewer than the minimum required players, **When** the VIP attempts to start, **Then** the system prevents starting and displays a message about insufficient players.

---

### User Story 4 - Traitor Selection and Card Assignment (Priority: P2)

After the game starts, the system selects traitors based on the total player count. Each player is assigned a random card (suit and number) which serves as their chat background image. Traitors are notified of their role privately; faithful players are not told who the traitors are.

**Why this priority**: Role assignment is the core mechanic that differentiates this game. It must happen before any game rounds can proceed.

**Independent Test**: Can be tested by initializing a game with a known player count, running traitor selection, and verifying the correct number of traitors are chosen and all players have unique card assignments.

**Acceptance Scenarios**:

1. **Given** a game has started with N players, **When** traitor selection runs, **Then** the correct number of traitors are selected based on player count (assumption: 1 traitor per 4-5 players, minimum 1).
2. **Given** traitor selection has completed, **When** each player views their role, **Then** traitors see they are traitors and faithful players see they are faithful — no player sees another's role.
3. **Given** card assignment runs, **When** completed, **Then** every player has a unique card (suit + number) assigned and this card image appears as their chat background.

---

### User Story 5 - Player Menu and Chat System (Priority: P3)

Each player sees a mobile-optimized main menu showing the game status and buttons for: Traitor Chat, All Chat, Banish Vote, Murder Vote, Individual Chat, Actions (stub), and Game Logs (stub). Chat screens display messages with the player's assigned card image as the background.

**Why this priority**: The menu and chat are the primary UI through which players interact with the game. While lower priority than game mechanics, they are essential for the player experience.

**Independent Test**: Can be tested by logging in as a player, verifying all menu buttons are visible with correct enabled/disabled states based on player role, and sending/receiving messages in each chat channel.

**Acceptance Scenarios**:

1. **Given** a player is logged into an active game, **When** they view the main menu, **Then** they see the current game status and buttons for all available actions.
2. **Given** a player is a traitor, **When** they open Traitor Chat, **Then** they see a chat with only other traitors, with their card image as the background.
3. **Given** a player is alive, **When** they open All Chat, **Then** they see a chat with all alive players, with their card image as the background.
4. **Given** a player opens Individual Chat, **When** they select another player from the dropdown, **Then** they can send and receive private messages with that player.
5. **Given** a player is faithful (not a traitor), **When** they view the menu, **Then** the Traitor Chat button is not visible or accessible.

---

### User Story 6 - Banish Vote (Priority: P3)

During the appropriate game phase, players can cast a banish vote. The vote screen shows all active players except the voting player. Only one player can be selected. The submit action availability depends on the current game mode/phase.

**Why this priority**: Voting is a core game mechanic but depends on the game loop being in place. It can be stubbed initially and fully wired later.

**Independent Test**: Can be tested by presenting the vote screen, verifying the voting player is excluded from the list, allowing one selection, and recording the vote result.

**Acceptance Scenarios**:

1. **Given** the game is in a voting phase, **When** a player opens the Banish Vote screen, **Then** they see all active players except themselves.
2. **Given** a player is viewing the Banish Vote screen, **When** they select one player and submit, **Then** their vote is recorded in the VoteResultObject.
3. **Given** a player has already cast a banish vote this round, **When** they return to the vote screen, **Then** they see their existing vote and cannot change it (or can change it, depending on game rules — assumed: one final vote per round).
4. **Given** the game is not in a voting phase, **When** a player attempts to access Banish Vote, **Then** the submit button is disabled or the screen indicates voting is not currently active.

---

### User Story 7 - Murder Vote (Priority: P3)

During the traitor-only phase, traitors can cast a murder vote. The vote screen shows all active non-traitor players. Multiple non-traitor targets can be selected (noted: this will need rework later). The result determines who is murdered.

**Why this priority**: Murder voting is the traitors' core action. Like banish voting, it depends on the game loop but can be independently tested as a voting mechanism.

**Independent Test**: Can be tested by presenting the murder vote screen to a traitor, verifying only non-traitor active players are shown, allowing selection, and recording the murder vote result.

**Acceptance Scenarios**:

1. **Given** the game is in the murder vote phase and the player is a traitor, **When** they open the Murder Vote screen, **Then** they see all active non-traitor players (excluding themselves and other traitors).
2. **Given** a traitor is viewing the Murder Vote screen, **When** they select target(s) and submit, **Then** the murder vote is recorded in the VoteResultObject.
3. **Given** a player is not a traitor, **When** they attempt to access the Murder Vote screen, **Then** the system denies access (screen is not visible or returns an error).

---

### User Story 8 - Game Thread and Round Lifecycle (Priority: P4)

The GameThread orchestrates the full game lifecycle: initialization (lobby), start (round loop), and end (final votes and reveals). Each round follows a sequence of events: murder reveal, mini-game, debate/pre-vote, vote, tie-break, vote reveal, recruit, and murder. The game continues while game criteria are met. The end phase includes final votes, last player reveals, traitor action history, and full game logs.

**Why this priority**: This is the orchestration layer. While critical for a complete game, the individual events it calls are stubs for now. It sets up the skeleton that future implementations will fill in.

**Independent Test**: Can be tested by creating a GameThread with mock events, stepping through the lifecycle, and verifying the correct sequence of events is called and game state transitions happen in order.

**Acceptance Scenarios**:

1. **Given** a new GameThread is created, **When** GameInit is called, **Then** a GameObject is created, the game status is set to "Init", and a LobbyEvent is triggered.
2. **Given** the game is initialized with players, **When** GameStart is called, **Then** the status changes to "Start", rounds execute in sequence while game criteria are met, and each round follows the prescribed event order.
3. **Given** game criteria are no longer met (e.g., all traitors found or traitors win), **When** the current round ends, **Then** the game transitions to GameEnd.
4. **Given** GameEnd is called, **When** end criteria are met, **Then** the final vote loop runs, followed by last player reveals, traitor action reveals, and the full game log display.

---

### Edge Cases

- What happens when a player loses connectivity mid-game? (Assumption: player remains in game with "disconnected" status; they can rejoin using their code)
- What happens when the VIP disconnects? (Assumption: game pauses or a co-host is designated; for now, game continues without VIP-specific actions)
- What happens when a vote results in a tie? (Handled by the TieBreakEvent in the game loop)
- What happens when there are not enough players to assign traitors? (Minimum player count enforced at lobby start)
- What happens when all traitors are banished before game criteria trigger end? (Game criteria check catches this and transitions to GameEnd)
- What happens when a dead player tries to access chat or vote? (Dead players can only access Dead Players Channel; voting screens are inaccessible)
- What happens when a player's card assignment collides with another's? (Card assignment ensures uniqueness from a standard 52-card deck; with ≤52 players, no collisions occur)

## Requirements *(mandatory)*

### Functional Requirements

**Player & Invitation**
- **FR-001**: System MUST allow a VIP to create a new game, generating a unique game ID and game code
- **FR-002**: System MUST send game invitations via email containing a clickable join link with the game code
- **FR-003**: System MUST send game invitations via text/SMS containing a clickable join link with the game code
- **FR-004**: System MUST allow invited players to join a game by clicking the invitation link, which routes to the Spring server for confirmation
- **FR-005**: System MUST authenticate players using a unique player code (no traditional username/password login)
- **FR-006**: System MUST prevent duplicate player entries when a player clicks the join link multiple times
- **FR-007**: System MUST reject join attempts with invalid or expired game codes

**Lobby**
- **FR-008**: System MUST display a lobby view showing all invited/joined players and their status
- **FR-009**: System MUST allow the VIP to start the game when minimum player count is reached
- **FR-010**: System MUST trigger a LobbyEvent that notifies all accepted players to proceed when the game starts
- **FR-011**: System MUST exclude pending/uninvited players when the VIP starts the game before all invitations are accepted

**Game Initialization & Roles**
- **FR-012**: System MUST select traitors based on total player count (assumption: 1 traitor per 4-5 players, minimum 1 traitor)
- **FR-013**: System MUST assign each player a unique random card (suit + number from a standard 52-card deck)
- **FR-014**: System MUST privately notify traitors of their role without revealing traitor identities to faithful players
- **FR-015**: System MUST use each player's assigned card image as the background for their chat screens

**Menu & Navigation**
- **FR-016**: System MUST present a mobile-optimized main menu showing current game status
- **FR-017**: System MUST provide navigation buttons for: Traitor Chat, All Chat, Banish Vote, Murder Vote, Individual Chat, Actions (stub), and Game Logs (stub)
- **FR-018**: System MUST show or hide menu options based on player role (e.g., Traitor Chat only visible to traitors)

**Chat System**
- **FR-019**: System MUST provide a Traitor Chat channel accessible only by traitors, displaying messages with the user's card image as background
- **FR-020**: System MUST provide an All Chat channel accessible by all alive players
- **FR-021**: System MUST provide an Individual Chat feature with a dropdown to select another player for private messaging
- **FR-022**: System MUST provide a Dead Players Chat channel accessible only by eliminated players

**Voting**
- **FR-023**: System MUST display a Banish Vote screen showing all active players except the voting player, allowing selection of exactly one player
- **FR-024**: System MUST enable or disable the Banish Vote submit button based on the current game phase
- **FR-025**: System MUST display a Murder Vote screen (traitors only) showing all active non-traitor players
- **FR-026**: System MUST record each vote as a vote object containing the casting player and receiving player
- **FR-027**: System MUST aggregate votes into a VoteResultObject containing the full vote list

**Game Thread & Lifecycle**
- **FR-028**: System MUST manage the game through three phases: Init (lobby), Start (round loop), and End (final reveals)
- **FR-029**: System MUST track game status via an enum: Init, Start, End, Over
- **FR-030**: System MUST execute rounds in sequence, each containing: murder reveal, mini-game, debate/pre-vote, vote, tie-break, vote reveal, recruit, and murder events
- **FR-031**: System MUST evaluate game criteria after each round to determine if the game continues or transitions to end phase
- **FR-032**: System MUST execute the end phase with final votes, last player reveals, traitor action history, and full game log display

**Communication Infrastructure**
- **FR-033**: System MUST support sending messages to players via a channel-based system (All Players, Dead Players, Traitors, Non-Traitors)
- **FR-034**: System MUST provide a ChatSenderObject that accepts text and a player reference and delivers to the appropriate channel

### Key Entities

- **Player**: Unique identifier (String ID), name, email, phone number, status (active/disconnected/removed), alive/dead flag, traitor/faithful flag, inventory of items
- **Card**: Suit (Spade, Club, Heart, Diamond) and number (1-13 where 11=Jack, 12=Queen, 13=King); serves as player's visual identity in chat
- **GameObject**: Central game state — game ID (Long), game code (String), player list, round list, current stage (InStart, NormalRounds, FinalRound), current round number (-1 at start, 0 at final), event log, start/end timestamps, last murder ID
- **GameThread**: Orchestrator holding a GameObject, GameCriteriaObject, and event list; manages Init→Start→End lifecycle
- **RoundObject**: Round number, murder revealed flag, mini-game played flag, mini-game winner, banish vote result, banished player, murder vote result, murder result (Successful/Blocked/Deflected), murdered player
- **VoteResultObject**: Contains an ArrayList of Vote objects for a given voting round
- **Vote**: Casting player and receiving player pair
- **EventObject (Interface)**: Start conditions, end conditions, and associated game reference — implemented by LobbyEvent, VoteEvent, MurderEvent, MiniGameEvent, TieBreakEvent, etc.
- **ConditionInterface**: Evaluates game conditions on a GameObject — implemented by NumberOfPlayerCondition, TimeCondition
- **ChannelObject (Interface)**: Communication channels — implemented by AllPlayersChannel, DeadPlayersChannel, TraitorsChannel, NonTraitorsChannel
- **ItemsInterface**: Game items — implemented by Shield, Knife, Tether, Duel, Lease
- **ChatSenderObject**: Sends chat messages to a specified player via the appropriate channel

## Assumptions

- **Minimum players**: A game requires at least 4 players to start (1 traitor + 3 faithful minimum)
- **Maximum players**: Capped at 52 (one unique card per player from a standard deck)
- **Traitor ratio**: Approximately 1 traitor per 4-5 players, minimum 1 traitor; exact formula to be finalized during implementation
- **Player authentication**: Code-based login only (no passwords, no OAuth) — the unique player code from the invitation link serves as authentication
- **Mobile-first**: All HTML pages are designed for mobile viewport; desktop is secondary
- **Stub methods**: Actions menu and Game Logs menu are placeholder buttons with no functionality in this phase
- **Event stubs**: Most game events (MiniGameEvent, RecruitEvent, etc.) are created as stub classes with interfaces defined but no business logic
- **SMS/Email provider**: The specific provider for text/email delivery is an implementation detail — the specification only requires the capability exists
- **Real-time updates**: Chat and lobby updates delivered in near-real-time (assumption: polling or WebSocket, implementation detail)
- **Single game per player**: A player can only be in one active game at a time

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A VIP can create a game and invite players via email or text within 2 minutes
- **SC-002**: An invited player can join a game by clicking the link and confirming within 30 seconds
- **SC-003**: The lobby correctly reflects all joined players in real-time and the VIP can start the game in one action
- **SC-004**: Traitor selection assigns the correct number of traitors based on player count with 100% accuracy
- **SC-005**: Every player receives a unique card assignment with no duplicates across the game
- **SC-006**: All chat channels enforce access control (traitors-only chat is invisible to faithful players) with zero leakage
- **SC-007**: Banish Vote screen correctly excludes the voting player and records exactly one vote per player per round
- **SC-008**: Murder Vote screen is accessible only to traitors and correctly excludes traitors from the target list
- **SC-009**: The GameThread successfully orchestrates the Init→Start→End lifecycle, executing events in the prescribed order
- **SC-010**: All defined classes and interfaces compile with stub methods in place, ready for future implementation
- **SC-011**: 90% of players can navigate the mobile menu and complete a voting action on first attempt without guidance
