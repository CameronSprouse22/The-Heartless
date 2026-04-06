package com.heartless.push;

import com.heartless.service.GameService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final String vapidPublicKey;
    private final PushSubscriptionStore subscriptionStore;
    private final GameService gameService;
    private final PushNotificationService pushNotificationService;

    public PushController(@Qualifier("vapidPublicKey") String vapidPublicKey,
                          PushSubscriptionStore subscriptionStore,
                          GameService gameService,
                          PushNotificationService pushNotificationService) {
        this.vapidPublicKey = vapidPublicKey;
        this.subscriptionStore = subscriptionStore;
        this.gameService = gameService;
        this.pushNotificationService = pushNotificationService;
    }

    /** Returns the VAPID public key the browser needs to create a push subscription. */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidPublicKey));
    }

    /**
     * Register a browser push subscription for a player.
     * Body: { playerCode, gameCode, endpoint, p256dh, auth }
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @RequestBody Map<String, String> body) {
        String playerCode = body.get("playerCode");
        String gameCode   = body.get("gameCode");
        String endpoint   = body.get("endpoint");
        String p256dh     = body.get("p256dh");
        String auth       = body.get("auth");

        if (playerCode == null || gameCode == null || endpoint == null
                || p256dh == null || auth == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "playerCode, gameCode, endpoint, p256dh and auth are required"));
        }

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid player code"));
        }

        String playerName = gameService.getPlayerName(playerCode);
        subscriptionStore.save(gameCode, playerId, playerName != null ? playerName : playerId,
                endpoint, p256dh, auth);
        return ResponseEntity.ok(Map.of("registered", true));
    }

    /** Remove a player's push subscription (e.g. on explicit unsubscribe). */
    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @RequestParam String playerCode,
            @RequestParam String gameCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId != null) {
            subscriptionStore.removePlayer(gameCode, playerId);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Debug: returns how many subscriptions are stored for a game.
     * GET /api/push/status?gameCode=XXXX
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam String gameCode) {
        var subs = subscriptionStore.getForGame(gameCode);
        var players = subs.stream()
                .map(s -> Map.of("playerName", s.playerName(), "playerId", s.playerId()))
                .toList();
        return ResponseEntity.ok(Map.of(
                "gameCode", gameCode,
                "subscriptionCount", subs.size(),
                "subscribers", players
        ));
    }

    /**
     * Debug: send a test push to all subscribers of a game.
     * POST /api/push/test  body: { "gameCode": "XXXX" }
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testPush(@RequestBody Map<String, String> body) {
        String gameCode = body.get("gameCode");
        if (gameCode == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "gameCode required"));
        }
        var subs = subscriptionStore.getForGame(gameCode);
        if (subs.isEmpty()) {
            return ResponseEntity.ok(Map.of("sent", false, "reason", "No subscribers for game " + gameCode));
        }
        pushNotificationService.notifyGame(gameCode, "Test Notification",
                "If you see this, push notifications are working!");
        return ResponseEntity.ok(Map.of("sent", true, "subscriberCount", subs.size()));
    }

    /**
     * Get the current notification preferences for a player.
     * GET /api/push/prefs
     */
    @GetMapping("/prefs")
    public ResponseEntity<Map<String, Object>> getPrefs(
            @RequestHeader("X-Player-Code") String playerCode) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        PushSubscriptionStore.NotifPrefs prefs = subscriptionStore.getPrefs(playerId);
        return ResponseEntity.ok(prefsToMap(prefs));
    }

    /**
     * Update notification preferences for a player.
     * PUT /api/push/prefs
     * Body: { allChats, individualChats, traitorChats, eventStarted, eventEnding }
     */
    @PutMapping("/prefs")
    public ResponseEntity<Map<String, Object>> updatePrefs(
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, Object> body) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        PushSubscriptionStore.NotifPrefs current = subscriptionStore.getPrefs(playerId);
        PushSubscriptionStore.NotifPrefs updated = new PushSubscriptionStore.NotifPrefs(
                getBool(body, "allChats",        current.allChats()),
                getBool(body, "individualChats", current.individualChats()),
                getBool(body, "traitorChats",    current.traitorChats()),
                getBool(body, "eventStarted",    current.eventStarted()),
                getBool(body, "eventEnding",     current.eventEnding())
        );

        subscriptionStore.updatePrefs(playerId, updated);
        return ResponseEntity.ok(prefsToMap(updated));
    }

    private Map<String, Object> prefsToMap(PushSubscriptionStore.NotifPrefs prefs) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("allChats",        prefs.allChats());
        map.put("individualChats", prefs.individualChats());
        map.put("traitorChats",    prefs.traitorChats());
        map.put("eventStarted",    prefs.eventStarted());
        map.put("eventEnding",     prefs.eventEnding());
        return map;
    }

    private boolean getBool(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        return defaultValue;
    }
}