package com.heartless.gamethread;

import com.heartless.event.EventObjectInterface;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.GameStatusEnum;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameThreadTest {

    private GameObject game;
    private GameCriteriaObject criteria;

    @BeforeEach
    void setUp() {
        game = new GameObject("THRD01");
        criteria = new GameCriteriaObject();
    }

    private Player createActive(String name, boolean isTraitor) {
        Player p = new Player(name, name.toLowerCase() + "@test.com", null);
        p.setStatus(PlayerStatusEnum.ACTIVE);
        p.setTraitor(isTraitor);
        game.addPlayer(p);
        return p;
    }

    @Test
    void gameInitSetsInitStatus() {
        GameThread thread = new GameThread(game, criteria, List.of());
        // Game should already be in INIT before gameInit
        assertEquals(GameStatusEnum.INIT, game.getGameStatus());
        thread.gameInit();
        // gameInit calls gameStart, but with no players the loop exits immediately
        assertEquals(GameStatusEnum.INIT, game.getGameStatus());
        // statusString ends at whatever gameStart set (no rounds ran, so "Round -1")
        assertTrue(thread.getStatusString().contains("Round"));
    }

    @Test
    void gameStartTransitionsToStart() {
        // Setup: traitor that will be killed during first round
        createActive("A", false);
        createActive("B", false);
        createActive("C", false);
        Player traitor = createActive("T", true);

        // Create an event that kills the traitor, ending the game
        EventObjectInterface killTraitor = new StubEvent(game, "Kill", new ArrayList<>()) {
            @Override
            public void execute() {
                traitor.setDead(true);
                super.execute();
            }
        };

        GameThread thread = new GameThread(game, criteria, List.of(killTraitor));
        thread.gameInit();
        game.transitionToStart(); // Simulates VIP starting
        thread.gameStart();

        // After gameStart runs through rounds and criteria fails, game stays START
        assertEquals(GameStatusEnum.START, game.getGameStatus());
    }

    @Test
    void gameEndTransitionsToOver() {
        createActive("A", false);
        createActive("B", false);
        Player traitor = createActive("T", true);
        traitor.setDead(true); // All traitors dead -> faithful win

        game.transitionToStart();
        game.transitionToEnd();

        GameThread thread = new GameThread(game, criteria, List.of());
        thread.gameEnd();

        assertEquals(GameStatusEnum.OVER, game.getGameStatus());
        assertFalse(game.isGameActive());
    }

    @Test
    void statusStringUpdates() {
        createActive("A", false);
        createActive("B", false);
        createActive("C", false);
        Player traitor = createActive("T", true);
        // Event that ends the game by killing traitor
        EventObjectInterface killEvent = new StubEvent(game, "Kill", new ArrayList<>()) {
            @Override
            public void execute() {
                traitor.setDead(true);
                super.execute();
            }
        };
        game.transitionToStart();
        GameThread thread = new GameThread(game, criteria, List.of(killEvent));
        thread.gameInit();
        // gameInit calls gameStart which runs the loop
        assertTrue(thread.getStatusString().contains("Round"));
    }

    @Test
    void getGameReturnsGameObject() {
        GameThread thread = new GameThread(game, criteria, List.of());
        assertSame(game, thread.getGameObject());
    }

    // --- Stub event for testing ---
    private static class StubEvent implements EventObjectInterface {
        private final GameObject game;
        private final String name;
        private final List<String> log;

        StubEvent(GameObject game, String name, List<String> log) {
            this.game = game;
            this.name = name;
            this.log = log;
        }

        @Override
        public boolean checkStartConditions() { return true; }

        @Override
        public boolean endConditonsMeet(com.heartless.model.GameObject gameObject) { return true; }

        @Override
        public ArrayList<UserSelectionsState> getUsersSelections() {
            return new ArrayList<>(game.getSelectionStateMap().values());
        }

        @Override
        public GameState getGameState() {
            return GameState.fromMenuControl(new MenuControl(), game);
        }

        @Override
        public long getEventTime() { return 300_000L; }

        @Override
        public long getEventEndTime() { return System.currentTimeMillis() + 300_000L; }

        @Override
        public String getStartNotification() { return name + " started."; }

        @Override
        public boolean checkForNotifications() { return true; }

        @Override
        public void execute() { log.add(name); }
    }
}
