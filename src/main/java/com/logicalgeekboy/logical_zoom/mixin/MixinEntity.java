package com.logicalgeekboy.logical_zoom.mixin;

import com.logicalgeekboy.logical_zoom.java_event.impl.EntityMarginEvent;
import com.logicalgeekboy.logical_zoom.java_event.impl.HitboxEvent;
import com.logicalgeekboy.logical_zoom.java_event.impl.JumpEvent;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={Entity.class})
public abstract class MixinEntity {
    @Shadow
    public abstract World getEntityWorld();



    @Inject(method={"getBoundingBox"}, at={@At(value="RETURN")}, cancellable=true)
    void getBoundingBox(CallbackInfoReturnable callbackInfoReturnable) {
        if (getEntityWorld() == null) {
            return;
        }
        Entity entity = getEntityWorld().getEntityById(getId());
        if (entity == null) {
            return;
        }
        Box box = (Box)callbackInfoReturnable.getReturnValue();
        HitboxEvent event = new HitboxEvent(entity, box);
        if (event.invoke()) {
            callbackInfoReturnable.setReturnValue(event.getBox());
        }
    }

    @Shadow
    public abstract int getId();


}