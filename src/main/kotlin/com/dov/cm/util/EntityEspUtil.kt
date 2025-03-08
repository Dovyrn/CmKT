package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Utility class for rendering entity ESP in Minecraft with Kotlin
 * Updated for 1.21.4 rendering pipeline with proper transparency
 */
object EntityEspUtil {
    private val MC = MinecraftClient.getInstance()

    /**
     * Renders ESP around an entity with the given color
     */
    fun renderEntity(
        entity: Entity,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f,
        expand: Float = 0f
    ) {
        try {
            // Get entity's bounding box and expand if needed
            val box = entity.boundingBox.expand(expand.toDouble())

            // Render the box
            renderBoxSafe(box, color, lineWidth, filled, outlined, fillOpacity)
        } catch (e: Exception) {
            // Silently handle any rendering errors to prevent crashes
        }
    }

    /**
     * Renders a box with the given parameters using safer methods for 1.21.4
     */
    fun renderBoxSafe(
        box: Box,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f
    ) {
        // Get render context
        val matrixStack = MatrixStack()
        val camera = MC.gameRenderer.camera ?: return

        // Apply camera offset
        val camPos = getCameraPos(camera)
        matrixStack.push()
        matrixStack.translate((-camPos.x).toFloat(), (-camPos.y).toFloat(), (-camPos.z).toFloat())

        // Make sure we're on the render thread
        if (!RenderSystem.isOnRenderThread()) {
            matrixStack.pop()
            return
        }

        try {
            // Set up render state for 1.21.4
            RenderSystem.enableBlend()
            RenderSystem.disableCull()

            // Key fix for 1.21.4 - use overlay blend function for proper transparency
            //RenderSystem.overlayBlendFunc()


            // Only disable depth test temporarily
            val depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
            if (depthEnabled) {
                RenderSystem.disableDepthTest()
            }

            // In 1.21.4, we use ShaderProgramKeys for the position shader
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

            // Draw filled box if requested
            if (filled) {
                try {
                    // Apply color with custom opacity for fill
                    val fillColor = Color(
                        color.red,
                        color.green,
                        color.blue,
                        (fillOpacity * 255).toInt().coerceIn(0, 255)
                    )

                    val r = fillColor.red / 255f
                    val g = fillColor.green / 255f
                    val b = fillColor.blue / 255f
                    val a = fillColor.alpha / 255f

                    // Use the safer tessellator approach
                    val tessellator = RenderSystem.renderThreadTesselator()
                    val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

                    // Draw each face of the box with color
                    drawBoxFacesWithColor(box, matrixStack, buffer, r, g, b, a)

                    // End and draw
                    BufferRenderer.drawWithGlobalProgram(buffer.end())
                } catch (e: Exception) {
                    // Handle silently
                }
            }

            // Draw outline if requested
            if (outlined) {
                try {
                    // Apply color for outline (full opacity)
                    val r = color.red / 255f
                    val g = color.green / 255f
                    val b = color.blue / 255f
                    val a = color.alpha / 255f

                    // Safe line width setting
                    RenderSystem.lineWidth(lineWidth.coerceIn(1f, 3f))

                    // Draw box outline with color
                    val tessellator = RenderSystem.renderThreadTesselator()
                    val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

                    drawBoxOutlineWithColor(box, matrixStack, buffer, r, g, b, a)

                    // End and draw
                    BufferRenderer.drawWithGlobalProgram(buffer.end())
                } catch (e: Exception) {
                    // Handle silently
                }
            }

            // Reset rendering state
            if (depthEnabled) {
                RenderSystem.enableDepthTest()
            }
            RenderSystem.enableCull()
            RenderSystem.defaultBlendFunc() // Reset blend function
            RenderSystem.disableBlend()
        } catch (e: Exception) {
            // Catch any unexpected errors
        } finally {
            // Always restore matrix state
            matrixStack.pop()
        }
    }

    /**
     * Draw box faces with color for filled rendering
     */
    private fun drawBoxFacesWithColor(
        box: Box,
        matrixStack: MatrixStack,
        buffer: VertexConsumer,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val matrix = matrixStack.peek().positionMatrix

        // Bottom face
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);

// Top face
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);

// Front face
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);

// Back face
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);

// Left face
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);

// Right face
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);

    }

    /**
     * Draw box outline edges with color
     */
    private fun drawBoxOutlineWithColor(
        box: Box,
        matrixStack: MatrixStack,
        buffer: VertexConsumer,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val matrix = matrixStack.peek().positionMatrix

        // Bottom rectangle
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)

        // Top rectangle
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)

        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)

        // Connecting pillars
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)

        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)

        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)
    }

    /**
     * Get camera position for rendering offset
     */
    private fun getCameraPos(camera: Camera): Vec3d {
        return camera.pos
    }
}