package com.heartless.gamethread;

/**
 * Records the outcome of a game event (mini-game, challenge, etc.) in a round.
 */
public class GameEventResult {

    private String prompt = "";
    private String answer = "";
    private String result = "";
    private GameRoundObject.RoundStatus status = GameRoundObject.RoundStatus.NOTSTARTED;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt != null ? prompt : ""; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer != null ? answer : ""; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result != null ? result : ""; }

    public GameRoundObject.RoundStatus getStatus() { return status; }
    public void setStatus(GameRoundObject.RoundStatus status) { this.status = status; }
}
