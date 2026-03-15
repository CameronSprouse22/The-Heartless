package com.heartless.model;

import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void createPlayerWithEmail() {
        Player player = new Player("Alice", "alice@test.com", null);
        assertEquals("Alice", player.getName());
        assertEquals("alice@test.com", player.getEmail());
        assertNull(player.getPhone());
        assertEquals(PlayerStatusEnum.PENDING, player.getStatus());
        assertFalse(player.isDead());
        assertFalse(player.isTraitor());
        assertNotNull(player.getId());
    }

    @Test
    void createPlayerWithPhone() {
        Player player = new Player("Bob", null, "+1234567890");
        assertEquals("Bob", player.getName());
        assertNull(player.getEmail());
        assertEquals("+1234567890", player.getPhone());
    }

    @Test
    void nullNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player(null, "a@b.com", null));
    }

    @Test
    void blankNameThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("  ", "a@b.com", null));
    }

    @Test
    void longNameThrows() {
        String longName = "A".repeat(51);
        assertThrows(IllegalArgumentException.class,
                () -> new Player(longName, "a@b.com", null));
    }

    @Test
    void noContactThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new Player("Alice", null, null));
    }

    @Test
    void statusTransitions() {
        Player player = new Player("Alice", "alice@test.com", null);
        assertEquals(PlayerStatusEnum.PENDING, player.getStatus());

        player.setStatus(PlayerStatusEnum.ACTIVE);
        assertEquals(PlayerStatusEnum.ACTIVE, player.getStatus());

        player.setStatus(PlayerStatusEnum.DISCONNECTED);
        assertEquals(PlayerStatusEnum.DISCONNECTED, player.getStatus());
    }

    @Test
    void getContactPrefersEmail() {
        Player player = new Player("Alice", "alice@test.com", "+123");
        assertEquals("alice@test.com", player.getContact());
    }

    @Test
    void getContactFallsBackToPhone() {
        Player player = new Player("Bob", null, "+123");
        assertEquals("+123", player.getContact());
    }

    @Test
    void equalityIsById() {
        Player p1 = new Player("Alice", "a@b.com", null);
        Player p2 = new Player("Alice", "a@b.com", null);
        assertNotEquals(p1, p2); // different UUIDs
        assertEquals(p1, p1);   // same instance
    }
}
