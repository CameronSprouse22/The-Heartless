package com.heartless.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Selects traitors from a player list.
 * Pure logic: 1 traitor per 5 players, minimum 1.
 */
@Service
public class TraitorSelectionService {

    /**
     * Determine traitor count: 1 traitor per 5 players, minimum 1.
     * 4-5 players  → 1 traitor
     * 6-10 players → 2 traitors
     * 11-15 players → 3 traitors
     */
    public static int calculateTraitorCount(int playerCount) {
        if (playerCount < 4) {
            throw new IllegalArgumentException("Minimum 4 players required");
        }
        return Math.max(1, playerCount / 5);
    }

    /**
     * Select traitor indices from a player list.
     * Returns an unmodifiable set of indices.
     * Pure function: given same Random seed, produces identical output.
     */
    public static Set<Integer> selectTraitorIndices(int playerCount, Random random) {
        int traitorCount = calculateTraitorCount(playerCount);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(indices.subList(0, traitorCount))
        );
    }
}
