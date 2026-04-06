package com.heartless.push;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store mapping gameCode → list of player push subscriptions.
 */
@Component
public class PushSubscriptionStore {

    /** gameCode → list of subscriptions */
    private final ConcurrentHashMap<String, List<PlayerPushSubscription>> subscriptions =
            new ConcurrentHashMap<>();

    /** playerId → notification preferences (global, not per-game) */
    private final ConcurrentHashMap<String, NotifPrefs> prefsMap = new ConcurrentHashMap<>();

    public void save(String gameCode, String playerId, String playerName,
                     String endpoint, String p256dh, String auth) {
        subscriptions.compute(gameCode, (key, existing) -> {
            List<PlayerPushSubscription> list = existing != null
                    ? new ArrayList<>(existing) : new ArrayList<>();
            // Replace any existing subscription for this player
            list.removeIf(s -> s.playerId().equals(playerId));
            list.add(new PlayerPushSubscription(playerId, playerName, endpoint, p256dh, auth));
            return list;
        });
    }

    public List<PlayerPushSubscription> getForGame(String gameCode) {
        return subscriptions.getOrDefault(gameCode, Collections.emptyList());
    }

    public void removePlayer(String gameCode, String playerId) {
        subscriptions.computeIfPresent(gameCode, (key, list) -> {
            List<PlayerPushSubscription> updated = new ArrayList<>(list);
            updated.removeIf(s -> s.playerId().equals(playerId));
            return updated;
        });
    }

    public void updatePrefs(String playerId, NotifPrefs prefs) {
        prefsMap.put(playerId, prefs);
    }

    public NotifPrefs getPrefs(String playerId) {
        return prefsMap.getOrDefault(playerId, NotifPrefs.defaults());
    }

    public record PlayerPushSubscription(
            String playerId,
            String playerName,
            String endpoint,
            String p256dh,
            String auth) {}

    public record NotifPrefs(
            boolean allChats,
            boolean individualChats,
            boolean traitorChats,
            boolean eventStarted,
            boolean eventEnding) {

        public static NotifPrefs defaults() {
            return new NotifPrefs(true, true, true, true, true);
        }
    }
}
