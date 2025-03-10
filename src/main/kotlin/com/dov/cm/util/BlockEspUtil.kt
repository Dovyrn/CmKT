package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color


/**
 * Utility class for rendering block ESP in Minecraft with Kotlin
 * Fixed to prevent OpenGL 1281 errors in 1.21.4
 */
object BlockEspUtil {
    private val MC = MinecraftClient.getInstance()


    /**
     * Renders a block ESP at the specified coordinates with the given color
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
     */
    fun renderBox(
        box: Box,
        color: Color,
        lineWidth: Float,
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

        // Make sure we're on the render thread
        if (!RenderSystem.isOnRenderThread()) {
            return
        }

        // Get render context
        val matrixStack = MatrixStack()

        try {
            // Apply camera offset
            val camPos = getCameraPos(camera)
            matrixStack.push()
            matrixStack.translate((-camPos.x).toFloat(), (-camPos.y).toFloat(), (-camPos.z).toFloat())

            // Set up render state for transparency
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc() // Key fix for proper transparency in 1.21.4
            RenderSystem.disableCull()

            // Only disable depth test temporarily
            val depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
            if (depthEnabled) {
                RenderSystem.disableDepthTest()
            }

            // Use the correct shader for 1.21.4
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

                    // Draw box with proper color attributes
                    val tessellator = RenderSystem.renderThreadTesselator()
                    val buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

                    drawBoxFacesWithColor(box, matrixStack, buffer, r, g, b, a)

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
                    RenderSystem.lineWidth(lineWidth.coerceIn(1F, 10F))

                    // Draw box outline with color
                    val tessellator = RenderSystem.renderThreadTesselator()
                    val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

                    drawBoxOutlineWithColor(box, matrixStack, buffer, r, g, b, a)

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
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        } catch (e: Exception) {
            // Graceful error handling
            println("Error in Block ESP rendering: ${e.message}")
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
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)

// Top face
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)

// Front face
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)

// Back face
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)

// Left face
        buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a)

// Right face
        buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a)
        buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a)

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