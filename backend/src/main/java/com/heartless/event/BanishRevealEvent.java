package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.RoundObject;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.model.enums.PlayerLifeStatusEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BanishRevealEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;
    /** Snapshot of each player's text input from the vote phase, captured before onStart() resets states. */
    private Map<String, String> voteTextSnapshot = new HashMap<>();

    public BanishRevealEvent(GameObject game) {
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
        // Snapshot text inputs from the vote phase before reinitializing selection states
        game.getSelectionStateMap().forEach((playerId, state) -> {
            String text = state.getTextFieldInput();
            if (text != null && !text.isBlank()) {
                voteTextSnapshot.put(playerId, text);
            }
        });
        game.initSelectionStates(game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(p -> p.getId())
                .toList());

        // Apply banishment: the player receiving the most votes is banished
        List<Vote> votes = game.getBanishVotes();
        if (!votes.isEmpty()) {
            Map<String, Long> tally = votes.stream()
                    .collect(Collectors.groupingBy(v -> v.getReceivingPlayer().getId(), Collectors.counting()));
            
            long maxVotes = tally.values().stream().max(Long::compare).orElse(0L);
            List<String> tiedPlayerIds = tally.entrySet().stream()
                    .filter(e -> e.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .toList();
            
            if (tiedPlayerIds.size() > 1) {
                // It's a tie, no banishment yet. Register for tie-break.
                game.setTieBreakCandidateIds(tiedPlayerIds);
            } else {
                // No tie, process the winner
                String winnerId = tiedPlayerIds.get(0);
                game.getPlayerList().stream()
                        .filter(p -> p.getId().equals(winnerId))
                        .findFirst()
                        .ifPresent(winner -> {
                            winner.setLifeStatus(PlayerLifeStatusEnum.MARKED_FOR_BANISHMENT);
                            RoundObject round = game.getCurrentRound();
                            if (round != null) round.setPlayerBanished(winner);
                        });
            }
        }
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return gameObject.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .allMatch(p -> {
                    UserSelectionsState state = gameObject.getSelectionState(p.getId());
                    return state != null && state.isSubmitPressed();
                });
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setStatusEnabled(true);
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
        List<EventAction> actions = new ArrayList<>();
        long intervalMs = 5000L;
        for (int i = 0; i < votes.size(); i++) {
            Vote v = votes.get(i);
            String textInput = voteTextSnapshot.getOrDefault(v.getCastingPlayer().getId(), "");
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
