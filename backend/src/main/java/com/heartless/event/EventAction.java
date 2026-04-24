package com.heartless.event;

/**
 * A scheduled action produced by a reveal event.
 * The frontend polls for actions whose executeTime has passed to drip-feed results.
 */
public class EventAction {

    private final Object actionObject;
    private final Long executeTime; // epoch ms when to reveal; null = not scheduled

    public EventAction(Object actionObject, Long executeTime) {
        this.actionObject = actionObject;
        this.executeTime = executeTime;
    }

    public Object getActionObject() { return actionObject; }
    public Long getExecuteTime() { return executeTime; }
}
