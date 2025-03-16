package com.logicalgeekboy.logical_zoom.java_event.impl;

import com.logicalgeekboy.logical_zoom.java_event.Event;
import net.minecraft.client.util.math.MatrixStack;

public class Render2DEvent extends Event {
    public MatrixStack matrixStack;
    public int scaledWidth;
    public int scaledHeight;


    public Render2DEvent(MatrixStack matrixStack, int scaledWidth, int scaledHeight) {
        this.matrixStack = matrixStack;
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;


    }

    @Override
    public boolean invoke() {
        boolean result = super.invoke();
        return result;
    }

    public MatrixStack getMatrixStack() {
        return this.matrixStack;
    }

    public int getScaledWidth() {
        return this.scaledWidth;
    }

    public int getScaledHeight() {
        return this.scaledHeight;
    }
}