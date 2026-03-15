package com.heartless.model.enums;

/**
 * Tracks the overall lifecycle phase of a game.
 * Transitions: INIT → START → END → OVER
 */
public enum GameStatusEnum {
    INIT,
    START,
    END,
    OVER
}
