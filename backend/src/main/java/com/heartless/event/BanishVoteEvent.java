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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BanishVoteEvent implements EventObjectInterface {

    private static final Logger log = LoggerFactory.getLogger(BanishVoteEvent.class);

    private final GameObject game;
    private Long startTime = null;

    public BanishVoteEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        List<String> activePlayerIds = gameObject.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(Player::getId)
                .toList();

        List<String> votedPlayerIds = gameObject.getBanishVotes().stream()
                .map(v -> v.getCastingPlayer().getId())
                .toList();

        log.debug("BanishVoteEvent.endConditonsMeet — activePlayers={} ({}) votes={} ({})",
                activePlayerIds.size(), activePlayerIds,
                votedPlayerIds.size(), votedPlayerIds);

        // Require at least one active player and at least one vote — never complete with zero votes
        if (activePlayerIds.isEmpty()) {
            log.warn("BanishVoteEvent.endConditonsMeet — activePlayerIds EMPTY, returning false (no active players to vote)");
            return false;
        }
        if (votedPlayerIds.isEmpty()) {
            return false;
        }
        boolean allVoted = votedPlayerIds.containsAll(activePlayerIds);
        if (allVoted) {
            log.info("BanishVoteEvent.endConditonsMeet — all {} active players have voted", activePlayerIds.size());
        }
        return allVoted;
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void onStart() {
        game.initSelectionStates(game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(p -> p.getId())
                .toList());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setBanishVoteEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.BANISH_VOTE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Voting has begun!";
    }

    @Override
    public String getInitialMessage() {
        return "It's time to vote. Choose wisely — one player will be banished.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Resolves unsubmitted votes when the banish vote timer expires.
     * <ol>
     *   <li>If a player selected someone but did not submit, their vote resolves to that player.</li>
     *   <li>If no current selection, the game resolves to the last alive player they previously voted for.</li>
     *   <li>If they have never voted for a currently-alive player, a random alive player is chosen.</li>
     * </ol>
     */
    @Override
    public void resolveEvent() {
        List<Player> alivePlayers = game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .toList();

        List<Vote> voteHistory = game.getBanishVotes();

        for (Player player : alivePlayers) {
            UserSelectionsState state = game.getSelectionState(player.getId());
            if (state == null || state.isSubmitPressed()) continue;

            Player target = null;

            // Rule 1: player selected someone but never pressed submit
            List<String> selected = state.getSelectedItems();
            if (!selected.isEmpty()) {
                Player candidate = game.findPlayerById(selected.get(0));
                if (candidate != null && !candidate.isDead()) {
                    target = candidate;
                }
            }

            // Rule 2: no current selection — find the most recent prior vote whose target is still alive
            if (target == null) {
                for (int i = voteHistory.size() - 1; i >= 0; i--) {
                    Vote v = voteHistory.get(i);
                    if (v.getCastingPlayer().getId().equals(player.getId())) {
                        Player candidate = v.getReceivingPlayer();
                        if (!candidate.isDead()) {
                            target = candidate;
                            break;
                        }
                    }
                }
            }

            // Rule 3: never voted for any currently-alive player — pick at random
            if (target == null) {
                List<Player> candidates = alivePlayers.stream()
                        .filter(p -> !p.getId().equals(player.getId()))
                        .toList();
                if (!candidates.isEmpty()) {
                    target = candidates.get(new Random().nextInt(candidates.size()));
                }
            }

            if (target != null) {
                game.addBanishVote(new Vote(player, target));
            }
        }
    }
}
