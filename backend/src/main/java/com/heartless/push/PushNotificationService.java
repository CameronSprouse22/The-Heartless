package com.heartless.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
     * Send a personalised push notification to every subscribed player in the given game.
     * Each notification title is {@code [playerName][gameCode]} and the body is the
     * notification text. Clicking the notification navigates to the player's game menu.
     *
     * @param gameCode  the game to notify
     * @param eventTitle short event title
     * @param body      notification body text
     */
    public void notifyGame(String gameCode, String eventTitle, String body) {
        List<PushSubscriptionStore.PlayerPushSubscription> subs =
                subscriptionStore.getForGame(gameCode);

        if (subs.isEmpty()) {
            log.info("[push] No subscriptions for game={} — skipping push", gameCode);
            return;
        }

        log.info("[push] Sending push to {} subscriber(s) for game={}", subs.size(), gameCode);

        for (PushSubscriptionStore.PlayerPushSubscription sub : subs) {
            String notifTitle = "[" + sub.playerName() + "][" + gameCode + "]";
            String notifBody  = eventTitle + ": " + body;
            String menuUrl    = "/menu/" + gameCode + "/" + sub.playerName();

            String payload;
            try {
                payload = objectMapper.writeValueAsString(Map.of(
                        "title", notifTitle,
                        "body",  notifBody,
                        "url",   menuUrl
                ));
            } catch (Exception e) {
                log.error("[push] Failed to serialize payload for player={}", sub.playerId(), e);
                continue;
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
                    log.info("[push] Delivered to player={} (HTTP 201)", sub.playerName());
                } else if (status == 404 || status == 410) {
                    log.warn("[push] Subscription gone for player={} (HTTP {}) — removing",
                            sub.playerName(), status);
                    subscriptionStore.removePlayer(gameCode, sub.playerId());
                } else {
                    log.warn("[push] Unexpected HTTP {} for player={}", status, sub.playerName());
                }
            } catch (Exception e) {
                log.error("[push] Exception sending to player={}: {}", sub.playerName(), e.getMessage(), e);
            }
        }
    }
}
