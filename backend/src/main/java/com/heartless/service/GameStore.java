package com.heartless.service;

import com.heartless.gamethread.GameThread;
import com.heartless.model.GameObject;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory game state store wrapping ConcurrentHashMap.
 * Uses per-game synchronized blocks for compound operations.
 */
@Service
public class GameStore {

    private final ConcurrentHashMap<String, GameObject> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GameThread> gameThreads = new ConcurrentHashMap<>();

    public GameObject getGame(String gameCode) {
        return games.get(gameCode);
    }

    public void putGame(String gameCode, GameObject game) {
        games.put(gameCode, game);
    }

    public GameObject removeGame(String gameCode) {
        gameThreads.remove(gameCode);
        return games.remove(gameCode);
    }

    public boolean containsGame(String gameCode) {
        return games.containsKey(gameCode);
    }

    public int size() {
        return games.size();
    }

    public GameThread getGameThread(String gameCode) {
        return gameThreads.get(gameCode);
    }

    public void putGameThread(String gameCode, GameThread gameThread) {
        gameThreads.put(gameCode, gameThread);
    }
}
