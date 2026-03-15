# Phase 0 Research: Traitors Game Core

**Feature Branch**: `001-traitors-game-core`  
**Date**: 2026-03-13  
**Input**: Plan from [plan.md](plan.md)

---

## Topic 1: Spring Boot Project Structure for Game Server

**Decision**: Use a **layered-by-type package structure** with an explicit `model/` package that has zero Spring imports, keeping game logic in pure Java POJOs and service classes that receive dependencies via constructor injection.

Package layout under `com.heartless`:

```
model/          → Pure Java POJOs + enums (Player, Card, GameObject, RoundObject, Vote, VoteResultObject, enums/)
gamethread/     → Pure Java orchestration (GameThread, GameCriteriaObject) — no Spring annotations
event/          → Pure Java event objects implementing EventObjectInterface — no Spring annotations
operation/      → Pure Java operations: condition/, channel/, item/ — no Spring annotations
service/        → Spring @Service classes (GameService, PlayerService, etc.) — thin orchestration that delegates to model/gamethread/event
controller/     → Spring @RestController classes — thin HTTP wrappers that delegate to service/
messaging/      → MessageSenderInterface + implementations (EmailMessageSender, SmsMessageSender) — Spring-managed, interface-isolated
```

**Rationale**: This structure enforces the plan's stated architecture: "game logic has zero Spring imports." The `model/`, `gamethread/`, `event/`, and `operation/` packages contain only plain Java — no `@Component`, no `@Autowired`, no Spring context required. This means all core game logic is testable with plain JUnit (no `@SpringBootTest` needed), which results in fast tests and clean separation. The `service/` layer is the boundary where Spring's DI container is used; services are `@Service`-annotated and receive collaborators via constructor injection. Controllers are thin — they validate HTTP input, delegate to a service, and return a response. This is the standard Spring Boot "hexagonal-lite" approach recommended by the Spring team and aligns with the project's Constitution requirement of testability-first.

The alternative "package-by-feature" layout (e.g., `game/`, `chat/`, `voting/` each containing their own models + services + controllers) was considered but rejected because at ~30 classes total, the project is not large enough to benefit from feature packaging, and the plan already specifies this exact layered structure.

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **Package-by-feature** (e.g., `game/GameController + GameService + GameObject` in one package) | Project has ~30 classes — not large enough to justify feature silos. Also makes the "zero Spring in game logic" boundary harder to enforce since Spring-annotated classes would sit next to pure POJOs. |
| **Hexagonal / Ports & Adapters** (separate `domain/`, `application/`, `adapter/in/`, `adapter/out/` modules) | Overkill for a scaffold phase. Adds Maven multi-module complexity (separate `pom.xml` per module) with no payoff at this scale. The layered approach achieves the same testability goal with less ceremony. Can refactor to hexagonal later if the codebase grows beyond ~100 classes. |
| **Single flat package** (`com.heartless.*` — all classes at root) | Violates separation of concerns. At 30+ classes, flat packages become unnavigable. No compile-time boundary between game logic and Spring infrastructure. |

---

## Topic 2: In-Memory Game State Management in Spring Boot

**Decision**: Use a **dedicated `@Service` class (`GameStore`) wrapping a `ConcurrentHashMap<String, GameObject>`** where the key is the game code (String). The `GameStore` is a Spring singleton bean, injected into services that need to read or mutate game state.

```java
@Service
public class GameStore {
    private final ConcurrentHashMap<String, GameObject> games = new ConcurrentHashMap<>();

    public GameObject getGame(String gameCode) { return games.get(gameCode); }
    public void putGame(String gameCode, GameObject game) { games.put(gameCode, game); }
    public GameObject removeGame(String gameCode) { return games.remove(gameCode); }
    public boolean containsGame(String gameCode) { return games.containsKey(gameCode); }
}
```

For mutations within a single `GameObject` (e.g., adding a player to the player list, changing game status), use **synchronized blocks on the `GameObject` instance** rather than locking the entire map. This gives per-game concurrency: players in different games never block each other, while players in the same game are serialized for state mutations.

```java
GameObject game = gameStore.getGame(gameCode);
synchronized (game) {
    game.addPlayer(player);
    game.setStatus(GameStatusEnum.ACTIVE);
}
```

**Rationale**: `ConcurrentHashMap` is the standard Java concurrent collection for key-value lookups — it provides thread-safe `get`/`put`/`remove` without global locking. Wrapping it in a Spring `@Service` bean makes it injectable, testable (mock-friendly), and gives a single clear ownership point for game state. The per-game `synchronized` block is appropriate because:

1. The spec supports up to 52 concurrent players per game, all hitting REST endpoints simultaneously.
2. Multiple operations on a `GameObject` (read status → check condition → mutate) must be atomic.
3. `ConcurrentHashMap` only guarantees atomicity of individual map operations, not compound operations on the value object.
4. Per-game locking avoids a global bottleneck — games are independent.

This approach is simple, correct, and easy to replace later with a database-backed store (just swap `GameStore` implementation).

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **Spring `@Scope("prototype")` beans for each game** | Prototype beans are not stored by Spring's container after creation — you'd still need a Map to look them up by game code. Adds Spring complexity with no benefit over a plain `ConcurrentHashMap`. |
| **Spring `@SessionScope` or `@RequestScope`** | Game state is not tied to an HTTP session or request. Multiple players share one `GameObject`. Session scope is per-user, not per-game. Fundamentally wrong abstraction. |
| **`ReadWriteLock` per `GameObject`** | More granular than `synchronized` but adds complexity. For a scaffold phase with sub-200ms response targets and ≤52 players per game, `synchronized` on short critical sections is sufficient. `ReadWriteLock` becomes worthwhile only if reads vastly outnumber writes and lock contention is measured — premature optimization here. |
| **Lock-free with `AtomicReference<GameState>` (immutable snapshots)** | Elegant but requires making `GameObject` immutable and copying on every mutation. The spec's `GameObject` has mutable collections (player lists, round lists). Immutability retrofit is high effort for a scaffold phase with no proven contention bottleneck. |
| **Hazelcast / Redis** | Distributed cache is unnecessary for a single-server deployment. Adds infrastructure dependencies. Plan explicitly says "in-memory for this phase." |

---

## Topic 3: SMS Integration Patterns in Spring Boot

**Decision**: Define a **`MessageSenderInterface`** with a single `send` method. Provide two implementations: `EmailMessageSender` (using Spring Mail / `JavaMailSender`) and `SmsMessageSender` (using Twilio SDK). Use Spring's `@Qualifier` annotation to inject the desired implementation, or inject both and select at runtime based on the contact type (email vs. phone number).

```java
public interface MessageSenderInterface {
    /**
     * Send a message to a recipient.
     * @param to       recipient address (email or phone number)
     * @param subject  subject/title (may be ignored for SMS)
     * @param body     message content
     */
    void send(String to, String subject, String body);
}

@Service("emailSender")
public class EmailMessageSender implements MessageSenderInterface {
    private final JavaMailSender mailSender;
    // constructor injection, send() delegates to mailSender
}

@Service("smsSender")
public class SmsMessageSender implements MessageSenderInterface {
    // Twilio client, send() delegates to Twilio API
}
```

The `InvitationService` receives both implementations and dispatches based on contact type:

```java
@Service
public class InvitationService {
    private final MessageSenderInterface emailSender;
    private final MessageSenderInterface smsSender;

    public InvitationService(
            @Qualifier("emailSender") MessageSenderInterface emailSender,
            @Qualifier("smsSender") MessageSenderInterface smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void sendInvitation(String contact, String gameCode) {
        String body = "Join the game: https://example.com/join/" + gameCode;
        if (contact.contains("@")) {
            emailSender.send(contact, "Game Invitation", body);
        } else {
            smsSender.send(contact, "Game Invitation", body);
        }
    }
}
```

For the scaffold phase, both implementations can be stubs that log the message instead of actually sending.

**Rationale**: The Strategy pattern with a shared interface is the canonical Spring approach for swappable implementations. Benefits:

1. **Testability**: Mock `MessageSenderInterface` in tests — no real email/SMS sent.
2. **Swappability**: Replace Twilio with Vonage or AWS SNS by writing a new implementation and changing `@Qualifier` or a config property.
3. **Single contract**: Both email and SMS conform to `send(to, subject, body)`. SMS implementations ignore `subject` — a minor semantic mismatch that's acceptable given the simplicity.
4. **Spring-native**: `@Service` + `@Qualifier` is standard Spring DI. No custom factories needed.

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **Spring Cloud Stream / Messaging abstraction** | Designed for message broker integration (Kafka, RabbitMQ), not transactional email/SMS. Massive overkill for sending invitation messages. |
| **Java SPI (`ServiceLoader`)** | Works but bypasses Spring's DI container. Loses `@Qualifier` switching and `@MockBean` test support. No benefit over Spring-native approach. |
| **Single `NotificationService` with `if/else` internally** | Violates Open/Closed principle. Adding a third channel (push notification) requires modifying existing code. The interface approach just adds a new implementation class. |
| **Separate interfaces: `EmailSenderInterface` + `SmsSenderInterface`** | Creates unnecessary duplication. Both have the same conceptual contract: deliver a text message to a recipient address. A shared interface keeps the `InvitationService` simpler and allows uniform handling. |
| **Twilio for both (Twilio SendGrid for email, Twilio for SMS)** | Vendor lock-in. The interface approach allows mixing providers (e.g., AWS SES for email, Twilio for SMS) without changing calling code. |

---

## Topic 4: React + Spring Boot Integration with Maven

**Decision**: Use **`frontend-maven-plugin`** to build the React (Vite) app during `mvn package` and copy the built output into `src/main/resources/static/`, producing a **single executable JAR** that serves both the API and the React SPA.

Directory structure:
```
backend/
├── pom.xml                          ← includes frontend-maven-plugin
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
└── src/main/resources/static/       ← Vite build output copied here during Maven build
```

Maven plugin configuration (in `pom.xml`):
```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.1</version>
    <configuration>
        <workingDirectory>frontend</workingDirectory>
        <nodeVersion>v20.11.0</nodeVersion>
    </configuration>
    <executions>
        <execution>
            <id>install-node-and-npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
        </execution>
        <execution>
            <id>npm-install</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm-build</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend</id>
            <phase>generate-resources</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.directory}/classes/static</outputDirectory>
                <resources>
                    <resource><directory>frontend/dist</directory></resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Spring Boot auto-serves anything in `classpath:/static/`. A catch-all controller forwards non-API routes to `index.html` for React Router support:

```java
@Controller
public class SpaController {
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
```

**Rationale**: Single-artifact deployment is the simplest operational model — one `java -jar` command runs everything. The `frontend-maven-plugin` is the de facto standard for this pattern (4,000+ GitHub stars, actively maintained as of 2025). Benefits:

1. **One command**: `mvn clean package` produces a self-contained JAR.
2. **No separate web server**: No nginx / Apache needed. Spring Boot's embedded Tomcat serves static files efficiently.
3. **Dev workflow preserved**: During development, run `npm run dev` in `frontend/` (Vite dev server with hot reload on port 5173) and proxy API calls to Spring Boot on port 8080. Production build goes through Maven.
4. **CI/CD simplicity**: A single Maven build artifact means one Docker image, one deployment unit.
5. **Vite compatibility**: Vite's `npm run build` produces a `dist/` folder with hashed assets — perfect for static serving.

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **Separate deployment (React on CDN/nginx, API on Spring Boot)** | Adds operational complexity — two deployments, CORS configuration, separate CI pipelines. The spec targets a single-server setup. Separate deployment is warranted at scale but premature for a scaffold phase. |
| **Thymeleaf / server-side rendering** | User explicitly requested React. Thymeleaf cannot deliver the SPA experience (real-time chat, dynamic menus, card backgrounds) that the spec requires. |
| **Spring Boot + Webpack (via `frontend-maven-plugin`)** | Webpack works but Vite is significantly faster for development (native ES modules, instant HMR). User specified Vite. The Maven plugin is agnostic — it runs `npm` commands regardless of bundler. |
| **Multi-module Maven (separate `frontend` and `backend` modules)** | Adds a parent `pom.xml` and two child modules. More complex for no benefit at this scale. Single-module with `frontend/` subdirectory is simpler and `frontend-maven-plugin` handles it natively. Can refactor to multi-module later. |
| **`exec-maven-plugin` to shell out `npm run build`** | Works but less portable (requires Node.js pre-installed on build machine). `frontend-maven-plugin` downloads and manages its own Node.js binary — builds are reproducible without global Node.js installation. |

---

## Topic 5: Card Deck Randomization in Java

**Decision**: Use **`Collections.shuffle()` on an `ArrayList<Card>`** seeded with `ThreadLocalRandom` (default). Generate all 52 cards, shuffle once, and deal from the top sequentially to assign unique cards to players.

```java
public class CardAssignmentService {

    public List<Card> generateShuffledDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (CardSuit suit : CardSuit.values()) {
            for (CardNumber number : CardNumber.values()) {
                deck.add(new Card(suit, number));
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    public Map<Player, Card> assignCards(List<Player> players) {
        List<Card> deck = generateShuffledDeck();
        Map<Player, Card> assignments = new LinkedHashMap<>();
        for (int i = 0; i < players.size(); i++) {
            assignments.put(players.get(i), deck.get(i));
        }
        return assignments;
    }
}
```

**Rationale**: `Collections.shuffle()` internally implements the **Fisher-Yates (Knuth) shuffle** — it is not a separate algorithm to choose between. From the OpenJDK source:

```java
// java.util.Collections.shuffle(List<?>, Random)
for (int i = size; i > 1; i--)
    swap(list, i - 1, rnd.nextInt(i));
```

This is the canonical Fisher-Yates algorithm. Using `Collections.shuffle()` gives:

1. **Correctness**: O(n) time, O(1) extra space, uniformly random permutation — mathematically proven unbiased.
2. **Simplicity**: One line of code. No manual index management.
3. **Thread safety**: The shuffle operates on a local `ArrayList` created per-call. No shared mutable state. Each game gets its own shuffle — no cross-game interference.
4. **Sufficient randomness**: The default `ThreadLocalRandom` source (used since Java 8) provides good statistical distribution for a card game. Cryptographic randomness is unnecessary — players cannot observe or predict the PRNG state.

The method is a **pure function**: input → player list, output → card assignments. No side effects or shared state. Easily testable (with a seeded `Random` for deterministic tests).

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **Manual Fisher-Yates implementation** | `Collections.shuffle()` already implements Fisher-Yates. Reimplementing it manually risks off-by-one errors with no benefit. |
| **`SecureRandom`** | Cryptographically secure PRNG. Significantly slower (~10x) than `ThreadLocalRandom`. Justified for gambling or security-critical randomness, but this is a social deduction party game — statistical uniformity of `ThreadLocalRandom` is more than sufficient. If needed later (e.g., competitive play), pass `SecureRandom` instance to `Collections.shuffle(list, secureRandom)`. |
| **Reservoir sampling / online shuffling** | Useful when the full set is unknown or streaming. We know exactly 52 cards upfront — full shuffle is simpler and more readable. |
| **Pre-generated shuffle tables** | Eliminates runtime randomness — defeats the purpose. Only useful for testing, where we can inject a seeded `Random`. |
| **`Random` (non-ThreadLocal)** | `ThreadLocalRandom` is preferred in concurrent environments (no contention). `Random` uses a shared `AtomicLong` seed which can be a bottleneck under concurrent access. Not a concern for single-call shuffles, but `ThreadLocalRandom` is the modern default. |

---

## Topic 6: Traitor Selection Algorithm

**Decision**: Implement traitor selection as a **pure static function** that takes a player count, computes traitor count via integer division with a floor/minimum, and selects random indices using `Collections.shuffle()` on an index list.

```java
public class TraitorSelectionService {

    /**
     * Determine traitor count: 1 traitor per 5 players, minimum 1.
     * 4-5 players  → 1 traitor
     * 6-10 players → 2 traitors
     * 11-15 players → 3 traitors
     * etc.
     */
    public static int calculateTraitorCount(int playerCount) {
        if (playerCount < 4) {
            throw new IllegalArgumentException("Minimum 4 players required");
        }
        return Math.max(1, playerCount / 5);
    }

    /**
     * Select traitor indices from a player list.
     * Returns an unmodifiable set of indices into the player list.
     * Pure function: no side effects, no shared state.
     */
    public static Set<Integer> selectTraitorIndices(int playerCount, Random random) {
        int traitorCount = calculateTraitorCount(playerCount);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        return Collections.unmodifiableSet(
            new LinkedHashSet<>(indices.subList(0, traitorCount))
        );
    }
}
```

Traitor ratio table:

| Players | Traitors | Ratio |
|---------|----------|-------|
| 4-5     | 1        | 1:4 – 1:5 |
| 6-10    | 1-2      | 1:5 |
| 11-15   | 2-3      | 1:5 |
| 16-20   | 3-4      | 1:5 |
| ...     | ...      | 1:5 |
| 50-52   | 10       | 1:5 |

**Rationale**: This approach is optimal because:

1. **Pure function**: `calculateTraitorCount` and `selectTraitorIndices` are static methods with no mutable state, no Spring dependencies, and no side effects. Given the same inputs and `Random` seed, they produce identical output — perfect for testing.
2. **Testable**: Inject a `Random` with a known seed to get deterministic, repeatable results in unit tests.
3. **Fair**: Shuffling all indices and taking the first N is equivalent to sampling without replacement — every player has an equal probability of being selected as a traitor.
4. **Simple ratio**: Integer division `playerCount / 5` with `Math.max(1, ...)` gives a clean 1:5 ratio with guaranteed minimum of 1. The spec says "1 per 4-5 players" — `/5` is the conservative side of that range, which is better for game balance (fewer traitors = harder for traitors = more suspense).
5. **Bounded**: Max 52 players (deck size), so max 10 traitors. The algorithm handles all valid counts.

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **`playerCount / 4` ratio** | More aggressive — more traitors. 52 players → 13 traitors (25%). This tips the balance toward traitors. `/5` (20%) is closer to the original Traitors TV show format and gives faithful players a reasonable chance. |
| **Lookup table instead of formula** | Hardcoded table (e.g., `{4:1, 5:1, 6:2, ...}`) is explicit but doesn't scale cleanly and requires manual maintenance. Integer division is formula-driven and handles any count up to 52 automatically. |
| **`Random.ints()` stream with distinct()** | Functional style: `random.ints(0, playerCount).distinct().limit(traitorCount)`. Works but is less readable, harder to test deterministically, and has unbounded iteration risk (the stream generates random ints until enough distinct ones appear — technically O(∞) worst case though practically fast). Shuffle-and-take is O(n) guaranteed. |
| **Using `ThreadLocalRandom` directly in method** | Couples the method to a specific randomness source. Accepting `Random` as a parameter allows injecting `SecureRandom` for production, a seeded `Random` for tests, or `ThreadLocalRandom` via `ThreadLocalRandom.current()` at call site. |

---

## Topic 7: WebSocket vs Polling for Real-Time Chat in Spring Boot

**Decision**: For this scaffold phase, use **simple REST polling endpoints** with a path toward WebSocket (STOMP) upgrade in a future phase. Define the chat API as RESTful endpoints (`POST /api/chat/{channel}/messages`, `GET /api/chat/{channel}/messages?since={timestamp}`) with stub implementations.

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping("/{channel}/messages")
    public ResponseEntity<Void> sendMessage(
            @PathVariable String channel,
            @RequestBody ChatMessage message) {
        // Stub: store in-memory list
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{channel}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable String channel,
            @RequestParam(required = false) Instant since) {
        // Stub: return messages since timestamp
        return ResponseEntity.ok(List.of());
    }
}
```

**Rationale**: Polling is the correct choice for the scaffold phase for these reasons:

1. **Simplicity**: REST endpoints are trivial to implement, test, and debug. WebSocket adds STOMP protocol configuration, message brokers, session management, and error handling — significant complexity for a stub phase.
2. **Testability**: REST controllers are testable with `MockMvc` and standard HTTP client libraries. WebSocket testing requires `WebSocketStompClient` and async assertions — much more complex test setup.
3. **Mobile-first compatibility**: REST over HTTP works on every mobile browser without special handling. WebSocket on mobile browsers has edge cases (network switching, battery optimization killing connections, proxy/firewall issues on cellular networks).
4. **Stub-friendly**: The controllers are stubs with `// TODO` bodies. Polling stubs are trivially replaceable — when WebSocket is implemented in a future phase, the polling endpoints can either be removed or kept as fallbacks.
5. **Sufficient for development**: With a 2-3 second polling interval, the chat UX is acceptable for testing and development. Real-time feel can wait for the WebSocket phase.
6. **Progressive upgrade path**: The data model (`ChatMessage`, channels, message storage) is identical whether delivered via polling or WebSocket push. The future WebSocket upgrade only changes the transport layer, not the domain model.

Future WebSocket implementation plan (not this phase):
```
- Add spring-boot-starter-websocket dependency
- Configure WebSocketMessageBrokerConfigurer with STOMP
- Add @MessageMapping endpoints in ChatController
- Subscribe clients to /topic/chat/{channel}
- Keep REST endpoints as fallback for clients that don't support WebSocket
```

**Alternatives Considered**:

| Alternative | Why Rejected |
|---|---|
| **WebSocket (STOMP) from the start** | Spring's STOMP support (`@MessageMapping`, `SimpMessagingTemplate`, `WebSocketMessageBrokerConfigurer`) requires non-trivial configuration. Error handling, reconnection logic, and session lifecycle all need implementation. For stub bodies that return empty data, this is wasted effort. The transport layer should be the *last* thing built, not the first. |
| **Server-Sent Events (SSE)** | One-directional (server → client). Chat requires bidirectional communication. Would still need REST POST for sending messages, making it a hybrid approach. More complex than pure polling, less capable than WebSocket. Falls in an awkward middle ground. |
| **Long polling** | More responsive than simple polling but requires careful thread management on the server (holding threads open). Spring's `DeferredResult` or reactive `Flux` can handle this, but it's more complex than short polling and less efficient than WebSocket. Not worth the complexity in a stub phase. |
| **Third-party chat service (e.g., Firebase, Pusher)** | External dependency. The spec requires custom chat channels (Traitor Chat, All Chat, Dead Players Chat) with specific access rules. A third-party service would need to enforce these game-specific authorization rules externally. Keeping chat in-app gives full control over channel access logic. |

---

## Summary of Decisions

| # | Topic | Decision |
|---|---|---|
| 1 | Project Structure | Layered-by-type packages; `model/`, `gamethread/`, `event/`, `operation/` are pure Java with zero Spring imports |
| 2 | In-Memory State | `@Service GameStore` wrapping `ConcurrentHashMap<String, GameObject>` with per-game `synchronized` blocks |
| 3 | SMS Integration | `MessageSenderInterface` with `@Service` + `@Qualifier` for `EmailMessageSender` and `SmsMessageSender` |
| 4 | React + Maven | `frontend-maven-plugin` builds Vite into `static/`, single JAR artifact serves API + SPA |
| 5 | Card Randomization | `Collections.shuffle()` (which is Fisher-Yates) on `ArrayList<Card>`, deal sequentially |
| 6 | Traitor Selection | Pure static function: `playerCount / 5` with `Math.max(1, ...)`, shuffle-and-take for index selection |
| 7 | Chat Transport | REST polling endpoints for scaffold phase; WebSocket (STOMP) upgrade planned for future phase |
