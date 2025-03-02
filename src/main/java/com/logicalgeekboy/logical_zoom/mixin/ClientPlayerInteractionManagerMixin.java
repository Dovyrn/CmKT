package com.logicalgeekboy.logical_zoom.mixin;

import com.dov.cm.modules.WeaponSwapper;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        // Check if WeaponSwapper wants to handle this attack
        if (WeaponSwapper.INSTANCE.shouldCancelAttack(target)) {
            // Cancel the attack packet
            ci.cancel();
        }
    }
}