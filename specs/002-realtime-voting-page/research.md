# Research: Real-Time Voting & Game Page

**Feature**: 002-realtime-voting-page  
**Date**: 2026-03-28  
**Purpose**: Resolve technical unknowns and document decisions for implementation planning

---

## Research Task 1: WebSocket Technology Choice for Real-Time Updates

### Context
The existing project uses REST-only communication (Spring Boot 3.2.3 + React 18). FR-012 requires pushing selection updates to all connected players in real-time.

### Decision: STOMP over SockJS via Spring WebSocket

### Rationale
- **Spring Boot native support**: `spring-boot-starter-websocket` provides first-class STOMP/SockJS integration — no additional framework wiring needed.
- **Topic-based pub/sub**: STOMP's `/topic/` prefix maps directly to the per-game-event broadcast model (e.g., `/topic/games/{gameCode}/event`). Players subscribe to their game's topic and receive all selection/status updates.
- **SockJS fallback**: Ensures connectivity on browsers or networks that block raw WebSocket (long-polling fallback).
- **Existing auth pattern compatibility**: The `X-Player-Code` header can be passed during the STOMP CONNECT handshake, and a `ChannelInterceptor` validates it — no new auth mechanism required.
- **Lightweight addition**: Only one new Maven dependency (`spring-boot-starter-websocket`) and two frontend packages (`@stomp/stompjs`, `sockjs-client`).

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Server-Sent Events (SSE)** | Server→client only; cannot receive client selection updates without a separate REST channel, adding complexity. STOMP provides bidirectional messaging in a single connection. |
| **Raw WebSocket (no STOMP)** | Requires hand-rolling message routing, serialization, and subscription management. STOMP gives these out of the box with Spring's `@MessageMapping` and `SimpMessagingTemplate`. |
| **Polling (setInterval fetch)** | Not truly real-time; 2-second latency target would require aggressive polling (every 500ms), wasting bandwidth. WebSocket pushes are event-driven with near-zero latency. |
| **Socket.IO** | Not natively supported by Spring Boot; would require a separate Node.js process or a third-party Java adapter. Adds operational complexity for no benefit over STOMP. |

---

## Research Task 2: Player Selection State Management on the Server

### Context
FR-007 requires persisting player selections immediately on change (without requiring submit). FR-027 requires the server to be able to resolve the event at any time using saved state.

### Decision: In-memory ConcurrentHashMap in GameEventService, keyed by gameCode

### Rationale
- **Consistency with existing pattern**: `GameStore`, `VotingService`, and `ChatService` all use `ConcurrentHashMap` for in-memory state. This project has no database and all state is ephemeral per game session.
- **Per-game isolation**: A `ConcurrentHashMap<String, GameEventState>` where `GameEventState` holds the config and a map of `playerId → PlayerSelection`. Each game's event is fully isolated.
- **Thread safety**: `ConcurrentHashMap` handles concurrent selection saves from multiple WebSocket connections. For compound operations (like agreement checking), a `synchronized` block on the game event state object prevents race conditions — same pattern used in `GameStore`.
- **Immediate persistence**: When a player toggles a button, the STOMP message handler calls `GameEventService.saveSelection()`, which writes to the map immediately. No batching or queue needed.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Store selections in GameObject** | Violates separation of concerns. `GameObject` tracks game-round lifecycle; event-level selection state is transient and should not pollute the core game model. |
| **Redis or external store** | The project has no external dependencies and runs in-memory by design (per constitution constraint: "No runtime dependencies beyond standard Java library unless justified"). Adding Redis is unjustified for a single-game-per-server architecture. |
| **Client-side only (localStorage)** | Cannot support timeout resolution (FR-023) or showing other players' selections (FR-012). Server must be the source of truth. |

---

## Research Task 3: Timeout Resolution Mechanism

### Context
FR-023 requires automatic event resolution when EndTime is reached, using server-side saved state.

### Decision: ScheduledExecutorService task scheduled at event creation time

### Rationale
- **Precise timing**: A single scheduled task per event fires at exactly EndTime. When it fires, it calls `GameEventService.resolveOnTimeout()` which reads current saved selections, builds the result, and broadcasts it via STOMP.
- **No polling overhead**: Unlike a periodic sweep, a single scheduled task per event is O(1) overhead.
- **Cancelable**: If the event resolves normally before timeout (all players submit, or agreement reached), the scheduled task is cancelled via its `ScheduledFuture`.
- **Existing Java API**: `ScheduledExecutorService` is standard Java — no additional dependency.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Client-side timer triggering a REST call** | Unreliable — player might close browser, lose connectivity, or manipulate the clock. Server-side timer is authoritative. |
| **Spring @Scheduled annotation with periodic sweep** | Periodic sweeps waste cycles checking events that aren't near their deadline. A per-event scheduled task is more precise and efficient. |
| **GameThread event model (execute/waitForEnd)** | GameThread runs in its own thread with a blocking event loop. Event timeout can coordinate with GameThread, but the actual timer still needs a scheduled task. Integrating with GameThread's existing event model is Phase 2 work and does not affect the core timer mechanism. |

---

## Research Task 4: Agreement Check Algorithm (PlayersMustAgree)

### Context
FR-019 requires closing the event when all players submit matching selections. FR-020 requires detecting disagreement and resetting.

### Decision: Check on each submission — compare all submitted selections when count equals total players

### Rationale
- **Simple trigger**: Every time `submitSelections()` is called, check if `submittedCount == totalPlayers`. If yes, collect all submitted selection sets and compare.
- **Equality check**: All players' selected item sets must be equal (same items selected). For SingleAnswer mode this is trivial (one item each). For multi-select, compare as `Set<String>`.
- **Disagreement handling**: If selections don't all match, reset all `PlayerSelection.submissionStatus` to `SELECTED` (not `SUBMITTED`), broadcast a disagreement notification via STOMP.
- **Thread safety**: The agreement check runs inside a `synchronized` block on the game event state, preventing race conditions when two players submit simultaneously.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Check agreement client-side** | Cannot be trusted — a malicious client could forge agreement. Server must be authoritative. |
| **Require all players to select the same option before allowing submit** | Changes the UX — the spec says players submit independently and disagreement is detected after the fact. |
| **Majority vote instead of unanimous** | Spec explicitly states "all players must agree" — majority is not the requirement. |

---

## Research Task 5: WebSocket Authentication and Security

### Context
The existing backend authenticates via `X-Player-Code` custom header on REST requests. WebSocket connections need equivalent authentication.

### Decision: Validate X-Player-Code during STOMP CONNECT handshake via ChannelInterceptor

### Rationale
- **STOMP CONNECT supports headers**: The client can pass native STOMP headers during the initial CONNECT frame. The `X-Player-Code` is sent as a STOMP header.
- **ChannelInterceptor**: Spring's `ChannelInterceptor.preSend()` intercepts CONNECT frames, extracts the player code, validates it against the game's player list via `PlayerService`, and stores the player identity in the session attributes.
- **Subsequent messages authenticated**: After CONNECT, the player identity from session attributes is used for all subsequent SEND/SUBSCRIBE operations — no need to re-send the header on each message.
- **Consistent with REST pattern**: Same identifier (`X-Player-Code`) used for both REST and WebSocket auth.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Query parameter in WebSocket URL** | Exposes the player code in server logs and browser history. Headers are more secure. |
| **Spring Security integration** | The project doesn't use Spring Security (no dependency). Adding it solely for WebSocket auth is over-engineering. |
| **No auth on WebSocket (rely on REST auth)** | Allows unauthorized users to subscribe to game topics and receive other players' selections. Must validate. |

---

## Research Task 6: Vite Proxy Configuration for WebSocket

### Context
The frontend dev server (Vite on port 5173) proxies `/api` to the backend (port 8080). WebSocket connections need proxy support too.

### Decision: Add `/ws` proxy entry to vite.config.js with `ws: true`

### Rationale
- **Vite supports WebSocket proxying**: Adding `'/ws': { target: 'http://localhost:8080', ws: true }` to the proxy config enables WebSocket pass-through.
- **SockJS initial handshake**: SockJS connects via HTTP first (for info endpoint), then upgrades to WebSocket. Vite's proxy handles both transparently with `ws: true`.
- **Separate path from REST**: Using `/ws` as the WebSocket endpoint path avoids conflicts with the existing `/api` proxy. The STOMP/SockJS endpoint is registered at `/ws` in `WebSocketConfig.java`.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Connect directly to port 8080 from frontend** | Would cause CORS issues in development and hardcodes the backend URL. Proxy keeps everything on the same origin. |
| **Reuse `/api` prefix for WebSocket** | Could conflict with REST routing. Separate `/ws` path is cleaner and conventional. |

---

## Research Task 7: Integration with Existing Event System

### Context
The existing game uses `EventObjectInterface` with `execute()`, `checkStartConditions()`, `checkEndConditions()`. The new game event page needs to work within or alongside this system.

### Decision: GameEventService operates independently; integration with GameThread's event loop is a future concern

### Rationale
- **Decoupled by design**: The game event page handles its own lifecycle (create event → collect selections → resolve). It does not need to block the GameThread's event loop.
- **GameThread can trigger events**: A future game event (e.g., `MiniGameEvent.execute()`) can call `GameEventService.createEvent()` to start a game/vote page and then poll/wait for the result. This integration is outside the scope of this feature's spec.
- **Testable in isolation**: `GameEventService` can be fully unit-tested without `GameThread`, `GameObject`, or any event classes — aligning with the Testability First principle.
- **REST endpoints for manual testing**: REST endpoints to create/query events allow testing without running a full game flow.

### Alternatives Considered
| Alternative | Why Rejected |
|---|---|
| **Embed event logic inside a new EventObjectInterface impl** | Mixes WebSocket/real-time concerns into the event system. Events are pure game logic; WebSocket communication is infrastructure. Separation of concerns (Principle III). |
| **Replace the existing vote system** | Too disruptive. The new game event page is a general-purpose component. Existing banish/murder votes continue working as-is; they can be migrated to use the new system in a future feature. |
