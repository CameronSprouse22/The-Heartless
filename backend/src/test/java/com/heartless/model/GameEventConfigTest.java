package com.heartless.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEventConfigTest {

    private static final long FUTURE_TIME = System.currentTimeMillis() + 60000;

    @Test
    void constructionWithValidFieldsSucceeds() {
        GameEventConfig config = new GameEventConfig("GAME01", "Pick a card", "Choose wisely",
                List.of("A", "B", "C"), true, false, 1, 0, FUTURE_TIME, false, false);

        assertEquals("GAME01", config.getGameCode());
        assertEquals("Pick a card", config.getTitle());
        assertEquals("Choose wisely", config.getPrompt());
        assertEquals(List.of("A", "B", "C"), config.getListOfItems());
        assertTrue(config.isSingleAnswer());
        assertFalse(config.isShowOthersSelections());
        assertEquals(1, config.getMinNumberSelectedToSubmit());
        assertEquals(0, config.getMaxNumberSelectedToSubmit());
        assertTrue(config.getEventId().startsWith("evt-"));
    }

    @Test
    void blankTitleThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameEventConfig("GAME01", "  ", null, List.of("A"), false, false, 0, 0, FUTURE_TIME, false, false));
    }

    @Test
    void nullTitleThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameEventConfig("GAME01", null, null, List.of("A"), false, false, 0, 0, FUTURE_TIME, false, false));
    }

    @Test
    void nullListOfItemsThrows() {
        assertThrows(NullPointerException.class, () ->
                new GameEventConfig("GAME01", "Title", null, null, false, false, 0, 0, FUTURE_TIME, false, false));
    }

    @Test
    void minExceedsListSizeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameEventConfig("GAME01", "Title", null, List.of("A", "B"), false, false, 5, 0, FUTURE_TIME, false, false));
    }

    @Test
    void maxLessThanMinThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                new GameEventConfig("GAME01", "Title", null, List.of("A", "B", "C"), false, false, 2, 1, FUTURE_TIME, false, false));
    }

    @Test
    void emptyListOfItemsIsValid() {
        GameEventConfig config = new GameEventConfig("GAME01", "Title", null, List.of(), false, false, 0, 0, FUTURE_TIME, false, false);
        assertTrue(config.getListOfItems().isEmpty());
    }

    @Test
    void listOfItemsIsImmutable() {
        GameEventConfig config = new GameEventConfig("GAME01", "Title", null, List.of("A", "B"), false, false, 0, 0, FUTURE_TIME, false, false);
        assertThrows(UnsupportedOperationException.class, () -> config.getListOfItems().add("C"));
    }

    @Test
    void nullPromptIsAllowed() {
        GameEventConfig config = new GameEventConfig("GAME01", "Title", null, List.of("A"), false, false, 0, 0, FUTURE_TIME, false, false);
        assertNull(config.getPrompt());
    }

    @Test
    void maxZeroMeansNoLimit() {
        GameEventConfig config = new GameEventConfig("GAME01", "Title", null, List.of("A", "B", "C"), false, false, 2, 0, FUTURE_TIME, false, false);
        assertEquals(0, config.getMaxNumberSelectedToSubmit());
    }
}
