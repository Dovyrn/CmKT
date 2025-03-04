package com.dov.cm.util

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * Utility class for rendering 2D tracers to entities from screen center
 */
object EntityTracerUtil {
    private val MC = MinecraftClient.getInstance()

    /**
     * Renders a 2D tracer line from screen center to entity
     *
     * @param entity Entity to trace to
     * @param color RGBA color for the tracer line
     * @param lineWidth Width of the tracer line
     */
    fun renderTracer(
        entity: Entity,
        color: Color,
        lineWidth: Float = 1.5f
    ) {
        // Calculate the entity's eye position (or center for non-living entities)
        val targetPos = if (entity is LivingEntity) {
            // For living entities, trace to the head/eye level
            entity.eyePos
        } else {
            // For other entities, trace to center
            entity.pos.add(0.0, entity.height / 2.0, 0.0)
        }

        render2DTracer(targetPos, color, lineWidth)
    }

    /**
     * Renders tracers to all entities of a specific type
     *
     * @param entityType Type of entities to trace to
     * @param color RGBA color for the tracer lines
     * @param lineWidth Width of the tracer lines
     * @param maxDistance Maximum distance to render tracers
     */
    fun renderTracersToAllEntities(
        entityType: EntityType<*>,
        color: Color,
        lineWidth: Float = 1.5f,
        maxDistance: Double = 64.0
    ) {
        val player = MC.player ?: return
        val world = MC.world ?: return

        world.entities.forEach { entity ->
            if (entity.type == entityType &&
                entity != player &&
                player.distanceTo(entity) <= maxDistance) {

                renderTracer(entity, color, lineWidth)
            }
        }
    }

    /**
     * Renders tracers to all players
     *
     * @param color RGBA color for the tracer lines
     * @param lineWidth Width of the tracer lines
     * @param maxDistance Maximum distance to render tracers
     * @param includeSelf Whether to include the client player
     */
    fun renderTracersToAllPlayers(
        color: Color,
        lineWidth: Float = 1.5f,
        maxDistance: Double = 64.0,
        includeSelf: Boolean = false
    ) {
        val player = MC.player ?: return
        val world = MC.world ?: return

        world.players.forEach { entity ->
            if ((includeSelf || entity != player) &&
                player.distanceTo(entity) <= maxDistance) {

                renderTracer(entity, color, lineWidth)
            }
        }
    }

    /**
     * Render a 2D tracer from screen center to world position
     */
    private fun render2DTracer(
        targetPos: Vec3d,
        color: Color,
        lineWidth: Float
    ) {
        val player = MC.player ?: return

        // Only render if we're on the render thread
        if (!RenderSystem.isOnRenderThread()) {
            return
        }

        // Check distance - don't render if too far
        val maxDistance = MC.options.viewDistance.value * 16.0
        if (player.pos.squaredDistanceTo(targetPos) > maxDistance * maxDistance) {
            return
        }

        try {
            // Get the target position in screen space
            val screenCoords = worldToScreen(targetPos) ?: return

            // Get screen dimensions
            val screenWidth = MC.window.scaledWidth
            val screenHeight = MC.window.scaledHeight

            // Calculate screen center
            val centerX = screenWidth / 2f
            val centerY = screenHeight / 2f

            // Create and set up matrix stack for 2D rendering
            val matrixStack = MatrixStack()
            matrixStack.push()

            // Set up rendering state for 2D
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableDepthTest()
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
            RenderSystem.lineWidth(lineWidth.coerceIn(0.5f, 5.0f)) // Properly respect line width

            // Extract color components
            val r = color.red / 255f
            val g = color.green / 255f
            val b = color.blue / 255f
            val a = color.alpha / 255f

            // Draw the line using a vertex buffer
            val tessellator = RenderSystem.renderThreadTesselator()
            val buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

            // Start vertex (screen center)
            buffer.vertex(matrixStack.peek().positionMatrix, centerX, centerY, 0f)
                .color(r, g, b, a)

            // End vertex (projected entity position)
            buffer.vertex(matrixStack.peek().positionMatrix,
                screenCoords.x.toFloat(),
                screenCoords.y.toFloat(),
                0f) // Z coordinate is 0 for 2D
                .color(r, g, b, a)

            // Draw the line
            BufferRenderer.drawWithGlobalProgram(buffer.end())

            // Restore rendering state
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
            RenderSystem.lineWidth(1.0f)

            // Pop matrix stack
            matrixStack.pop()

        } catch (e: Exception) {
            // Handle errors silently
        }
    }

    /**
     * Convert a 3D world position to 2D screen coordinates
     * Returns null if the position is not visible on screen
     */
    private fun worldToScreen(worldPos: Vec3d): Vec3d? {
        // This projection logic is implemented to work with Minecraft 1.21.4
        try {
            val camera = MC.gameRenderer.camera ?: return null

            // Get view matrix properties
// Get view matrix properties
            val viewDist = MC.options.viewDistance.value * 16.0

            // Check if the point is in front of the camera
            val cameraPosToTarget = worldPos.subtract(camera.pos)

            // Use the camera's rotation vector (calculated from yaw and pitch)
            val cameraYaw = Math.toRadians(-camera.yaw - 180.0)
            val cameraPitch = Math.toRadians((-camera.pitch).toDouble())

            // Create a normalized vector for the camera facing direction
            val cameraVecX = Math.sin(cameraYaw) * Math.cos(cameraPitch)
            val cameraVecY = Math.sin(cameraPitch)
            val cameraVecZ = Math.cos(cameraYaw) * Math.cos(cameraPitch)

            // Calculate dot product to check if in front of camera
            val dot = cameraPosToTarget.x * cameraVecX +
                    cameraPosToTarget.y * cameraVecY +
                    cameraPosToTarget.z * cameraVecZ

            // If behind camera, don't render
            if (dot < 0) return null

            // If behind camera, don't render
            if (dot < 0) return null

            // Project the 3D point onto 2D screen space
            val modelView = RenderSystem.getModelViewMatrix()
            val projection = RenderSystem.getProjectionMatrix()

            // Get viewport dimensions
            val viewport = intArrayOf(0, 0, MC.window.framebufferWidth, MC.window.framebufferHeight)

            // Get window dimensions and scale factor
            val window = MC.window
            val scaleFactor = window.scaleFactor

            // Use a simpler projection calculation
            // Convert world position to view space
            val viewPos = worldPos.subtract(camera.pos)

            // Rotate by camera view matrix
            val yaw = Math.toRadians(-camera.yaw - 180.0)
            val pitch = Math.toRadians((-camera.pitch).toDouble())

            val rotatedX = viewPos.x * Math.cos(yaw) - viewPos.z * Math.sin(yaw)
            val rotatedZ = viewPos.x * Math.sin(yaw) + viewPos.z * Math.cos(yaw)
            val rotatedY = viewPos.y * Math.cos(pitch) + rotatedZ * Math.sin(pitch)
            val finalZ = -viewPos.y * Math.sin(pitch) + rotatedZ * Math.cos(pitch)

            // Skip if behind camera
            if (finalZ < 0.1) return null

            // Convert to screen space
            val fov = Math.toRadians(MC.options.fov.value.toDouble())
            val aspectRatio = window.scaledWidth.toFloat() / window.scaledHeight.toFloat()

            val scaleZ = Math.tan(fov / 2.0) * finalZ
            val screenX = window.scaledWidth / 2.0 + rotatedX / scaleZ / aspectRatio * window.scaledHeight
            val screenY = window.scaledHeight / 2.0 - rotatedY / scaleZ * window.scaledHeight

            // Make sure the point is in screen bounds
            if (screenX < 0 || screenX > window.scaledWidth || screenY < 0 || screenY > window.scaledHeight) {
                // Clamp to screen edges to show direction
                val clampedX = screenX.coerceIn(0.0, window.scaledWidth.toDouble())
                val clampedY = screenY.coerceIn(0.0, window.scaledHeight.toDouble())
                return Vec3d(clampedX, clampedY, 0.0)
            }

            return Vec3d(screenX, screenY, 0.0)

        } catch (e: Exception) {
            // Return null if any errors occur during projection
            return null
        }
    }
}