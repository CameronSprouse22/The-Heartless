package com.heartless.service;

import com.heartless.model.GameObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStoreTest {

    private GameStore gameStore;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
    }

    @Test
    void putAndGetGame() {
        GameObject game = new GameObject("ABC123");
        gameStore.putGame("ABC123", game);
        assertEquals(game, gameStore.getGame("ABC123"));
    }

    @Test
    void getNonExistentReturnsNull() {
        assertNull(gameStore.getGame("NONE"));
    }

    @Test
    void containsGame() {
        gameStore.putGame("ABC123", new GameObject("ABC123"));
        assertTrue(gameStore.containsGame("ABC123"));
        assertFalse(gameStore.containsGame("XYZ789"));
    }

    @Test
    void removeGame() {
        GameObject game = new GameObject("ABC123");
        gameStore.putGame("ABC123", game);
        GameObject removed = gameStore.removeGame("ABC123");
        assertEquals(game, removed);
        assertFalse(gameStore.containsGame("ABC123"));
    }

    @Test
    void removeNonExistentReturnsNull() {
        assertNull(gameStore.removeGame("NONE"));
    }

    @Test
    void size() {
        assertEquals(0, gameStore.size());
        gameStore.putGame("A", new GameObject("A"));
        gameStore.putGame("B", new GameObject("B"));
        assertEquals(2, gameStore.size());
    }

    @Test
    void threadSafety() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final String code = "GAME" + i;
            threads[i] = new Thread(() -> gameStore.putGame(code, new GameObject(code)));
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(threadCount, gameStore.size());
    }
}
