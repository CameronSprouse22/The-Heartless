package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A multi-question letter-matching mini game served to every active player
 * before the traitor murder vote.
 *
 * <p>Each player works through the question list <em>independently</em>.
 * Their answers are appended to {@link UserSelectionsState#selectedItems}
 * in order — so {@code selectedItems.size()} equals the number of questions
 * they have answered so far, and {@code submitPressed} becomes {@code true}
 * only once they have answered every question.
 *
 * <p>The event ends when all players have {@code submitPressed == true},
 * or when the time window elapses.
 */
public class MiniGameEvent implements EventObjectInterface {

    /** Simple, JSON-friendly question descriptor. */
    public static class LetterMatchQuestion {
        private final String question;
        private final List<String> options;
        private final String answer;   // correct option — never sent to clients

        public LetterMatchQuestion(String question, List<String> options, String answer) {
            this.question = question;
            this.options  = List.copyOf(options);
            this.answer   = answer;
        }

        public String       getQuestion() { return question; }
        public List<String> getOptions()  { return options;  }
        public String       getAnswer()   { return answer;   }

        /** Safe map representation — answer is intentionally excluded. */
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("question", question);
            m.put("options",  options);
            return m;
        }
    }

    // ── Default question set ─────────────────────────────────────────────────
    private static final List<LetterMatchQuestion> DEFAULT_QUESTIONS = List.of(
            new LetterMatchQuestion(
                    "Which letter comes after D in the alphabet?",
                    List.of("A", "B", "C", "E", "F"), "E"),
            new LetterMatchQuestion(
                    "Which letter comes before J in the alphabet?",
                    List.of("G", "H", "I", "K", "L"), "I"),
            new LetterMatchQuestion(
                    "Which is the 1st vowel in the English alphabet?",
                    List.of("A", "B", "C", "D", "E"), "A")
    );

    private final GameObject game;
    private final List<LetterMatchQuestion> questions;
    private Long startTime = null;
    /** When set by a parent event, overrides the computed end time so the frontend countdown matches. */
    private Long overrideEndTime = null;

    public void setOverrideEndTime(Long endTime) {
        this.overrideEndTime = endTime;
    }

    public MiniGameEvent(GameObject game) {
        this(game, DEFAULT_QUESTIONS);
    }

    public MiniGameEvent(GameObject game, List<LetterMatchQuestion> questions) {
        this.game      = game;
        this.questions = List.copyOf(questions);
    }

    public List<LetterMatchQuestion> getQuestions() { return questions; }

    // ── EventObjectInterface ─────────────────────────────────────────────────

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        List<String> activePlayers = game.getPlayerList().stream()
                .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(Player::getId)
                .toList();
        game.initSelectionStates(activePlayers);
        return true;
    }

    @Override
    public void onStart() { /* startTime set in checkStartConditions */ }

    /**
     * Ends when every active player has answered all questions
     * ({@code submitPressed == true}).
     */
    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        Map<String, UserSelectionsState> stateMap = gameObject.getSelectionStateMap();
        if (stateMap == null || stateMap.isEmpty()) return true;
        return stateMap.values().stream().allMatch(UserSelectionsState::isSubmitPressed);
    }

    @Override
    public void resolveEvent() { /* answers stored per-player in selection states */ }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setMiniGameEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.MINI_GAME_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (overrideEndTime != null) return overrideEndTime;
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() { return "A mini game is starting!"; }

    @Override
    public String getInitialMessage() { return "A mini game is about to begin. Get ready!"; }

    @Override
    public boolean checkForNotifications() { return true; }

    // ── Answer submission (called by GameController) ─────────────────────────

    /**
     * Records a player's answer for their <em>current</em> question (derived
     * from how many answers they have already stored).
     *
     * @return a result map with {@code accepted}, {@code correct}, {@code done},
     *         {@code questionIndex} (0-based index just answered), and
     *         {@code nextQuestionIndex} — or {@code null} if the player has no
     *         active slot or has already finished all questions.
     */
    public Map<String, Object> submitAnswer(String playerId, String selectedOption) {
        UserSelectionsState state = game.getSelectionState(playerId);
        if (state == null || state.isSubmitPressed()) return null;

        List<String> current = state.getSelectedItems();      // defensive copy
        int idx = current.size();
        if (idx >= questions.size()) return null;             // already answered all

        // Append this answer
        current.add(selectedOption);
        state.setSelectedItems(current);

        boolean correct = selectedOption.equals(questions.get(idx).getAnswer());
        boolean done    = current.size() >= questions.size();
        if (done) {
            state.setSubmitPressed(true);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted",          true);
        result.put("correct",           correct);
        result.put("questionIndex",     idx);
        result.put("done",              done);
        result.put("nextQuestionIndex", done ? -1 : idx + 1);
        return result;
    }

    /**
     * Builds the status payload returned by the GET endpoint.
     * The current question is derived from how many answers the player has
     * already stored — no extra server-side index is needed.
     */
    public Map<String, Object> buildStatusMap(String playerId) {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalQuestions", questions.size());

        UserSelectionsState myState = game.getSelectionState(playerId);
        List<String> myAnswers  = myState != null ? myState.getSelectedItems() : List.of();
        boolean      myDone     = myState != null && myState.isSubmitPressed();
        int          currentIdx = myAnswers.size(); // next question to answer

        result.put("myAnswers",           myAnswers);
        result.put("myDone",              myDone);
        result.put("currentQuestionIndex", currentIdx);

        // Current question (null when player has finished)
        if (!myDone && currentIdx < questions.size()) {
            result.put("question", questions.get(currentIdx).toMap());
        } else {
            result.put("question", null);
        }

        // Aggregate: how many players have finished all questions
        Map<String, UserSelectionsState> stateMap = game.getSelectionStateMap();
        int required  = stateMap == null ? 0 : stateMap.size();
        long finished = stateMap == null ? 0L
                : stateMap.values().stream().filter(UserSelectionsState::isSubmitPressed).count();
        result.put("completedCount", (int) finished);
        result.put("requiredCount",  required);

        result.put("eventEndTime", getEventEndTime());

        return result;
    }
}
