package com.heartless.service;

import com.heartless.model.*;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameEventServiceTest {

    private GameEventService gameEventService;
    private GameStore gameStore;
    private GameService gameService;
    private GameObject game;
    private Player player1;
    private Player player2;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
        gameService = null; // will be set after creating game
        game = new GameObject("GAME01");
        game.transitionToStart();

        player1 = new Player("Alice", "alice@test.com", null);
        player1.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(player1);

        player2 = new Player("Bob", "bob@test.com", null);
        player2.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(player2);

        gameStore.putGame("GAME01", game);
         gameEventService = new GameEventService(gameStore, null, null);
    }

    private GameEventConfig makeConfig(String gameCode, List<String> items, boolean singleAnswer) {
        return new GameEventConfig(gameCode, "Test Event", "Pick one", items,
                singleAnswer, false, 1, 0, 0, false, false);
    }

    // --- createEvent tests ---

    @Test
    void createEventSuccess() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave", "River"), true);
        GameEventState state = gameEventService.createEvent(config);

        assertNotNull(state);
        assertEquals(config, state.getConfig());
        assertFalse(state.isResolved());
        // Should have PlayerSelection entries for both players
        assertEquals(2, state.getSelections().size());
        assertNotNull(state.getSelections().get(player1.getId()));
        assertNotNull(state.getSelections().get(player2.getId()));
    }

    @Test
    void createEventInitializesPlayerNames() {
        GameEventConfig config = makeConfig("GAME01", List.of("A", "B"), false);
        GameEventState state = gameEventService.createEvent(config);

        assertEquals("Alice", state.getSelections().get(player1.getId()).getPlayerName());
        assertEquals("Bob", state.getSelections().get(player2.getId()).getPlayerName());
    }

    @Test
    void createEventDuplicateActiveReturnsConflict() {
        GameEventConfig config = makeConfig("GAME01", List.of("A", "B"), false);
        gameEventService.createEvent(config);

        GameEventConfig config2 = makeConfig("GAME01", List.of("X", "Y"), false);
        assertThrows(IllegalStateException.class, () -> gameEventService.createEvent(config2));
    }

    @Test
    void createEventGameNotFoundThrows() {
        GameEventConfig config = makeConfig("NONEXIST", List.of("A"), false);
        assertThrows(IllegalArgumentException.class, () -> gameEventService.createEvent(config));
    }

    // --- saveSelection tests ---

    @Test
    void saveSelectionPersists() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(Set.of("Forest"), sel.getSelectedItems());
        assertEquals(SubmissionStatus.SELECTED, sel.getSubmissionStatus());
    }

    @Test
    void saveSelectionSingleAnswerEnforcesOneItem() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave", "River"), true);
        gameEventService.createEvent(config);

        assertThrows(IllegalArgumentException.class,
                () -> gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest", "Cave"), null));
    }

    @Test
    void saveSelectionMultiSelectAllowsMultiple() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave", "River"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest", "Cave"), null);

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(Set.of("Forest", "Cave"), sel.getSelectedItems());
    }

    @Test
    void saveSelectionRejectsInvalidItem() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave"), false);
        gameEventService.createEvent(config);

        assertThrows(IllegalArgumentException.class,
                () -> gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Mountain"), null));
    }

    @Test
    void saveSelectionReSelectOverwrites() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave", "River"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Cave", "River"), null);

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(Set.of("Cave", "River"), sel.getSelectedItems());
    }

    @Test
    void saveSelectionNoActiveEventThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null));
    }

    // --- getEventState tests ---

    @Test
    void getEventStateReturnsActive() {
        GameEventConfig config = makeConfig("GAME01", List.of("A"), false);
        gameEventService.createEvent(config);

        GameEventState state = gameEventService.getEventState("GAME01");
        assertNotNull(state);
        assertEquals("GAME01", state.getConfig().getGameCode());
    }

    @Test
    void getEventStateReturnsNullWhenNone() {
        assertNull(gameEventService.getEventState("GAME01"));
    }

    // --- submitSelections tests (T022) ---

    private GameEventConfig makeSubmitConfig(int min, int max, boolean inputString) {
        return new GameEventConfig("GAME01", "Submit Test", null, List.of("A", "B", "C"),
                false, false, min, max, 0, inputString, false);
    }

    @Test
    void submitSelectionsSuccess() {
        GameEventConfig config = makeSubmitConfig(1, 0, false);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);

        gameEventService.submitSelections("GAME01", player1.getId());

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(SubmissionStatus.SUBMITTED, sel.getSubmissionStatus());
    }

    @Test
    void submitSelectionsMinCountNotMetRejects() {
        GameEventConfig config = makeSubmitConfig(2, 0, false);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);

        assertThrows(IllegalStateException.class,
                () -> gameEventService.submitSelections("GAME01", player1.getId()));
    }

    @Test
    void submitSelectionsMaxCountExceededRejects() {
        GameEventConfig config = makeSubmitConfig(1, 2, false);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A", "B", "C"), null);

        assertThrows(IllegalStateException.class,
                () -> gameEventService.submitSelections("GAME01", player1.getId()));
    }

    @Test
    void submitSelectionsInputStringRequiredButEmpty() {
        GameEventConfig config = makeSubmitConfig(1, 0, true);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);

        assertThrows(IllegalStateException.class,
                () -> gameEventService.submitSelections("GAME01", player1.getId()));
    }

    @Test
    void submitSelectionsInputStringProvidedSucceeds() {
        GameEventConfig config = makeSubmitConfig(1, 0, true);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), "my answer");

        gameEventService.submitSelections("GAME01", player1.getId());

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(SubmissionStatus.SUBMITTED, sel.getSubmissionStatus());
    }

    @Test
    void submitSelectionsAlreadySubmittedRejects() {
        GameEventConfig config = makeSubmitConfig(1, 0, false);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);
        gameEventService.submitSelections("GAME01", player1.getId());

        assertThrows(IllegalStateException.class,
                () -> gameEventService.submitSelections("GAME01", player1.getId()));
    }

    // --- broadcast / getPlayerSelections tests (T026) ---

    @Test
    void getPlayerSelectionsReturnsAllPlayers() {
        GameEventConfig config = new GameEventConfig("GAME01", "Vis Test", null, List.of("A", "B"),
                false, true, 1, 0, 0, false, false);
        gameEventService.createEvent(config);
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);

        Map<String, PlayerSelection> selections = gameEventService.getPlayerSelections("GAME01");
        assertEquals(2, selections.size());
        assertEquals(Set.of("A"), selections.get(player1.getId()).getSelectedItems());
        assertTrue(selections.get(player2.getId()).getSelectedItems().isEmpty());
    }

    @Test
    void getPlayerSelectionsNoActiveEventThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> gameEventService.getPlayerSelections("GAME01"));
    }

    // --- agreement flow tests (T031) ---

    private GameEventConfig makeAgreeConfig() {
        return new GameEventConfig("GAME01", "Agree Test", null, List.of("Forest", "Cave"),
                false, true, 1, 0, 0, false, true);
    }

    @Test
    void agreementAllSubmitSameSelectionsResolvesEvent() {
        gameEventService.createEvent(makeAgreeConfig());

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.submitSelections("GAME01", player1.getId());

        gameEventService.saveSelection("GAME01", player2.getId(), Set.of("Forest"), null);
        gameEventService.submitSelections("GAME01", player2.getId());

        GameEventState state = gameEventService.getEventState("GAME01");
        assertTrue(state.isResolved());
        assertEquals(ResolutionType.AGREEMENT_REACHED, state.getResult().getResolutionType());
    }

    @Test
    void disagreementResetsAllSubmissions() {
        gameEventService.createEvent(makeAgreeConfig());

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.submitSelections("GAME01", player1.getId());

        gameEventService.saveSelection("GAME01", player2.getId(), Set.of("Cave"), null);
        gameEventService.submitSelections("GAME01", player2.getId());

        GameEventState state = gameEventService.getEventState("GAME01");
        assertFalse(state.isResolved());
        // Both should be reset to SELECTED
        assertEquals(SubmissionStatus.SELECTED, state.getSelections().get(player1.getId()).getSubmissionStatus());
        assertEquals(SubmissionStatus.SELECTED, state.getSelections().get(player2.getId()).getSubmissionStatus());
    }

    @Test
    void cancelSubmitRevertsToSelected() {
        gameEventService.createEvent(makeAgreeConfig());

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.submitSelections("GAME01", player1.getId());

        gameEventService.cancelSubmit("GAME01", player1.getId());

        PlayerSelection sel = gameEventService.getEventState("GAME01").getSelections().get(player1.getId());
        assertEquals(SubmissionStatus.SELECTED, sel.getSubmissionStatus());
    }

    @Test
    void cancelSubmitWhenNotAgreeModeRejects() {
        // Use a non-agree config
        GameEventConfig config = makeConfig("GAME01", List.of("A", "B"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("A"), null);
        gameEventService.submitSelections("GAME01", player1.getId());

        assertThrows(IllegalStateException.class,
                () -> gameEventService.cancelSubmit("GAME01", player1.getId()));
    }

    @Test
    void cancelSubmitWhenNotSubmittedRejects() {
        gameEventService.createEvent(makeAgreeConfig());
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);

        assertThrows(IllegalStateException.class,
                () -> gameEventService.cancelSubmit("GAME01", player1.getId()));
    }

    // --- timeout flow tests (T035) ---

    @Test
    void resolveOnTimeoutBuildsResultWithSavedSelections() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave", "River"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest", "Cave"), null);
        gameEventService.saveSelection("GAME01", player2.getId(), Set.of("River"), null);

        gameEventService.resolveOnTimeout("GAME01");

        GameEventState state = gameEventService.getEventState("GAME01");
        assertTrue(state.isResolved());
        assertEquals(ResolutionType.TIMEOUT, state.getResult().getResolutionType());
        assertNotNull(state.getResult().getResolvedAt());
    }

    @Test
    void resolveOnTimeoutAbstainingPlayersHaveEmptySelections() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave"), false);
        gameEventService.createEvent(config);

        // Only player1 selects; player2 abstains
        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);

        gameEventService.resolveOnTimeout("GAME01");

        GameEventState state = gameEventService.getEventState("GAME01");
        assertTrue(state.isResolved());

        Map<String, PlayerSelection> finalSelections = state.getResult().getFinalSelections();
        assertEquals(Set.of("Forest"), finalSelections.get(player1.getId()).getSelectedItems());
        assertTrue(finalSelections.get(player2.getId()).getSelectedItems().isEmpty());
    }

    @Test
    void resolvedEventRejectsSaveSelection() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave"), false);
        gameEventService.createEvent(config);

        gameEventService.resolveOnTimeout("GAME01");

        assertThrows(IllegalStateException.class,
                () -> gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null));
    }

    @Test
    void resolvedEventRejectsSubmitSelections() {
        GameEventConfig config = makeConfig("GAME01", List.of("Forest", "Cave"), false);
        gameEventService.createEvent(config);

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.resolveOnTimeout("GAME01");

        assertThrows(IllegalStateException.class,
                () -> gameEventService.submitSelections("GAME01", player1.getId()));
    }

    @Test
    void resolveOnTimeoutOverridesPlayersMustAgree() {
        gameEventService.createEvent(makeAgreeConfig());

        gameEventService.saveSelection("GAME01", player1.getId(), Set.of("Forest"), null);
        gameEventService.submitSelections("GAME01", player1.getId());
        // player2 hasn't submitted — agreement not reached yet

        gameEventService.resolveOnTimeout("GAME01");

        GameEventState state = gameEventService.getEventState("GAME01");
        assertTrue(state.isResolved());
        assertEquals(ResolutionType.TIMEOUT, state.getResult().getResolutionType());
    }
}
