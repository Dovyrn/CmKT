package com.logicalgeekboy.logical_zoom.java_event.impl;


import com.logicalgeekboy.logical_zoom.java_event.Event;

public class JumpEvent extends Event {
    public float yaw;

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
