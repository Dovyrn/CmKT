package com.dov.cm.event

import com.dov.cm.event.Event
import net.minecraft.client.util.math.MatrixStack

class Render2DEvent(
    val matrixStack: MatrixStack,
    val scaledWidth: Int,
    val scaledHeight: Int
) : Event()