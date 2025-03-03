package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Utility class for rendering entity ESP in Minecraft with Kotlin
 */
object EntityEspUtil {
    private val MC = MinecraftClient.getInstance()

    /**
     * Renders ESP around an entity with the given color
     *
     * @param entity Entity to render ESP around
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     * @param expand How much to expand the entity hitbox for ESP (default 0)
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
        // Get entity's bounding box and expand if needed
        val box = entity.boundingBox.expand(expand.toDouble())

        // Render the box
        renderBox(box, color, lineWidth, filled, outlined, fillOpacity)
    }

    /**
     * Renders ESP around all entities of the specified type
     *
     * @param entityType Type of entity to render ESP around
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param maxDistance Maximum distance to render ESP
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     * @param expand How much to expand the entity hitbox for ESP
     */
    fun renderAllEntities(
        entityType: EntityType<*>,
        color: Color,
        maxDistance: Double = 64.0,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f,
        expand: Float = 0f
    ) {
        val player = MC.player ?: return
        val world = MC.world ?: return

        world.entities.forEach { entity ->
            if (entity.type == entityType && entity != player && player.distanceTo(entity) <= maxDistance) {
                renderEntity(entity, color, lineWidth, filled, outlined, fillOpacity, expand)
            }
        }
    }

    /**
     * Renders ESP around all players
     *
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param maxDistance Maximum distance to render ESP
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     * @param includeSelf Whether to include the client player
     * @param expand How much to expand the entity hitbox for ESP
     */
    fun renderAllPlayers(
        color: Color,
        maxDistance: Double = 64.0,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f,
        includeSelf: Boolean = false,
        expand: Float = 0f
    ) {
        val player = MC.player ?: return
        val world = MC.world ?: return

        world.players.forEach { entity ->
            if ((includeSelf || entity != player) && player.distanceTo(entity) <= maxDistance) {
                renderEntity(entity, color, lineWidth, filled, outlined, fillOpacity, expand)
            }
        }
    }

    /**
     * Renders ESP around all living entities (mobs)
     *
     * @param color RGBA color for the ESP (java.awt.Color)
     * @param maxDistance Maximum distance to render ESP
     * @param lineWidth Width of lines for outlined rendering
     * @param filled Whether to render filled boxes
     * @param outlined Whether to render outlined boxes
     * @param fillOpacity Opacity for filled boxes (0.0f to 1.0f)
     * @param includePlayers Whether to include players
     * @param expand How much to expand the entity hitbox for ESP
     */
    fun renderAllLivingEntities(
        color: Color,
        maxDistance: Double = 64.0,
        lineWidth: Float = 2f,
        filled: Boolean = false,
        outlined: Boolean = true,
        fillOpacity: Float = 0.25f,
        includePlayers: Boolean = false,
        expand: Float = 0f
    ) {
        val player = MC.player ?: return
        val world = MC.world ?: return

        world.entities.forEach { entity ->
            if (entity is LivingEntity &&
                entity != player &&
                (includePlayers || entity !is PlayerEntity) &&
                player.distanceTo(entity) <= maxDistance
            ) {
                renderEntity(entity, color, lineWidth, filled, outlined, fillOpacity, expand)
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
        // Get render context
        val matrixStack = MatrixStack()
        val camera = MC.gameRenderer.camera ?: return

        // Apply camera offset
        val camPos = getCameraPos(camera)
        matrixStack.push()
        matrixStack.translate((-camPos.x).toFloat(), (-camPos.y).toFloat(), (-camPos.z).toFloat())

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
            GL11.glLineWidth(lineWidth)
            drawOutlinedBox(box, matrixStack)
            resetOutlineRendering()
        }

        // Reset rendering state
        matrixStack.pop()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }

    /**
     * Render 2D tracers from screen center to entities
     */
    fun renderTracer(
        entity: Entity,
        color: Color,
        lineWidth: Float = 1f
    ) {
        val player = MC.player ?: return
        val camera = MC.gameRenderer.camera ?: return

        // Get entity position
        val entityPos = entity.pos.add(0.0, entity.height / 2.0, 0.0)

        // Get screen center
        val screenWidth = MC.window.scaledWidth
        val screenHeight = MC.window.scaledHeight
        val screenCenter = Vec3d(screenWidth / 2.0, screenHeight / 2.0, 0.0)

        // Get camera position
        val cameraPos = camera.pos

        // Setup rendering
        val matrixStack = MatrixStack()
        matrixStack.push()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()

        // Set line width
        GL11.glLineWidth(lineWidth)

        // Set color
        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f
        RenderSystem.setShaderColor(r, g, b, a)

        // Draw line from screen center to entity
        val tessellator = RenderSystem.renderThreadTesselator()
        val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION)

        buffer.vertex(matrixStack.peek().positionMatrix, screenCenter.x.toFloat(), screenCenter.y.toFloat(), 0f)

        // Calculate projected position
        val relativePos = entityPos.subtract(cameraPos)
        buffer.vertex(matrixStack.peek().positionMatrix, relativePos.x.toFloat(), relativePos.y.toFloat(), relativePos.z.toFloat())

        BufferRenderer.drawWithGlobalProgram(buffer.end())

        // Reset rendering state
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        matrixStack.pop()
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