package com.logicalgeekboy.logical_zoom.java_manaagers;

import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_event.Priority;
import com.logicalgeekboy.logical_zoom.java_event.impl.Render2DEvent;
import com.logicalgeekboy.logical_zoom.skid;

public class RenderManager {
    public long ms;
    public long lastMs;
    private boolean DEBUG = true;



    public RenderManager() {
        skid.Companion.getEventBus().registerListener(this);
    }

    @EventListener(getPriority= Priority.HIGHEST)
    public void event(Render2DEvent event) {
        long currentTime = System.currentTimeMillis();
        ms = currentTime - lastMs;
        lastMs = currentTime;

        // Cap the MS value to avoid extreme values
        if (ms > 30) {
            ms = 0L;
        } else {
        }
    }

    public float getMs() {
        float result = (float)this.ms * 0.005f;
        return result;
    }
}