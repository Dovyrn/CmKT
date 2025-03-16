package com.logicalgeekboy.logical_zoom.java_modules;


import com.dov.cm.config.Config;
import com.logicalgeekboy.logical_zoom.java_event.impl.HitboxEvent;
import com.logicalgeekboy.logical_zoom.java_event.impl.RenderHitboxEvent;
import com.logicalgeekboy.logical_zoom.skid;
import com.logicalgeekboy.logical_zoom.java_event.impl.EntityMarginEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import com.logicalgeekboy.logical_zoom.java_event.EventListener;
import com.logicalgeekboy.logical_zoom.java_util.MC;

public class Hitboxes  {

    private Config config = Config.INSTANCE;

    public float expand;
    public int targets;
    public boolean render;
    public float distance;
    public Entity entity;

    public Hitboxes() {

    }
    private MinecraftClient getMc(){
        return MinecraftClient.getInstance();
    }

    @EventListener
    public void event(EntityMarginEvent event) {
        expand = config.getHitboxExpand();
        targets = config.getHitboxTargets();
        render = false;
        distance = 20;
        if (event.equals(getMc().player) || !(event.getEntity() instanceof EndCrystalEntity) && !(event.getEntity() instanceof PlayerEntity)) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity && (!(this.targets == 0) || !skid.Companion.getAntiBotManager().isNotBot(event.getEntity())) || event.getEntity() instanceof EndCrystalEntity && !(this.targets == 1)) {
            return;
        }
        event.setMargin(event.getMargin() + this.expand / 10);
        event.setCancelled();
    }

    @EventListener
    public void event(HitboxEvent event) {
        if (entity == event.getEntity() && render && entity != null && entity.distanceTo(getMc().player) > distance) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity && (!(targets == 0) || !skid.Companion.getAntiBotManager().isNotBot(event.getEntity())) || event.getEntity() instanceof EndCrystalEntity && !(targets == 1)) {
            return;
        }
        event.setBox(event.getBox().expand(expand / 10));
        event.setCancelled();
    }

    @EventListener
    public void event(RenderHitboxEvent event) {
        entity = event.getEntity();
    }
}