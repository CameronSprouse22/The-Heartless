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
import java.util.List;
import java.util.Map;

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
            miniGameEvent.checkStartConditions();
            miniGameEvent.onStart();
        } else {
            transitionToMurderVote();
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
        if (!miniGameDone) {
            // Traitors advance to murder vote as soon as they all finish the mini game;
            // faithful players still playing don't block the transition.
            List<Player> activeTraitors = game.getPlayerList().stream()
                    .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE && p.isTraitor())
                    .toList();
            boolean allTraitorsDone = !activeTraitors.isEmpty() && activeTraitors.stream()
                    .allMatch(p -> {
                        UserSelectionsState state = game.getSelectionState(p.getId());
                        return state != null && state.isSubmitPressed();
                    });
            boolean miniGameEnded = miniGameEvent == null
                    || allTraitorsDone
                    || miniGameEvent.endConditonsMeet(gameObject)
                    || (startTime != null && System.currentTimeMillis() >= startTime + getEventTime());
            if (miniGameEnded) {
                if (miniGameEvent != null) miniGameEvent.resolveEvent();
                transitionToMurderVote();
            }
            return false; // never complete during mini game phase
        }
        // Murder vote phase: end only when ALL traitors have submitted their vote.
        // Time expiry is handled by the GameThread (getEventEndTime) — no timer check here.
        Map<String, UserSelectionsState> stateMap = gameObject.getSelectionStateMap();
        if (stateMap == null || stateMap.isEmpty()) return true; // no traitors — end immediately
        boolean allVoted = stateMap.values().stream().allMatch(UserSelectionsState::isSubmitPressed);
        if (allVoted) {
            applyMurder();
            return true;
        }
        return false;
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
     * - Mini game phase   → everyone sees the mini game
     * - Murder vote phase → traitors see the murder vote; faithful see nothing
     */
    @Override
    public GameState getGameState(Player player) {
        if (!miniGameDone && miniGameEvent != null) {
            return miniGameEvent.getGameState();
        }
        MenuControl mc = new MenuControl();
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
        if (!miniGameDone && miniGameEvent != null) {
            return miniGameEvent.getInitialMessage();
        }
        return "The night falls. Traitors \u2014 select your target carefully.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
