package com.logicalgeekboy.logical_zoom.java_event;


import com.logicalgeekboy.logical_zoom.skid;

public class Event {
    public Phase phase;
    public boolean cancelled;

    public Event(Phase phase) {
        this.phase = phase;
    }

    public Event() {
        this(Phase.NONE);
    }

    public boolean invoke() {
        return skid.Companion.getEventBus().invoke(this);
    }

    public Phase getPhase() {
        return this.phase;
    }

    public void setCancelled() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}
