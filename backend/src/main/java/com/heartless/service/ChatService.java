package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.operation.channel.AllPlayersChannel;
import com.heartless.push.PushNotificationService;
import com.heartless.operation.channel.ChannelObjectInterface;
import com.heartless.operation.channel.DeadPlayersChannel;
import com.heartless.operation.channel.TraitorsChannel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat channels per game: all, traitors, dead, individual.
 */
@Service
public class ChatService {

    private final GameStore gameStore;
    private final PushNotificationService pushNotificationService;

    // gameCode -> channelName -> channel instance
    private final ConcurrentHashMap<String, Map<String, ChannelObjectInterface>> gameChannels = new ConcurrentHashMap<>();

    public ChatService(GameStore gameStore, PushNotificationService pushNotificationService) {
        this.gameStore = gameStore;
        this.pushNotificationService = pushNotificationService;
    }

    public Map<String, Object> sendMessage(String gameCode, String senderId, String channelName, String text, String recipientId) {
        GameObject game = getGameOrThrow(gameCode);
        Player sender = game.findPlayerById(senderId);
        if (sender == null) {
            throw new IllegalArgumentException("Player not found");
        }

        ChannelObjectInterface channel = getOrCreateChannel(gameCode, channelName);
        validateAccess(channel, game, sender, channelName);

        if ("individual".equals(channelName) && (recipientId == null || recipientId.isBlank())) {
            throw new IllegalArgumentException("recipientId required for individual channel");
        }

        channel.sendMessage(text, sender);

        List<ChannelObjectInterface.ChatMessage> msgs = channel.getMessages();
        ChannelObjectInterface.ChatMessage last = msgs.get(msgs.size() - 1);

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", last.messageId());
        result.put("channel", channelName);
        result.put("senderId", last.senderId());
        result.put("senderName", last.senderName());
        result.put("text", last.text());
        result.put("timestamp", last.timestamp());

        notifyChatRecipients(gameCode, game, senderId, channelName, recipientId, sender.getName(), text);

        return result;
    }

    public Map<String, Object> getMessages(String gameCode, String playerId, String channelName, Long since) {
        GameObject game = getGameOrThrow(gameCode);
        Player player = game.findPlayerById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }

        ChannelObjectInterface channel = getOrCreateChannel(gameCode, channelName);
        validateAccess(channel, game, player, channelName);

        List<ChannelObjectInterface.ChatMessage> messages = channel.getMessages();
        if (since != null) {
            messages = messages.stream()
                    .filter(m -> m.timestamp() > since)
                    .toList();
        }

        List<Map<String, Object>> messageMaps = messages.stream()
                .map(this::messageToMap).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("channel", channelName);
        result.put("messages", messageMaps);
        return result;
    }

    private Map<String, Object> messageToMap(ChannelObjectInterface.ChatMessage m) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", m.messageId());
        map.put("senderId", m.senderId());
        map.put("senderName", m.senderName());
        map.put("text", m.text());
        map.put("timestamp", m.timestamp());
        return map;
    }

    private ChannelObjectInterface getOrCreateChannel(String gameCode, String channelName) {
        Map<String, ChannelObjectInterface> channels = gameChannels.computeIfAbsent(gameCode, k -> new ConcurrentHashMap<>());
        return channels.computeIfAbsent(channelName, this::createChannel);
    }

    private ChannelObjectInterface createChannel(String channelName) {
        return switch (channelName) {
            case "all" -> new AllPlayersChannel();
            case "traitors" -> new TraitorsChannel();
            case "dead" -> new DeadPlayersChannel();
            case "individual" -> new AllPlayersChannel(); // Individual reuses all-players eligibility
            default -> throw new IllegalArgumentException("Unknown channel: " + channelName);
        };
    }

    private void validateAccess(ChannelObjectInterface channel, GameObject game, Player player, String channelName) {
        // Individual channel: any alive player can use it
        if ("individual".equals(channelName)) {
            if (player.isDead()) {
                throw new SecurityException("Dead players cannot use individual chat");
            }
            return;
        }
        // Traitor chat: accessible to all players (security deferred)
        if ("traitors".equals(channelName)) {
            return;
        }

        List<Player> eligible = channel.getEligiblePlayers(game);
        if (!eligible.contains(player)) {
            throw new SecurityException("Player not eligible for channel: " + channelName);
        }
    }

    private void notifyChatRecipients(String gameCode, GameObject game, String senderId,
                                       String channelName, String recipientId,
                                       String senderName, String text) {
        String notifType = switch (channelName) {
            case "all"        -> "allChats";
            case "individual" -> "individualChats";
            case "traitors"   -> "traitorChats";
            default           -> "allChats";
        };

        List<String> recipientPlayerIds = switch (channelName) {
            case "all" -> game.getPlayerList().stream()
                    .filter(p -> !p.getId().equals(senderId))
                    .map(Player::getId).toList();
            // Traitor chat access is open to all active players until security is enforced;
            // notify every active non-dead player except the sender.
            case "traitors" -> game.getPlayerList().stream()
                    .filter(p -> !p.isDead() && !p.getId().equals(senderId))
                    .map(Player::getId).toList();
            case "dead" -> game.getPlayerList().stream()
                    .filter(p -> p.isDead() && !p.getId().equals(senderId))
                    .map(Player::getId).toList();
            case "individual" -> (recipientId != null && !recipientId.isBlank())
                    ? List.of(recipientId) : List.of();
            default -> List.of();
        };

        if (recipientPlayerIds.isEmpty()) return;

        String preview = text.length() > 100 ? text.substring(0, 100) + "\u2026" : text;
        pushNotificationService.notifyPlayers(gameCode, recipientPlayerIds, senderName, preview, channelName, notifType);
    }

    private GameObject getGameOrThrow(String gameCode) {
        GameObject game = gameStore.getGame(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameCode);
        }
        return game;
    }

    /**
     * Returns message counts per channel for a game.
     */
    public Map<String, Integer> getMessageCounts(String gameCode) {
        Map<String, ChannelObjectInterface> channels = gameChannels.get(gameCode);
        Map<String, Integer> counts = new HashMap<>();
        if (channels == null) {
            counts.put("all", 0);
            counts.put("traitors", 0);
            counts.put("dead", 0);
            counts.put("individual", 0);
            return counts;
        }
        for (String name : List.of("all", "traitors", "dead", "individual")) {
            ChannelObjectInterface ch = channels.get(name);
            counts.put(name, ch != null ? ch.getMessages().size() : 0);
        }
        return counts;
    }
}
