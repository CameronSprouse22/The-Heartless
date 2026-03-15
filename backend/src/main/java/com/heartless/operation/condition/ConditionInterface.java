package com.heartless.operation.condition;

import com.heartless.model.GameObject;

/**
 * Contract for evaluating game conditions.
 */
public interface ConditionInterface {

    boolean checkGameConditions(GameObject game);
}
