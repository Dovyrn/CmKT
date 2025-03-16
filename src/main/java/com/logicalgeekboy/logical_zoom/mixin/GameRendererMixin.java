package com.logicalgeekboy.logical_zoom.mixin;

import com.dov.cm.CmKtClient;

import com.logicalgeekboy.logical_zoom.java_event.EventBus;
import com.logicalgeekboy.logical_zoom.java_event.impl.Render2DEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Only dispatch event if a world is loaded and we're not in a screen
        if (client.world != null) {
            MatrixStack matrixStack = new MatrixStack();
            Render2DEvent event = new Render2DEvent(
                    matrixStack,
                    client.getWindow().getScaledWidth(),
                    client.getWindow().getScaledHeight()
            );

            // Get the shared EventBus from CmKtClient
            EventBus eventBus = CmKtClient.INSTANCE.getEventBus();
            if (eventBus != null) {
                System.out.println("[Render] Dispatching Render2DEvent");
                eventBus.invoke(event);
            } else {
                System.out.println("[Render] EventBus is null");
            }
        }
    }
}