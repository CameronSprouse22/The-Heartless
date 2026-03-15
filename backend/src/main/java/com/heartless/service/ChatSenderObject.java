package com.heartless.service;

import com.heartless.model.Player;
import com.heartless.operation.channel.ChannelObjectInterface;

/**
 * Delegates message delivery to appropriate channel.
 */
public class ChatSenderObject {

    private final ChannelObjectInterface channel;

    public ChatSenderObject(ChannelObjectInterface channel) {
        this.channel = channel;
    }

    public void send(String message, Player sender) {
        channel.sendMessage(message, sender);
    }

    public ChannelObjectInterface getChannel() {
        return channel;
    }
}
