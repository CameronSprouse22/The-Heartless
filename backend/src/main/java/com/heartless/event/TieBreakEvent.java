package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TieBreakEvent implements EventObjectInterface {

    private final GameObject game;
    /** IDs of the players who tied — the only valid banish targets for this vote. */
    private List<String> tiedCandidateIds;
    private Long startTime = null;

    /** Used by GameThread when tie candidates are determined at runtime (stored on game object). */
    public TieBreakEvent(GameObject game) {
        this.game = game;
        this.tiedCandidateIds = new ArrayList<>();
    }

    public TieBreakEvent(GameObject game, List<String> tiedCandidateIds) {
        this.game = game;
        this.tiedCandidateIds = tiedCandidateIds != null ? new ArrayList<>(tiedCandidateIds) : new ArrayList<>();
    }

    @Override
    public boolean checkStartConditions() {
        // Read candidates from the game object at runtime so dynamic ties are captured
        List<String> gameCandidates = game.getTieBreakCandidateIds();
        if (!gameCandidates.isEmpty()) {
            this.tiedCandidateIds = new ArrayList<>(gameCandidates);
        }
        // Skip this event if there are fewer than 2 tied candidates
        if (tiedCandidateIds.size() < 2) {
            return false;
        }
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        // Register the restricted candidate pool so VotingService can filter to it
        game.setTieBreakCandidateIds(tiedCandidateIds);
        game.initSelectionStates(game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(Player::getId)
                .toList());
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
        mc.setBanishVoteEnabled(true);
        mc.setAllChatEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.TIE_BREAK_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "It's a tie! Tiebreaker vote starting.";
    }

    @Override
    public String getInitialMessage() {
        return "It's a tie! Cast your vote again to break the deadlock.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Auto-resolves unsubmitted votes when the tiebreak timer expires.
     * Candidates are restricted to the originally tied players.
     */
    @Override
    public void resolveEvent() {
        List<Player> alivePlayers = game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .toList();

        // Valid banish targets for this tiebreak are the originally tied candidates
        List<Player> tieCandidates = tiedCandidateIds.isEmpty() ? alivePlayers
                : alivePlayers.stream().filter(p -> tiedCandidateIds.contains(p.getId())).toList();

        List<Vote> voteHistory = game.getBanishVotes();

        for (Player player : alivePlayers) {
            UserSelectionsState state = game.getSelectionState(player.getId());
            if (state == null || state.isSubmitPressed()) continue;

            Player target = null;

            // Rule 1: player selected a tied candidate but never pressed submit
            List<String> selected = state.getSelectedItems();
            if (!selected.isEmpty()) {
                Player candidate = game.findPlayerById(selected.get(0));
                if (candidate != null && !candidate.isDead() && tiedCandidateIds.contains(candidate.getId())) {
                    target = candidate;
                }
            }

            // Rule 2: find the most recent prior vote whose target is a tied candidate still alive
            if (target == null) {
                for (int i = voteHistory.size() - 1; i >= 0; i--) {
                    Vote v = voteHistory.get(i);
                    if (v.getCastingPlayer().getId().equals(player.getId())) {
                        Player candidate = v.getReceivingPlayer();
                        if (!candidate.isDead() && tiedCandidateIds.contains(candidate.getId())) {
                            target = candidate;
                            break;
                        }
                    }
                }
            }

            // Rule 3: pick at random from the tied candidates (excluding self if they happen to be one)
            if (target == null) {
                List<Player> fallback = tieCandidates.stream()
                        .filter(p -> !p.getId().equals(player.getId()))
                        .toList();
                if (!fallback.isEmpty()) {
                    target = fallback.get(new Random().nextInt(fallback.size()));
                }
            }

            if (target != null) {
                game.addBanishVote(new Vote(player, target));
            }
        }

        // Clear the restricted candidate pool now that the tiebreak is resolved
        game.clearTieBreakCandidateIds();
    }
}
