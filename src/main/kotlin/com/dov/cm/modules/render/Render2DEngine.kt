package com.dov.cm.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import java.awt.Color

class Render2DEngine {
    companion object {

        fun drawRectWithOutline(
            matrices: MatrixStack,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            fillColor: Color,
            outlineColor: Color
        ) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            // Draw filled rectangle - Corrected vertex order
            val fillTessellator = Tessellator.getInstance()
            val fillBuffer = fillTessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
            val matrix = matrices.peek().positionMatrix

            // Proper counter-clockwise order for quads
            fillBuffer.vertex(matrix, x, y, 0f).color(fillColor.rgb)
            fillBuffer.vertex(matrix, x, y + height, 0f).color(fillColor.rgb)
            fillBuffer.vertex(matrix, x + width, y + height, 0f).color(fillColor.rgb)
            fillBuffer.vertex(matrix, x + width, y, 0f).color(fillColor.rgb)

            BufferRenderer.drawWithGlobalProgram(fillBuffer.end())

            // Draw outline - Corrected loop order
            RenderSystem.lineWidth(1.0f) // Set line width explicitly

            val outlineTessellator = Tessellator.getInstance()
            val outlineBuffer = outlineTessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR)

            outlineBuffer.vertex(matrix, x, y, 0f).color(outlineColor.rgb)
            outlineBuffer.vertex(matrix, x, y + height, 0f).color(outlineColor.rgb)
            outlineBuffer.vertex(matrix, x + width, y + height, 0f).color(outlineColor.rgb)
            outlineBuffer.vertex(matrix, x + width, y, 0f).color(outlineColor.rgb)
            outlineBuffer.vertex(matrix, x, y, 0f).color(outlineColor.rgb) // Close the loop

            BufferRenderer.drawWithGlobalProgram(outlineBuffer.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        fun rainbow(
            speed: Int,
            index: Int,
            saturation: Float = 0.4f,
            brightness: Float = 1f
        ): Color {
            val hue = ((System.currentTimeMillis() + index * speed) % 3000) / 3000f
            return Color.getHSBColor(hue, saturation, brightness)
        }
    }
}