package com.logicalgeekboy.logical_zoom.java_manaagers;

import com.logicalgeekboy.logical_zoom.interfaces.IRotatable;
import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_event.impl.JumpEvent;
import com.logicalgeekboy.logical_zoom.java_event.impl.MoveEvent;
import com.logicalgeekboy.logical_zoom.java_event.impl.TickEvent;
import com.logicalgeekboy.logical_zoom.java_util.impl.Rotation;
import com.logicalgeekboy.logical_zoom.skid;


import java.util.ArrayList;

public class RotationManager {
    public Rotation rotation;
    public ArrayList<IRotatable> rotations = new ArrayList();

    public RotationManager() {
        skid.Companion.getEventBus().registerListener(this);
    }


    @EventListener
    public void event(TickEvent event) {
        this.rotation = null;
        for (IRotatable iRotatable : this.rotations) {
            Rotation rotation = iRotatable.getRotation();
            if (rotation != null) {
                this.rotation = rotation;
            }
        }
    }

    @EventListener
    public void event(MoveEvent event) {
        if (this.rotation != null) {
            event.setYaw(this.rotation.getYaw());
            event.setPitch(this.rotation.getPitch());
        }
        event.setCancelled();
    }

    @EventListener
    public void event(JumpEvent event) {
        if (this.rotation != null) {
            Rotation rotation = this.rotation;
            event.setYaw(rotation.getYaw());
        }
        event.setCancelled();
    }

    public void addRotation(IRotatable iRotatable) {
        this.rotations.add(iRotatable);
    }

    public void removeRotation(IRotatable iRotatable) {
        this.rotations.remove(iRotatable);
    }
}
