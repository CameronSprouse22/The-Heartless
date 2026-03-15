package com.heartless.service;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TraitorSelectionServiceTest {

    @Test
    void calculateTraitorCountMinimum4Players() {
        assertEquals(1, TraitorSelectionService.calculateTraitorCount(4));
    }

    @Test
    void calculateTraitorCount5Players() {
        assertEquals(1, TraitorSelectionService.calculateTraitorCount(5));
    }

    @Test
    void calculateTraitorCount6Players() {
        assertEquals(1, TraitorSelectionService.calculateTraitorCount(6));
    }

    @Test
    void calculateTraitorCount10Players() {
        assertEquals(2, TraitorSelectionService.calculateTraitorCount(10));
    }

    @Test
    void calculateTraitorCount15Players() {
        assertEquals(3, TraitorSelectionService.calculateTraitorCount(15));
    }

    @Test
    void calculateTraitorCount52Players() {
        assertEquals(10, TraitorSelectionService.calculateTraitorCount(52));
    }

    @Test
    void calculateTraitorCountRejectsBelow4() {
        assertThrows(IllegalArgumentException.class,
                () -> TraitorSelectionService.calculateTraitorCount(3));
    }

    @Test
    void selectTraitorIndicesDeterministic() {
        Set<Integer> result1 = TraitorSelectionService.selectTraitorIndices(10, new Random(42));
        Set<Integer> result2 = TraitorSelectionService.selectTraitorIndices(10, new Random(42));
        assertEquals(result1, result2);
    }

    @Test
    void selectTraitorIndicesCorrectCount() {
        Set<Integer> result = TraitorSelectionService.selectTraitorIndices(10, new Random(42));
        assertEquals(2, result.size()); // 10 / 5 = 2
    }

    @Test
    void selectTraitorIndicesWithinBounds() {
        Set<Integer> result = TraitorSelectionService.selectTraitorIndices(10, new Random(42));
        for (int idx : result) {
            assertTrue(idx >= 0 && idx < 10);
        }
    }

    @Test
    void selectTraitorIndicesUniqueValues() {
        Set<Integer> result = TraitorSelectionService.selectTraitorIndices(20, new Random(99));
        assertEquals(4, result.size()); // 20 / 5 = 4, all unique by Set nature
    }
}
