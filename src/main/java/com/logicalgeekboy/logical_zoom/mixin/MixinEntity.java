package com.logicalgeekboy.logical_zoom.mixin;

import com.logicalgeekboy.logical_zoom.java_event.impl.HitboxEvent;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        // Cast this to Entity - more efficient than querying by ID
        Entity self = (Entity) (Object) this;
        Box originalBox = cir.getReturnValue();

        // Create and dispatch the event
        HitboxEvent event = new HitboxEvent(self, originalBox);
        if (event.invoke()) {
            cir.setReturnValue(event.getBox());
        }
    }
}