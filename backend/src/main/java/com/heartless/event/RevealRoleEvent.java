package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Reveals each player's own role (Traitor / Faithful) privately.
 * The event ends when every active player presses Confirm, or the timer expires.
 */
public class RevealRoleEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public RevealRoleEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        List<String> activeIds = game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(Player::getId)
                .toList();
        game.initSelectionStates(activeIds);
    }

    /**
     * Ends when every active player has set submitPressed = true (i.e. hit Confirm).
     */
    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        List<String> activeIds = gameObject.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(Player::getId)
                .toList();
        if (activeIds.isEmpty()) return false;
        return activeIds.stream().allMatch(id -> {
            UserSelectionsState state = gameObject.getSelectionState(id);
            return state != null && state.isSubmitPressed();
        });
    }

    /**
     * When time runs out, auto-confirm any players who haven't confirmed yet.
     */
    @Override
    public void resolveEvent() {
        game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .forEach(p -> {
                    UserSelectionsState state = game.getSelectionState(p.getId());
                    if (state != null) {
                        state.setSubmitPressed(true);
                    }
                });
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setRoleRevealEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.REVEAL_ROLE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Your part has been decided. Discover your role.";
    }

    @Override
    public String getInitialMessage() {
        return "The cast is set. Find out who you are.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
