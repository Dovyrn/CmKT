package com.logicalgeekboy.logical_zoom.java_util.impl;

import com.logicalgeekboy.logical_zoom.java_util.MC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;

public class PacketUtil implements MC {

    public static PacketUtil INSTANCE = new PacketUtil();

    public void sendPacket(Packet<?> packet) {
        if (this.getMc().getNetworkHandler() != null) {
            this.getMc().getNetworkHandler().sendPacket(packet);
        }
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }
}