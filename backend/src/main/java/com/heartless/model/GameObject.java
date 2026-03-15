package com.heartless.model;

import com.heartless.event.EventObjectInterface;
import com.heartless.model.enums.GameStageEnum;
import com.heartless.model.enums.GameStatusEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central game state container. One per active game session.
 */
public class GameObject {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final long gameId;
    private final String gameIdCode;
    private boolean isGameActive;
    private GameStatusEnum gameStatus;
    private final List<Player> playerList;
    private final List<RoundObject> roundList;
    private GameStageEnum currentStage;
    private int round;
    private String currentTask;
    private final List<EventObjectInterface> eventLog;
    private Long startGameTime;
    private Long endGameTime;
    private String lastMurderId;

    public GameObject(String gameIdCode) {
        this.gameId = ID_GENERATOR.getAndIncrement();
        this.gameIdCode = gameIdCode;
        this.isGameActive = true;
        this.gameStatus = GameStatusEnum.INIT;
        this.playerList = new ArrayList<>();
        this.roundList = new ArrayList<>();
        this.currentStage = GameStageEnum.IN_START;
        this.round = -1;
        this.currentTask = null;
        this.eventLog = new ArrayList<>();
        this.startGameTime = null;
        this.endGameTime = null;
        this.lastMurderId = null;
    }

    // --- State transitions ---

    public void transitionToStart() {
        if (gameStatus != GameStatusEnum.INIT) {
            throw new IllegalStateException("Can only start from INIT state");
        }
        this.gameStatus = GameStatusEnum.START;
        this.currentStage = GameStageEnum.NORMAL_ROUNDS;
        this.round = 1;
        this.startGameTime = System.currentTimeMillis();
    }

    public void transitionToEnd() {
        if (gameStatus != GameStatusEnum.START) {
            throw new IllegalStateException("Can only end from START state");
        }
        this.gameStatus = GameStatusEnum.END;
        this.currentStage = GameStageEnum.FINAL_ROUND;
        this.round = 0;
    }

    public void transitionToOver() {
        if (gameStatus != GameStatusEnum.END) {
            throw new IllegalStateException("Can only finish from END state");
        }
        this.gameStatus = GameStatusEnum.OVER;
        this.isGameActive = false;
        this.endGameTime = System.currentTimeMillis();
    }

    public void incrementRound() {
        this.round++;
    }

    public void addPlayer(Player player) {
        this.playerList.add(player);
    }

    public void addRound(RoundObject roundObject) {
        this.roundList.add(roundObject);
    }

    public void addEvent(EventObjectInterface event) {
        this.eventLog.add(event);
    }

    // --- Getters & Setters ---

    public long getGameId() { return gameId; }
    public String getGameIdCode() { return gameIdCode; }
    public boolean isGameActive() { return isGameActive; }
    public void setGameActive(boolean gameActive) { isGameActive = gameActive; }
    public GameStatusEnum getGameStatus() { return gameStatus; }
    public void setGameStatus(GameStatusEnum gameStatus) { this.gameStatus = gameStatus; }
    public List<Player> getPlayerList() { return playerList; }
    public List<RoundObject> getRoundList() { return roundList; }
    public GameStageEnum getCurrentStage() { return currentStage; }
    public void setCurrentStage(GameStageEnum currentStage) { this.currentStage = currentStage; }
    public int getRound() { return round; }
    public void setRound(int round) { this.round = round; }
    public String getCurrentTask() { return currentTask; }
    public void setCurrentTask(String currentTask) { this.currentTask = currentTask; }
    public List<EventObjectInterface> getEventLog() { return eventLog; }
    public Long getStartGameTime() { return startGameTime; }
    public void setStartGameTime(Long startGameTime) { this.startGameTime = startGameTime; }
    public Long getEndGameTime() { return endGameTime; }
    public void setEndGameTime(Long endGameTime) { this.endGameTime = endGameTime; }
    public String getLastMurderId() { return lastMurderId; }
    public void setLastMurderId(String lastMurderId) { this.lastMurderId = lastMurderId; }

    /**
     * Find a player by their unique ID.
     */
    public Player findPlayerById(String playerId) {
        return playerList.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find a player by their display name (case-insensitive).
     */
    public Player findPlayerByName(String name) {
        return playerList.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the VIP (first player added to the game).
     */
    public Player getVip() {
        return playerList.isEmpty() ? null : playerList.get(0);
    }

    /**
     * Count active (non-removed) players.
     */
    public int getActivePlayerCount() {
        return (int) playerList.stream()
                .filter(p -> p.getStatus() != com.heartless.model.enums.PlayerStatusEnum.REMOVED)
                .count();
    }
}
