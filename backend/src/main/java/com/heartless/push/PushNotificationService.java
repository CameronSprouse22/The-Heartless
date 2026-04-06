package com.heartless.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PushNotificationService {

    private static final Logger log = LogManager.getLogger(PushNotificationService.class);

    private final PushService pushService;
    private final PushSubscriptionStore subscriptionStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PushNotificationService(PushService pushService,
                                   PushSubscriptionStore subscriptionStore) {
        this.pushService = pushService;
        this.subscriptionStore = subscriptionStore;
    }

    /**
     * Send a push to every subscribed player in the game, checking each player's
     * notification preferences for the given type.
     *
     * @param gameCode   the game to notify
     * @param eventTitle short event title
     * @param body       notification body text
     * @param notifType  preference key ("eventStarted", "eventEnding", …)
     */
    public void notifyGame(String gameCode, String eventTitle, String body, String notifType) {
        List<PushSubscriptionStore.PlayerPushSubscription> subs =
                subscriptionStore.getForGame(gameCode);

        if (subs.isEmpty()) {
            log.info("[push] No subscriptions for game={} — skipping push", gameCode);
            return;
        }

        log.info("[push] Sending {} push to {} subscriber(s) for game={}", notifType, subs.size(), gameCode);

        for (PushSubscriptionStore.PlayerPushSubscription sub : subs) {
            PushSubscriptionStore.NotifPrefs prefs = subscriptionStore.getPrefs(sub.playerId());
            if (!isTypeEnabled(prefs, notifType)) continue;

            String notifTitle = "[" + sub.playerName() + "][" + gameCode + "]";
            String notifBody  = eventTitle.isBlank() ? body : (body.isBlank() ? eventTitle : eventTitle + ": " + body);
            String menuUrl    = "/menu/" + gameCode + "/" + sub.playerName();

            sendToSubscriber(sub, gameCode, notifTitle, notifBody, menuUrl);
        }
    }

    /**
     * Legacy overload — defaults to "eventStarted" type.
     */
    public void notifyGame(String gameCode, String eventTitle, String body) {
        notifyGame(gameCode, eventTitle, body, "eventStarted");
    }

    /**
     * Send a chat push notification to a specific subset of players, respecting each
     * player's notification preferences for the given {@code notifType}.
     *
     * @param gameCode    the game the players belong to
     * @param playerIds   IDs of players to notify
     * @param senderName  display name of the message sender
     * @param body        message preview (already truncated)
     * @param channelName chat channel name ("all", "traitors", "dead", "individual")
     * @param notifType   preference key ("allChats", "individualChats", "traitorChats", …)
     */
    public void notifyPlayers(String gameCode, List<String> playerIds,
                              String senderName, String body, String channelName,
                              String notifType) {
        if (playerIds.isEmpty()) return;

        Set<String> targetIds = new HashSet<>(playerIds);
        List<PushSubscriptionStore.PlayerPushSubscription> subs =
                subscriptionStore.getForGame(gameCode).stream()
                        .filter(s -> targetIds.contains(s.playerId()))
                        .toList();

        if (subs.isEmpty()) return;

        String url = "/chat/" + gameCode + "/" + channelName;
        String title = senderName + " sent a message";

        for (PushSubscriptionStore.PlayerPushSubscription sub : subs) {
            PushSubscriptionStore.NotifPrefs prefs = subscriptionStore.getPrefs(sub.playerId());
            if (!isTypeEnabled(prefs, notifType)) continue;

            sendToSubscriber(sub, gameCode, title, body, url);
        }
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private void sendToSubscriber(PushSubscriptionStore.PlayerPushSubscription sub,
                                  String gameCode, String title, String body, String url) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body",  body,
                    "url",   url
            ));
        } catch (Exception e) {
            log.error("[push] Failed to serialize payload for player={}", sub.playerId(), e);
            return;
        }

        try {
            Notification notification = new Notification(
                    sub.endpoint(),
                    sub.p256dh(),
                    sub.auth(),
                    payload.getBytes()
            );
            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 201) {
                log.debug("[push] Delivered to player={} (HTTP 201)", sub.playerName());
            } else if (status == 404 || status == 410) {
                log.warn("[push] Stale subscription removed for player={} (HTTP {})", sub.playerName(), status);
                subscriptionStore.removePlayer(gameCode, sub.playerId());
            } else {
                log.warn("[push] Unexpected HTTP {} for player={}", status, sub.playerName());
            }
        } catch (Exception e) {
            log.error("[push] Exception sending push to player={}: {}", sub.playerName(), e.getMessage(), e);
        }
    }

    private boolean isTypeEnabled(PushSubscriptionStore.NotifPrefs prefs, String type) {
        return switch (type) {
            case "allChats"        -> prefs.allChats();
            case "individualChats" -> prefs.individualChats();
            case "traitorChats"    -> prefs.traitorChats();
            case "eventStarted"    -> prefs.eventStarted();
            case "eventEnding"     -> prefs.eventEnding();
            default -> true;
        };
    }
}
