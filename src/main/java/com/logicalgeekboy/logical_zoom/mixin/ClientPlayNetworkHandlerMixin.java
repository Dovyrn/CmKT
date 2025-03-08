package com.logicalgeekboy.logical_zoom.mixin;

import com.dov.cm.modules.combat.Backtrack;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    /**
     * Intercept entity position packets for backtracking
     */
    @Inject(method = "onEntityPosition", at = @At("HEAD"), cancellable = true)
    private void onEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
        // Let the Backtrack module handle the packet
        // We don't need to cancel the packet here - the Backtrack module
        // will modify the entity's position directly
    }

    /**
     * Intercept entity relative move packets for backtracking
     */
    @Inject(method = "onEntity", at = @At("HEAD"), cancellable = true)
    private void onEntity(EntityS2CPacket packet, CallbackInfo ci) {
        // These are relative move packets, also handled by Backtrack
        // But again, we don't need to cancel - just let the module update positions directly
    }

    /**
     * Detect when player gets damaged to handle disable-on-hit
     */
    @Inject(method = "onGameStateChange", at = @At("RETURN"))
    private void onGameStateChange(CallbackInfo ci) {
        // Notify the Backtrack module when the player takes damage
        if (net.minecraft.client.MinecraftClient.getInstance().player != null &&
                net.minecraft.client.MinecraftClient.getInstance().player.hurtTime > 0) {
            Backtrack.INSTANCE.onPlayerDamaged();
        }
    }
}