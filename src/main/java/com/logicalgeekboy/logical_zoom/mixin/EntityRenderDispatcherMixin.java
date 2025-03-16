package com.logicalgeekboy.logical_zoom.mixin;

import com.logicalgeekboy.logical_zoom.java_event.impl.RenderHitboxEvent;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(
            method = "renderHitbox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;FFFF)V",
            at = @At("RETURN")
    )
    private static void renderHitboxReturn(MatrixStack matrices, VertexConsumer vertices, Entity entity,
                                           float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        new RenderHitboxEvent(null).invoke();
    }

    @Inject(
            method = "renderHitbox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;FFFF)V",
            at = @At("HEAD")
    )
    private static void renderHitbox(MatrixStack matrices, VertexConsumer vertices, Entity entity,
                                     float tickDelta, float red, float green, float blue, CallbackInfo ci) {
        new RenderHitboxEvent(entity).invoke();
    }
}