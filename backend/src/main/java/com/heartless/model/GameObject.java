package com.heartless.model;

import com.heartless.event.EventObjectInterface;
import com.heartless.model.enums.GameStageEnum;
import com.heartless.model.enums.GameStatusEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
    private MenuControl menuControl;
    private final Map<String, EventMessageDismissalState> initialMessageDismissalMap;
    private final List<Vote> banishVotes;
    private final List<Vote> banishSecondVotes;
    private Map<String, UserSelectionsState> selectionStateMap;
    /** IDs of the players who tied in the most recent banish vote; empty when no tiebreak is active. */
    private List<String> tieBreakCandidateIds = new ArrayList<>();

    /** Player IDs who have pressed "Ready" in the pre-game lobby. Thread-safe. */
    private final Set<String> lobbyReadyPlayerIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Set by the VIP when they initiate the game start (with or without unconfirmed players). */
    private volatile boolean lobbyVipForcedStart = false;

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
        this.menuControl = new MenuControl();
        this.selectionStateMap = new HashMap<>();
        this.initialMessageDismissalMap = new HashMap<>();
        this.banishVotes = new ArrayList<>();
        this.banishSecondVotes = new ArrayList<>();
        this.tieBreakCandidateIds = new ArrayList<>();
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

    //May not use
    public void addRound(RoundObject roundObject) {
        this.roundList.add(roundObject);
    }

    public RoundObject getCurrentRound() {
        return roundList.isEmpty() ? null : roundList.get(roundList.size() - 1);
    }

    public RoundObject startNewRound() {
        incrementRound();
        banishVotes.clear();
        banishSecondVotes.clear();
        tieBreakCandidateIds.clear();
        selectionStateMap = new HashMap<>();
        RoundObject newRound = new RoundObject(this.round);
        roundList.add(newRound);
        return newRound;
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
    public MenuControl getMenuControl() { return menuControl; }
    public void setMenuControl(MenuControl menuControl) { this.menuControl = menuControl; }

    public synchronized void addBanishVote(Vote vote) { banishVotes.add(vote); }
    public synchronized List<Vote> getBanishVotes() { return new ArrayList<>(banishVotes); }
    public synchronized void clearBanishVotes() { banishVotes.clear(); }

    public synchronized void addBanishSecondVote(Vote vote) { banishSecondVotes.add(vote); }
    public synchronized List<Vote> getBanishSecondVotes() { return new ArrayList<>(banishSecondVotes); }
    public synchronized void clearBanishSecondVotes() { banishSecondVotes.clear(); }

    public List<String> getTieBreakCandidateIds() { return tieBreakCandidateIds; }
    public void setTieBreakCandidateIds(List<String> ids) { this.tieBreakCandidateIds = ids != null ? ids : new ArrayList<>(); }
    public void clearTieBreakCandidateIds() { this.tieBreakCandidateIds = new ArrayList<>(); }
    // --- Selection State ---

    // --- Initial Message Dismissal ---

    /**
     * Resets the initial-message dismissal state for all players at the start of a new event.
     * Each player's dismissed flag is set to {@code false} so the message appears again.
     */
    public void initInitialMessageDismissalStates() {
        initialMessageDismissalMap.clear();
        for (Player player : playerList) {
            initialMessageDismissalMap.put(player.getId(), new EventMessageDismissalState(player));
        }
    }

    /**
     * Returns {@code true} if the player has already dismissed the initial event message.
     *
     * @param playerId the player's unique ID
     * @return {@code true} if dismissed, {@code false} if not yet dismissed or state not found
     */
    public boolean hasPlayerDismissedInitialMessage(String playerId) {
        EventMessageDismissalState state = initialMessageDismissalMap.get(playerId);
        return state != null && state.isDismissed();
    }

    /**
     * Marks the initial event message as dismissed for the given player.
     *
     * @param playerId the player's unique ID
     */
    public void dismissInitialMessage(String playerId) {
        EventMessageDismissalState state = initialMessageDismissalMap.get(playerId);
        if (state != null) {
            state.dismiss();
        }
    }

    // --- Selection State ---

    /**
     * Clears and re-populates the selection state map with a fresh
     * {@link UserSelectionsState} for each player ID.
     * Must be called at the start of each vote event.
     *
     * @param playerIds IDs of all players participating in the vote event
     */
    public void initSelectionStates(List<String> playerIds) {
        selectionStateMap.clear();
        for (String pid : playerIds) {
            selectionStateMap.put(pid, new UserSelectionsState());
        }
    }

    /**
     * Resets the selection state for a single player without affecting other players.
     * Used when a player transitions between phases (e.g. mini game → murder vote).
     */
    public void resetSelectionState(String playerId) {
        selectionStateMap.put(playerId, new UserSelectionsState());
    }

    /**
     * Returns the {@link UserSelectionsState} for the given player,
     * or {@code null} if the player has no entry (state not initialised).
     *
     * @param playerId the player's unique ID
     * @return the player's selection state, or {@code null}
     */
    public UserSelectionsState getSelectionState(String playerId) {
        return selectionStateMap.get(playerId);
    }

    /**
     * Returns the full selection state map (player ID → state).
     * Used by event implementations to expose all players' states.
     *
     * @return the live map — do not modify directly
     */
    public Map<String, UserSelectionsState> getSelectionStateMap() {
        return selectionStateMap;
    }

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

    // --- Lobby Ready State ---

    /** Marks a player as ready in the pre-game lobby. */
    public void markLobbyPlayerReady(String playerId) {
        lobbyReadyPlayerIds.add(playerId);
    }

    /** Removes a player's ready state (they pressed "Not Ready"). */
    public void markLobbyPlayerNotReady(String playerId) {
        lobbyReadyPlayerIds.remove(playerId);
    }

    /** Returns true if the given player has pressed Ready in the lobby. */
    public boolean isLobbyPlayerReady(String playerId) {
        return lobbyReadyPlayerIds.contains(playerId);
    }

    /** Returns an unmodifiable snapshot of all ready player IDs. */
    public Set<String> getLobbyReadyPlayerIds() {
        return Collections.unmodifiableSet(new HashSet<>(lobbyReadyPlayerIds));
    }

    /** Sets the VIP forced-start flag (bypasses ready checks). */
    public void setLobbyVipForcedStart(boolean forcedStart) {
        this.lobbyVipForcedStart = forcedStart;
    }

    /** Returns true if the VIP has triggered a forced start. */
    public boolean isLobbyVipForcedStart() {
        return lobbyVipForcedStart;
    }
}
