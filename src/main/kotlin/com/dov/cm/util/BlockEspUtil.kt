package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Utility class for rendering block ESP in Minecraft with Kotlin
 */
object BlockEspUtil {
    private val MC = MinecraftClient.getInstance()

    /**
     * Renders a block ESP at the specified coordinates with the given color
     *
     * @param x X coordinate of the block
     * @param y Y coordinate of the block
     * @param z Z coordinate of the block
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     */
    fun renderBlock(
        x: Int,
        y: Int,
        z: Int,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f
    ) {
        renderBox(Box(
            x.toDouble(),
            y.toDouble(),
            z.toDouble(),
            (x + 1).toDouble(),
            (y + 1).toDouble(),
            (z + 1).toDouble()
        ), color, lineWidth, filled, outlined, fillOpacity)
    }

    /**
     * Renders a block ESP at the specified BlockPos with the given color
     *
     * @param pos BlockPos of the block
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     */
    fun renderBlock(
        pos: BlockPos,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f
    ) {
        renderBlock(pos.x, pos.y, pos.z, color, lineWidth, filled, outlined, fillOpacity)
    }

    /**
     * Renders a block ESP for all instances of the specified block with the given color
     */
    fun renderAllBlocks(
        block: Block,
        color: Color,
        radius: Int = 50,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f
    ) {
        val player = MC.player ?: return
        val playerPos = player.blockPos

        val xRange = (playerPos.x - radius)..(playerPos.x + radius)
        val yRange = (playerPos.y - radius)..(playerPos.y + radius)
        val zRange = (playerPos.z - radius)..(playerPos.z + radius)

        for (x in xRange) {
            for (y in yRange) {
                for (z in zRange) {
                    val pos = BlockPos(x, y, z)
                    if (MC.world?.getBlockState(pos)?.block == block) {
                        renderBlock(pos, color, lineWidth, filled, outlined, fillOpacity)
                    }
                }
            }
        }
    }

    /**
     * Renders a box with the given parameters
     *
     * @param box Box to render
     * @param color RGBA color for the ESP
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     */
    fun renderBox(
        box: Box,
        color: Color,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f
    ) {
        // Comprehensive rendering context checks
        val camera = MC.gameRenderer?.camera
        val world = MC.world
        val player = MC.player

        if (camera == null || world == null || player == null) {
            return  // Exit early if any core rendering context is missing
        }

        // Prevent rendering if too far from the player
        val renderDistance = MC.options.viewDistance.value * 16.0
        val playerPos = player.pos
        val boxCenter = Vec3d(
            (box.minX + box.maxX) / 2.0,
            (box.minY + box.maxY) / 2.0,
            (box.minZ + box.maxZ) / 2.0
        )

        if (playerPos.squaredDistanceTo(boxCenter) > renderDistance * renderDistance) {
            return
        }

        // Additional shader availability check
        if (!RenderSystem.isOnRenderThread()) {
            return
        }

        try {
            // Get render context
            val matrixStack = MatrixStack()

            // Apply camera offset
            val camPos = getCameraPos(camera)
            matrixStack.push()
            matrixStack.translate((-camPos.x).toFloat(), (-camPos.y).toFloat(), (-camPos.z).toFloat())

            // Safe shader and render state management
            RenderSystem.assertOnRenderThread()
            RenderSystem.disableDepthTest()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                770, 771, 1, 0 // Standard alpha blending
            )

            // Draw filled box if requested
            if (filled) {
                setupFilledRendering()

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

                RenderSystem.setShaderColor(r, g, b, a)
                drawFilledBox(box, matrixStack)
                resetFilledRendering()
            }

            // Draw outline if requested
            if (outlined) {
                setupOutlineRendering()

                // Apply color for outline (full opacity)
                val r = color.red / 255f
                val g = color.green / 255f
                val b = color.blue / 255f
                val a = color.alpha / 255f

                RenderSystem.setShaderColor(r, g, b, a)

                // Safe line width setting
                val safeLineWidth = lineWidth.coerceIn(0.1f, 10f)
                GL11.glLineWidth(safeLineWidth)

                drawOutlinedBox(box, matrixStack)
                resetOutlineRendering()
            }

            // Reset rendering state
            matrixStack.pop()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()

        } catch (e: Exception) {
            // Graceful error handling
            println("Error in ESP rendering: ${e.message}")
        }
    }

    /**
     * Setup rendering state for filled boxes
     */
    private fun setupFilledRendering() {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.setShader(ShaderProgramKeys.POSITION)
    }

    /**
     * Reset rendering state after filled boxes
     */
    private fun resetFilledRendering() {
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.disableBlend()
    }

    /**
     * Setup rendering state for outlines
     */
    private fun setupOutlineRendering() {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.setShader(ShaderProgramKeys.POSITION)
    }

    /**
     * Reset rendering state after outlines
     */
    private fun resetOutlineRendering() {
        RenderSystem.enableDepthTest()
        RenderSystem.enableCull()
        RenderSystem.disableBlend()
    }

    /**
     * Draw a filled box
     */
    private fun drawFilledBox(box: Box, matrixStack: MatrixStack) {
        val tessellator = RenderSystem.renderThreadTesselator()
        val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)

        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val matrix = matrixStack.peek().positionMatrix

        // Bottom face
        buffer.vertex(matrix, minX, minY, minZ)
        buffer.vertex(matrix, maxX, minY, minZ)
        buffer.vertex(matrix, maxX, minY, maxZ)
        buffer.vertex(matrix, minX, minY, maxZ)

        // Top face
        buffer.vertex(matrix, minX, maxY, minZ)
        buffer.vertex(matrix, minX, maxY, maxZ)
        buffer.vertex(matrix, maxX, maxY, maxZ)
        buffer.vertex(matrix, maxX, maxY, minZ)

        // Front face
        buffer.vertex(matrix, minX, minY, minZ)
        buffer.vertex(matrix, minX, maxY, minZ)
        buffer.vertex(matrix, maxX, maxY, minZ)
        buffer.vertex(matrix, maxX, minY, minZ)

        // Back face
        buffer.vertex(matrix, minX, minY, maxZ)
        buffer.vertex(matrix, maxX, minY, maxZ)
        buffer.vertex(matrix, maxX, maxY, maxZ)
        buffer.vertex(matrix, minX, maxY, maxZ)

        // Left face
        buffer.vertex(matrix, minX, minY, minZ)
        buffer.vertex(matrix, minX, minY, maxZ)
        buffer.vertex(matrix, minX, maxY, maxZ)
        buffer.vertex(matrix, minX, maxY, minZ)

        // Right face
        buffer.vertex(matrix, maxX, minY, minZ)
        buffer.vertex(matrix, maxX, maxY, minZ)
        buffer.vertex(matrix, maxX, maxY, maxZ)
        buffer.vertex(matrix, maxX, minY, maxZ)

        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    /**
     * Draw an outlined box
     */
    private fun drawOutlinedBox(box: Box, matrixStack: MatrixStack) {
        val tessellator = RenderSystem.renderThreadTesselator()
        val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION)

        val minX = box.minX.toFloat()
        val minY = box.minY.toFloat()
        val minZ = box.minZ.toFloat()
        val maxX = box.maxX.toFloat()
        val maxY = box.maxY.toFloat()
        val maxZ = box.maxZ.toFloat()

        val matrix = matrixStack.peek().positionMatrix

        // Bottom face
        buffer.vertex(matrix, minX, minY, minZ)
        buffer.vertex(matrix, maxX, minY, minZ)

        buffer.vertex(matrix, maxX, minY, minZ)
        buffer.vertex(matrix, maxX, minY, maxZ)

        buffer.vertex(matrix, maxX, minY, maxZ)
        buffer.vertex(matrix, minX, minY, maxZ)

        buffer.vertex(matrix, minX, minY, maxZ)
        buffer.vertex(matrix, minX, minY, minZ)

        // Top face
        buffer.vertex(matrix, minX, maxY, minZ)
        buffer.vertex(matrix, maxX, maxY, minZ)

        buffer.vertex(matrix, maxX, maxY, minZ)
        buffer.vertex(matrix, maxX, maxY, maxZ)

        buffer.vertex(matrix, maxX, maxY, maxZ)
        buffer.vertex(matrix, minX, maxY, maxZ)

        buffer.vertex(matrix, minX, maxY, maxZ)
        buffer.vertex(matrix, minX, maxY, minZ)

        // Connecting lines
        buffer.vertex(matrix, minX, minY, minZ)
        buffer.vertex(matrix, minX, maxY, minZ)

        buffer.vertex(matrix, maxX, minY, minZ)
        buffer.vertex(matrix, maxX, maxY, minZ)

        buffer.vertex(matrix, maxX, minY, maxZ)
        buffer.vertex(matrix, maxX, maxY, maxZ)

        buffer.vertex(matrix, minX, minY, maxZ)
        buffer.vertex(matrix, minX, maxY, maxZ)

        BufferRenderer.drawWithGlobalProgram(buffer.end())
    }

    /**
     * Get camera position for rendering offset
     */
    private fun getCameraPos(camera: Camera): Vec3d {
        return camera.pos
    }
}