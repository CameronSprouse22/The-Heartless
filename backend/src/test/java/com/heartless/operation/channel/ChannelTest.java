package com.heartless.operation.channel;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTest {

    private GameObject game;
    private Player alivePlayer;
    private Player deadPlayer;
    private Player traitorPlayer;
    private Player faithfulPlayer;

    @BeforeEach
    void setUp() {
        game = new GameObject("TEST01");

        alivePlayer = new Player("Alive", "alive@test.com", null);
        alivePlayer.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(alivePlayer);

        deadPlayer = new Player("Dead", "dead@test.com", null);
        deadPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        deadPlayer.setDead(true);
        game.addPlayer(deadPlayer);

        traitorPlayer = new Player("Traitor", "traitor@test.com", null);
        traitorPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        traitorPlayer.setTraitor(true);
        game.addPlayer(traitorPlayer);

        faithfulPlayer = new Player("Faithful", "faithful@test.com", null);
        faithfulPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(faithfulPlayer);
    }

    // AllPlayersChannel tests
    @Test
    void allPlayersChannelFiltersAlive() {
        AllPlayersChannel channel = new AllPlayersChannel();
        List<Player> eligible = channel.getEligiblePlayers(game);
        // alive, traitor, faithful are alive; dead is excluded
        assertEquals(3, eligible.size());
        assertFalse(eligible.contains(deadPlayer));
    }

    @Test
    void allPlayersChannelSendsAndRetrievesMessages() {
        AllPlayersChannel channel = new AllPlayersChannel();
        channel.sendMessage("Hello all!", alivePlayer);
        assertEquals(1, channel.getMessages().size());
        assertEquals("Hello all!", channel.getMessages().get(0).text());
        assertEquals("all", channel.getMessages().get(0).channel());
    }

    // TraitorsChannel tests
    @Test
    void traitorsChannelFiltersTraitorsOnly() {
        TraitorsChannel channel = new TraitorsChannel();
        List<Player> eligible = channel.getEligiblePlayers(game);
        assertEquals(1, eligible.size());
        assertTrue(eligible.contains(traitorPlayer));
    }

    @Test
    void traitorsChannelSendsAndRetrievesMessages() {
        TraitorsChannel channel = new TraitorsChannel();
        channel.sendMessage("Secret plan", traitorPlayer);
        assertEquals(1, channel.getMessages().size());
        assertEquals("traitors", channel.getMessages().get(0).channel());
    }

    // DeadPlayersChannel tests
    @Test
    void deadPlayersChannelFiltersDeadOnly() {
        DeadPlayersChannel channel = new DeadPlayersChannel();
        List<Player> eligible = channel.getEligiblePlayers(game);
        assertEquals(1, eligible.size());
        assertTrue(eligible.contains(deadPlayer));
    }

    @Test
    void deadPlayersChannelSendsMessages() {
        DeadPlayersChannel channel = new DeadPlayersChannel();
        channel.sendMessage("Ghost chat", deadPlayer);
        assertEquals(1, channel.getMessages().size());
        assertEquals("dead", channel.getMessages().get(0).channel());
    }

    // NonTraitorsChannel tests
    @Test
    void nonTraitorsChannelFiltersFaithfulAlive() {
        NonTraitorsChannel channel = new NonTraitorsChannel();
        List<Player> eligible = channel.getEligiblePlayers(game);
        // alivePlayer and faithfulPlayer are alive non-traitors
        assertEquals(2, eligible.size());
        assertFalse(eligible.contains(traitorPlayer));
        assertFalse(eligible.contains(deadPlayer));
    }

    @Test
    void nonTraitorsChannelSendsMessages() {
        NonTraitorsChannel channel = new NonTraitorsChannel();
        channel.sendMessage("Faithful chat", faithfulPlayer);
        assertEquals(1, channel.getMessages().size());
        assertEquals("faithful", channel.getMessages().get(0).channel());
    }
}
