package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.service.VotingService;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MurderVoteEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;
    private MiniGameEvent miniGameEvent = null;
    private volatile boolean miniGameDone = false;
    private final Set<String> miniGameCompletedPlayerIds = ConcurrentHashMap.newKeySet();
    private volatile boolean murderApplied = false;
    private VotingService votingService;
    private String gameCode;

    public MurderVoteEvent(GameObject game) {
        this.game = game;
    }

    /** Attach a mini game that will run before the murder vote. */
    public void setMiniGameEvent(MiniGameEvent miniGameEvent) {
        this.miniGameEvent = miniGameEvent;
    }

    public MiniGameEvent getMiniGameEvent() { return miniGameEvent; }
    public boolean isMiniGameDone()         { return miniGameDone;  }
    public boolean hasPlayerCompletedMiniGame(String playerId) { return miniGameCompletedPlayerIds.contains(playerId); }

    /** Attach the voting service so murder results can be applied when all traitors vote. */
    public void setVotingService(VotingService vs, String gameCode) {
        this.votingService = vs;
        this.gameCode = gameCode;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        if (miniGameEvent != null) {
            miniGameEvent.setOverrideEndTime(getEventEndTime()); // share the single timer
            miniGameEvent.checkStartConditions(); // inits selection states for ALL players
            miniGameEvent.onStart();
        } else {
            // No mini game — traitors go straight to the murder vote phase.
            // Mark all active traitors as having "completed" the mini game so the
            // routing in GameThread.buildGameState correctly sends them to murder vote.
            List<String> traitorIds = game.getPlayerList().stream()
                    .filter(p -> !p.isDead()
                            && p.getStatus() == PlayerStatusEnum.ACTIVE
                            && p.isTraitor())
                    .map(Player::getId)
                    .toList();
            miniGameCompletedPlayerIds.addAll(traitorIds);
            game.initSelectionStates(traitorIds);
        }
    }

    /** Switch from mini game phase to murder vote phase, initialising only traitor selection states. */
    private void transitionToMurderVote() {
        miniGameDone = true;
        game.initSelectionStates(
                game.getPlayerList().stream()
                        .filter(p -> !p.isDead()
                                && p.getStatus() == PlayerStatusEnum.ACTIVE
                                && p.isTraitor())
                        .map(Player::getId)
                        .toList()
        );
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        java.util.List<Player> active = game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .toList();

        // Guard: if no active players exist yet, don't end the event prematurely
        // (Java's allMatch on an empty stream returns true, which would trigger a false-positive)
        if (active.isEmpty()) return false;

        // Promote any traitor who just finished the mini game to the murder vote phase
        if (miniGameEvent != null) {
            active.stream()
                    .filter(p -> p.isTraitor() && !miniGameCompletedPlayerIds.contains(p.getId()))
                    .forEach(p -> {
                        UserSelectionsState sel = game.getSelectionState(p.getId());
                        if (sel != null && sel.isSubmitPressed()) {
                            miniGameCompletedPlayerIds.add(p.getId());
                            game.resetSelectionState(p.getId()); // fresh state for murder vote
                        }
                    });
        }

        // All faithful must have completed the mini game (submitPressed on their selection state).
        // If there is no mini game, faithful have nothing to do — treat them as done.
        boolean allFaithfulDone = miniGameEvent == null || active.stream()
                .filter(p -> !p.isTraitor())
                .allMatch(p -> {
                    UserSelectionsState sel = game.getSelectionState(p.getId());
                    return sel != null && sel.isSubmitPressed();
                });

        // All traitors must have been promoted past the mini game AND submitted a murder vote
        boolean allTraitorsDone = active.stream()
                .filter(Player::isTraitor)
                .allMatch(p -> {
                    if (!miniGameCompletedPlayerIds.contains(p.getId())) return false;
                    UserSelectionsState sel = game.getSelectionState(p.getId());
                    return sel != null && sel.isSubmitPressed();
                });

        if (allFaithfulDone && allTraitorsDone) {
            applyMurder();
            return true;
        }
        return false;
    }

    @Override
    public void resolveEvent() {
        applyMurder(); // called on timeout; flag prevents double-application
    }

    /** Tally murder votes and mark the plurality winner as MARKED_FOR_MURDER. */
    private void applyMurder() {
        if (murderApplied) return;
        murderApplied = true;
        if (votingService != null && gameCode != null) {
            votingService.applyMurderResult(gameCode, game);
        }
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    /**
     * Player-specific state:
     * - Faithful players see the mini game for the full duration
     * - Traitors see the murder vote + traitor chat alongside the mini game
     */
    @Override
    public GameState getGameState(Player player) {
        MenuControl mc = new MenuControl();
        if (!player.isTraitor()) {
            // Faithful players only see the mini game for the full duration
            mc.setMiniGameEnabled(true);
        } else if (miniGameCompletedPlayerIds.contains(player.getId())) {
            // Traitor has finished the mini game — show the murder vote
            mc.setMurderVoteEnabled(true);
            mc.setTraitorChatEnabled(true);
        } else {
            // Traitor has not yet finished the mini game
            mc.setMiniGameEnabled(true);
        }
        return GameState.fromEvent(mc, game, this);
    }

    /** Role-agnostic fallback (used by the interface default when no player is available). */
    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setMurderVoteEnabled(true);
        mc.setTraitorChatEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.MURDER_VOTE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return miniGameEvent != null ? miniGameEvent.getStartNotification() : "The murder phase has begun.";
    }

    @Override
    public String getInitialMessage() {
        return miniGameEvent != null ? miniGameEvent.getInitialMessage()
                : "The night falls. Traitors — select your target carefully.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
