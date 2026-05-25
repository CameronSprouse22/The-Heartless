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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MurderVoteEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;
    private EventObjectInterface coverEvent = null;
    private EventObjectInterface faithfulOnlyEvent = null;
    private volatile boolean miniGameDone = false;
    private final Set<String> miniGameCompletedPlayerIds = ConcurrentHashMap.newKeySet();
    private volatile boolean murderApplied = false;
    private VotingService votingService;
    private String gameCode;

    public MurderVoteEvent(GameObject game) {
        this.game = game;
    }

    /** Attach an event all players do before traitors get the murder vote. */
    public void setCoverEvent(EventObjectInterface coverEvent) {
        this.coverEvent = coverEvent;
    }

    /** Attach an event shown only to faithful players (runs in parallel with traitor murder vote). */
    public void setFaithfulOnlyEvent(EventObjectInterface faithfulOnlyEvent) {
        this.faithfulOnlyEvent = faithfulOnlyEvent;
    }

    public EventObjectInterface getCoverEvent()  { return coverEvent; }
    public boolean isMiniGameDone()              { return miniGameDone; }
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
        if (coverEvent != null) {
            // Share the murder-vote timer with the cover event
            coverEvent.setOverrideEndTime(getEventEndTime());
            coverEvent.checkStartConditions();
            coverEvent.onStart(); // inits selection states for ALL active players
            // No pre-promotions — everyone starts the cover event
        } else {
            // No cover event — start faithfulOnlyEvent (if any) for faithful;
            // traitors skip straight to murder vote
            if (faithfulOnlyEvent != null) {
                faithfulOnlyEvent.setOverrideEndTime(getEventEndTime());
                faithfulOnlyEvent.checkStartConditions();
                faithfulOnlyEvent.onStart(); // inits selection states for ALL active players
            } else {
                game.initSelectionStates(
                        game.getPlayerList().stream()
                                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                                .map(Player::getId)
                                .toList());
            }
            // Fast-track traitors past the faithful phase straight to murder vote
            game.getPlayerList().stream()
                    .filter(p -> !p.isDead()
                            && p.getStatus() == PlayerStatusEnum.ACTIVE
                            && p.isTraitor())
                    .map(Player::getId)
                    .forEach(miniGameCompletedPlayerIds::add);
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

        // Promote any player who just finished the cover event to their next phase:
        //   traitors -> murder vote, faithful -> faithfulOnlyEvent (or done if none)
        if (coverEvent != null) {
            active.stream()
                    .filter(p -> !miniGameCompletedPlayerIds.contains(p.getId()))
                    .forEach(p -> {
                        UserSelectionsState sel = game.getSelectionState(p.getId());
                        if (sel != null && sel.isSubmitPressed()) {
                            miniGameCompletedPlayerIds.add(p.getId());
                            game.resetSelectionState(p.getId()); // fresh state for next phase
                        }
                    });
        }

        // All faithful must have completed their task.
        boolean allFaithfulDone = active.stream()
                .filter(p -> !p.isTraitor())
                .allMatch(p -> {
                    if (faithfulOnlyEvent != null) {
                        // Faithful must complete the faithfulOnly task (submitPressed after promotion)
                        UserSelectionsState sel = game.getSelectionState(p.getId());
                        return sel != null && sel.isSubmitPressed();
                    } else if (coverEvent != null) {
                        // No faithfulOnly — faithful are done once they finish the cover event
                        return miniGameCompletedPlayerIds.contains(p.getId());
                    } else {
                        // No tasks for faithful — they are immediately done
                        return true;
                    }
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
     * - While in mini-game/scuttlebutt phase: players see their respective event
     * - Traitors promoted past that phase: see murder vote + traitor chat
     */
    @Override
    public GameState getGameState(Player player) {
        if (!miniGameCompletedPlayerIds.contains(player.getId())) {
            // Player has not yet been promoted past the cover phase
            if (coverEvent != null) {
                // Everyone does the cover event first
                return coverEvent.getGameState(player);
            } else if (!player.isTraitor() && faithfulOnlyEvent != null) {
                // No cover event — faithful go straight to their dedicated event
                return faithfulOnlyEvent.getGameState(player);
            }
            // Traitors without a cover event are pre-added to miniGameCompletedPlayerIds at onStart()
        } else {
            // Player has been promoted past the cover phase
            if (player.isTraitor()) {
                MenuControl mc = new MenuControl();
                mc.setMurderVoteEnabled(true);
                mc.setTraitorChatEnabled(true);
                return GameState.fromEvent(mc, game, this);
            } else if (faithfulOnlyEvent != null) {
                // Faithful finished cover event — hand off to the faithful-only event
                return faithfulOnlyEvent.getGameState(player);
            }
        }
        return getGameState();
    }

    /** Role-agnostic fallback (used by the interface default when no player is available). */
    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setMurderVoteEnabled(true);
        mc.setTraitorChatEnabled(true);
        mc.setCurrentPage("murder-vote");
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
        return coverEvent != null ? coverEvent.getStartNotification() : "The murder phase has begun.";
    }

    @Override
    public String getInitialMessage() {
        return coverEvent != null ? coverEvent.getInitialMessage()
                : "The night falls. Traitors — select your target carefully.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
