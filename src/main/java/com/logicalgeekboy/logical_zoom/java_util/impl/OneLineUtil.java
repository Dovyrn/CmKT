package com.logicalgeekboy.logical_zoom.java_util.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public class OneLineUtil {

    //XDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD
    public static boolean isInvalidPlayer() {
        return MinecraftClient.getInstance().world.getPlayers().stream().filter(player -> player.equals(MinecraftClient.getInstance().player)).filter(player -> Math.sqrt(player.squaredDistanceTo(MinecraftClient.getInstance().player)) < 5).anyMatch(LivingEntity::isDead);
    }
}
