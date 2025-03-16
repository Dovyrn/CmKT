package com.logicalgeekboy.logical_zoom.java_event.impl;


import com.logicalgeekboy.logical_zoom.java_event.Event;
import net.minecraft.entity.Entity;

public class RenderHitboxEvent extends Event {
    public Entity entity;

    public RenderHitboxEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }
}
