package com.heartless.operation.channel;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Channel for non-traitor (faithful) players.
 */
public class NonTraitorsChannel implements ChannelObjectInterface {

    private final List<ChatMessage> messages = new ArrayList<>();

    @Override
    public List<Player> getEligiblePlayers(GameObject game) {
        return game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE && !p.isTraitor() && !p.isDead())
                .toList();
    }

    @Override
    public void sendMessage(String message, Player sender) {
        messages.add(new ChatMessage(
                UUID.randomUUID().toString(),
                sender.getId(),
                sender.getName(),
                message,
                "faithful",
                System.currentTimeMillis()
        ));
    }

    @Override
    public List<ChatMessage> getMessages() {
        return List.copyOf(messages);
    }
}
