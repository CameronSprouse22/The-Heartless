package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;
import java.util.Map;

/**
 * Situation Report event — shown at the start of each round.
 * Displays the current round number, which players are alive or dead,
 * who was murdered, who was banished, and which traitors were banished.
 *
 * Ends when every active (non-dead, ACTIVE) player acknowledges the report,
 * or when the event timer expires.
 */
public class SitRepEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public SitRepEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean isShowToDeadPlayers() { return true; }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        // Fresh selection state per active player so we can track acknowledgements
        game.initSelectionStates(
                game.getPlayerList().stream()
                        .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                        .map(p -> p.getId())
                        .toList()
        );
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        // Ends early when every active player has pressed Confirm
        Map<String, UserSelectionsState> stateMap = gameObject.getSelectionStateMap();
        if (stateMap == null || stateMap.isEmpty()) return false;
        return stateMap.values().stream().allMatch(UserSelectionsState::isSubmitPressed);
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setSitRepEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.SITREP_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Situation Report — Round " + game.getRound();
    }

    @Override
    public String getInitialMessage() {
        return "Review the current game state below. Acknowledge when you are ready to continue.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
