package com.heartless.service;

import com.heartless.model.Card;
import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.GameStatusEnum;
import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.operation.item.ItemsInterface;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles player joining and player info retrieval.
 */
@Service
public class PlayerService {

    private final GameStore gameStore;
    private final GameService gameService;

    public PlayerService(GameStore gameStore, GameService gameService) {
        this.gameStore = gameStore;
        this.gameService = gameService;
    }

    public Map<String, Object> joinGame(String gameCode, String name, String contact) {
        GameObject game = gameService.getGameOrThrow(gameCode);
        validateGameAcceptsPlayers(game);
        Player player = findInvitedPlayer(game, name, contact);
        validateNotAlreadyJoined(player);

        synchronized (game) {
            player.setStatus(PlayerStatusEnum.ACTIVE);
        }

        String playerCode = UUID.randomUUID().toString();
        gameService.registerPlayerCode(playerCode, gameCode, player.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("playerCode", playerCode);
        result.put("player", buildPlayerSummary(player));
        result.put("gameCode", gameCode);
        return result;
    }

    public Map<String, Object> getJoinInfo(String gameCode) {
        GameObject game = gameService.getGameOrThrow(gameCode);
        validateGameAcceptsPlayers(game);

        Player vip = game.getVip();
        Map<String, Object> result = new HashMap<>();
        result.put("gameCode", gameCode);
        result.put("gameName", "The Heartless Game");
        result.put("hostName", vip != null ? vip.getName() : "Unknown");
        result.put("playerCount", game.getPlayerList().size());
        result.put("status", game.getGameStatus().name());
        return result;
    }

    public Map<String, Object> getPlayerInfo(String gameCode, String playerCode) {
        GameObject game = gameService.getGameOrThrow(gameCode);
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            throw new IllegalStateException("Invalid player code");
        }
        Player player = game.findPlayerById(playerId);
        if (player == null) {
            throw new IllegalStateException("Player not found in game");
        }
        return buildPrivatePlayerInfo(player);
    }

    private void validateGameAcceptsPlayers(GameObject game) {
        if (game.getGameStatus() != GameStatusEnum.INIT) {
            throw new IllegalStateException("Game is not accepting new players");
        }
    }

    private Player findInvitedPlayer(GameObject game, String name, String contact) {
        return game.getPlayerList().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .filter(p -> contactMatches(p, contact))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No matching invitation found"));
    }

    private boolean contactMatches(Player player, String contact) {
        String playerContact = player.getContact();
        return playerContact != null && playerContact.equalsIgnoreCase(contact);
    }

    private void validateNotAlreadyJoined(Player player) {
        if (player.getStatus() == PlayerStatusEnum.ACTIVE) {
            throw new IllegalStateException("Player already joined");
        }
    }

    private Map<String, Object> buildPlayerSummary(Player player) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", player.getId());
        map.put("name", player.getName());
        map.put("status", player.getStatus().name());
        return map;
    }

    private Map<String, Object> buildPrivatePlayerInfo(Player player) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", player.getId());
        map.put("name", player.getName());
        map.put("status", player.getStatus().name());
        map.put("isDead", player.isDead());
        map.put("isTraitor", player.isTraitor());
        Card card = player.getCard();
        if (card != null) {
            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("suit", card.getSuit().name());
            cardMap.put("number", card.getNumber().name());
            cardMap.put("imageUrl", card.getImageUrl());
            map.put("card", cardMap);
        } else {
            map.put("card", null);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (ItemsInterface item : player.getItems()) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", item.getName());
            itemMap.put("usable", item.isUsable(player, null));
            items.add(itemMap);
        }
        map.put("items", items);
        return map;
    }
}
