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

public class MurderVoteEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;
    private MiniGameEvent miniGameEvent = null;
    private volatile boolean miniGameDone = false;
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
            // No mini game — initialise traitor selection states directly
            game.initSelectionStates(
                    game.getPlayerList().stream()
                            .filter(p -> !p.isDead()
                                    && p.getStatus() == PlayerStatusEnum.ACTIVE
                                    && p.isTraitor())
                            .map(Player::getId)
                            .toList()
            );
        }
        // Traitors see the murder vote immediately, in parallel with the mini game
        miniGameDone = true;
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
        // Always run to the full timer so the mini game and murder vote end together.
        // Murder result is applied in resolveEvent() when the timer expires.
        return false;
    }

    @Override
    public void resolveEvent() {
        applyMurder();
    }

    /** Tally murder votes and mark the plurality winner as MARKED_FOR_MURDER. */
    private void applyMurder() {
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
        mc.setMiniGameEnabled(true); // everyone sees the mini game
        if (player.isTraitor()) {
            mc.setMurderVoteEnabled(true);
            mc.setTraitorChatEnabled(true);
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
