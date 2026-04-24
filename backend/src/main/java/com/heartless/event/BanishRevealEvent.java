package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.Vote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BanishRevealEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public BanishRevealEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return false;
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setRevealEnabled(true);
        mc.setAllChatEnabled(true);
        mc.setGameLogsEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.BANISH_REVEAL_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The banish vote results are being revealed.";
    }

    @Override
    public String getInitialMessage() {
        return "The votes are in. See who the group has chosen to banish.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Returns the banish votes as a timed sequence of reveal actions.
     * Votes are revealed one at a time, 5 seconds apart, starting 5 seconds
     * after the reveal event begins.
     */
    @Override
    public List<EventAction> getEvents() {
        if (startTime == null) return null;
        List<Vote> votes = game.getBanishVotes();
        if (votes.isEmpty()) return List.of();
        Map<String, UserSelectionsState> selStates = game.getSelectionStateMap();
        List<EventAction> actions = new ArrayList<>();
        long intervalMs = 5000L;
        for (int i = 0; i < votes.size(); i++) {
            Vote v = votes.get(i);
            UserSelectionsState state = selStates.get(v.getCastingPlayer().getId());
            String textInput = (state != null) ? state.getTextFieldInput() : "";
            Map<String, Object> action = new HashMap<>();
            action.put("player", v.getCastingPlayer().getName());
            action.put("vote", v.getReceivingPlayer().getName());
            action.put("string", textInput);
            long executeTime = startTime + 5000L + (long) i * intervalMs;
            actions.add(new EventAction(action, executeTime));
        }
        return actions;
    }
}
