package com.heartless.service;

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

    public GameObject getGame(String gameCode) {
        return games.get(gameCode);
    }

    public void putGame(String gameCode, GameObject game) {
        games.put(gameCode, game);
    }

    public GameObject removeGame(String gameCode) {
        return games.remove(gameCode);
    }

    public boolean containsGame(String gameCode) {
        return games.containsKey(gameCode);
    }

    public int size() {
        return games.size();
    }
}
