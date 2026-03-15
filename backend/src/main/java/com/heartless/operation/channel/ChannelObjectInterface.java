package com.heartless.operation.channel;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

import java.util.List;

/**
 * Contract for communication channels.
 */
public interface ChannelObjectInterface {

    List<Player> getEligiblePlayers(GameObject game);

    void sendMessage(String message, Player sender);

    List<ChatMessage> getMessages();

    /**
     * Simple message record for channel communication.
     */
    record ChatMessage(
            String messageId,
            String senderId,
            String senderName,
            String text,
            String channel,
            long timestamp
    ) {}
}
