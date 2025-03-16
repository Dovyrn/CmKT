package com.logicalgeekboy.logical_zoom.java_util.impl;



public class TimerUtil {
    public long lastMs = System.currentTimeMillis();

    public boolean delay(float f) {
        return (float)(System.currentTimeMillis() - lastMs) > f;
    }



    public float getElapsedTime() {
        return System.currentTimeMillis() - this.lastMs;
    }

    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }
}
