/*
when sex mod for prestige clarinet? - cloovey
*/
package com.logicalgeekboy.logical_zoom.java_manaagers;

import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_event.Priority;
import com.logicalgeekboy.logical_zoom.java_event.impl.Render2DEvent;
import com.logicalgeekboy.logical_zoom.skid;



public class RenderManager {
    public long ms;
    public long lastMs;

    public RenderManager() {
        skid.Companion.getEventBus().registerListener(this);
    }

    @EventListener(getPriority= Priority.HIGHEST)
    public void event(Render2DEvent event) {
        ms = System.currentTimeMillis() - lastMs;
        lastMs = System.currentTimeMillis();
        if (ms > 30) {
            ms = 0L;
        }
    }

    public float getMs() {
        return (float)this.ms * 0.005f;
    }
}
