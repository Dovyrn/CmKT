package com.logicalgeekboy.logical_zoom.java_event.impl;

import com.logicalgeekboy.logical_zoom.java_event.Event;
import net.minecraft.entity.Entity;

public class EntityMarginEvent extends Event {
    public Entity entity;
    public float margin;

    public EntityMarginEvent(Entity entity, float f) {
        this.entity = entity;
        this.margin = f;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public float getMargin() {
        return this.margin;
    }

    public void setMargin(float margin) {
        this.margin = margin;
    }
}
