package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.operation.channel.ChannelObjectInterface;
import com.heartless.push.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChatServiceTest {

    private ChatService chatService;
    private GameStore gameStore;
    private GameObject game;
    private Player alivePlayer;
    private Player traitorPlayer;
    private Player deadPlayer;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
        chatService = new ChatService(gameStore, mock(PushNotificationService.class));

        game = new GameObject("CHAT01");
        alivePlayer = new Player("Alice", "alice@test.com", null);
        alivePlayer.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(alivePlayer);

        traitorPlayer = new Player("Traitor", "traitor@test.com", null);
        traitorPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        traitorPlayer.setTraitor(true);
        game.addPlayer(traitorPlayer);

        deadPlayer = new Player("Dead", "dead@test.com", null);
        deadPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        deadPlayer.setDead(true);
        game.addPlayer(deadPlayer);

        gameStore.putGame("CHAT01", game);
    }

    @Test
    void sendMessageToAllChannel() {
        var result = chatService.sendMessage("CHAT01", alivePlayer.getId(), "all", "Hello!", null);
        assertNotNull(result);
        assertEquals("Hello!", result.get("text"));
        assertEquals("all", result.get("channel"));
    }

    @Test
    void getMessagesFromAllChannel() {
        chatService.sendMessage("CHAT01", alivePlayer.getId(), "all", "Hello!", null);
        var result = chatService.getMessages("CHAT01", alivePlayer.getId(), "all", null);
        List<?> messages = (List<?>) result.get("messages");
        assertEquals(1, messages.size());
    }

    @Test
    void allowNonTraitorInTraitorsChannel() {
        // Traitor chat is accessible to all players (security deferred)
        var result = chatService.sendMessage("CHAT01", alivePlayer.getId(), "traitors", "Sneak!", null);
        assertEquals("traitors", result.get("channel"));
    }

    @Test
    void allowTraitorInTraitorsChannel() {
        var result = chatService.sendMessage("CHAT01", traitorPlayer.getId(), "traitors", "Secret!", null);
        assertEquals("traitors", result.get("channel"));
    }

    @Test
    void rejectAliveFromDeadChannel() {
        assertThrows(SecurityException.class,
                () -> chatService.sendMessage("CHAT01", alivePlayer.getId(), "dead", "Am I dead?", null));
    }

    @Test
    void allowDeadInDeadChannel() {
        var result = chatService.sendMessage("CHAT01", deadPlayer.getId(), "dead", "Ghost!", null);
        assertEquals("dead", result.get("channel"));
    }

    @Test
    void getMessagesWithSinceFilter() throws InterruptedException {
        chatService.sendMessage("CHAT01", alivePlayer.getId(), "all", "Old message", null);
        long sinceTime = System.currentTimeMillis();
        Thread.sleep(10); // small delay
        chatService.sendMessage("CHAT01", alivePlayer.getId(), "all", "New message", null);
        var result = chatService.getMessages("CHAT01", alivePlayer.getId(), "all", sinceTime);
        List<?> messages = (List<?>) result.get("messages");
        assertEquals(1, messages.size());
    }
}
