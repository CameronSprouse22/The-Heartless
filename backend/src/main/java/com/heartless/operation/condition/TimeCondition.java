package com.heartless.operation.condition;

import com.heartless.model.GameObject;

/**
 * Checks whether a time limit has been exceeded.
 */
public class TimeCondition implements ConditionInterface {

    private final long timeLimitMillis;

    public TimeCondition(long timeLimitMillis) {
        this.timeLimitMillis = timeLimitMillis;
    }

    @Override
    public boolean checkGameConditions(GameObject game) {
        Long startTime = game.getStartGameTime();
        if (startTime == null) {
            return true; // No start time = condition passes
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed < timeLimitMillis;
    }

    public long getTimeLimitMillis() {
        return timeLimitMillis;
    }
}
