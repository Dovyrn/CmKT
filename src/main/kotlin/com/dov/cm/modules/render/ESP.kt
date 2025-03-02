package com.dov.cm.modules.render

import com.dov.cm.config.Config
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Box
import org.lwjgl.opengl.GL11
import org.joml.Vector4f
import org.joml.Vector3f
import java.awt.Color

object ESP {
    // ESP Configuration
    private var enabled: Boolean = Config.espEnabled
    private var espType: ESPType = ESPType.values()[Config.espType]
    private var renderDistance: Double = Config.espRenderDistance.toDouble()
    private var lineWidth: Float = Config.espLineWidth.toFloat()

    // Color Settings
    private var playerColor: Color = Config.espPlayerColor
    private var mobColor: Color = Config.espMobColor
    private var itemColor: Color = Config.espItemColor

    // Toggles
    private var renderPlayers: Boolean = Config.espRenderPlayers
    private var renderMobs: Boolean = Config.espRenderMobs
    private var renderItems: Boolean = Config.espRenderItems
    private var renderInvisible: Boolean = Config.espRenderInvisible
    private var renderSelf: Boolean = Config.espRenderSelf
    private var rainbowColor: Boolean = Config.espRainbowColor

    // Enum for ESP Rendering Types
    enum class ESPType {
        BOX, OUTLINE, TWO_D, HEALTH_BAR, SHADED, RING
    }

    fun init() {
        // Register render events
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick)
        HudRenderCallback.EVENT.register { context, tickCounter ->
            onHudRender(context, tickCounter)
        }

        // Initial configuration sync
        syncConfigSettings()
    }

    private fun syncConfigSettings() {
        enabled = Config.espEnabled
        espType = ESPType.values()[Config.espType]
        renderDistance = Config.espRenderDistance.toDouble()
        lineWidth = Config.espLineWidth.toFloat()

        playerColor = Config.espPlayerColor
        mobColor = Config.espMobColor
        itemColor = Config.espItemColor

        renderPlayers = Config.espRenderPlayers
        renderMobs = Config.espRenderMobs
        renderItems = Config.espRenderItems
        renderInvisible = Config.espRenderInvisible
        renderSelf = Config.espRenderSelf
        rainbowColor = Config.espRainbowColor
    }

    private fun onClientTick(client: MinecraftClient) {
        // Sync configuration on each tick to allow real-time updates
        syncConfigSettings()

        // Optional: Update rainbow color
        if (rainbowColor) {
            updateRainbowColors()
        }
    }

    private fun onHudRender(context: DrawContext, tickCounter: RenderTickCounter) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (!enabled) return

        // Find and render entities
        mc.world?.entities?.forEach { entity ->
            if (shouldRenderEntity(player, entity)) {
                renderESP(context, entity, tickCounter.getTickDelta(true))
            }
        }
    }

    private fun shouldRenderEntity(selfPlayer: PlayerEntity, entity: Entity): Boolean {
        return when {
            entity == selfPlayer && !renderSelf -> false
            entity is PlayerEntity -> renderPlayers
            entity is LivingEntity && entity !is PlayerEntity -> renderMobs
            else -> renderItems
        } && (renderInvisible || !entity.isInvisible)
                && selfPlayer.distanceTo(entity) <= renderDistance
    }

    private fun renderESP(context: DrawContext, entity: Entity, partialTicks: Float) {
        val mc = MinecraftClient.getInstance()

        // Calculate render positions
        val renderX = mc.entityRenderDispatcher.camera.pos.x
        val renderY = mc.entityRenderDispatcher.camera.pos.y
        val renderZ = mc.entityRenderDispatcher.camera.pos.z
        val factor = mc.window.scaleFactor.toInt()

        // Calculate screen position
        val pos = calc(entity, partialTicks, renderX, renderY, renderZ, factor)

        // Skip if not visible
        if (pos == null || pos.x == 0f && pos.y == 0f && pos.z == 0f && pos.w == 0f) return

        val color = determineColor(entity)

        // Render ESP box
        drawESPBox(pos, color)
    }

    private fun calc(entity: Entity, partialTicks: Float, renderX: Double, renderY: Double, renderZ: Double, factor: Int): Vector4f? {
        // Interpolate entity position
        val x = entity.lastRenderX + (entity.x - entity.lastRenderX) * partialTicks - renderX
        val y = entity.lastRenderY + (entity.y - entity.lastRenderY) * partialTicks - renderY
        val z = entity.lastRenderZ + (entity.z - entity.lastRenderZ) * partialTicks - renderZ

        // Calculate bounding box
        val width = (entity.width + 0.2) / 2
        val height = entity.height + (if (entity.isSneaking) -0.3f else 0.2f) + 0.05

        val aabb = Box(x - width, y, z - width, x + width, y + height, z + width)

        // Project corners
        val vectors = listOf(
            Vector3f(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat()),
            Vector3f(aabb.minX.toFloat(), aabb.maxY.toFloat(), aabb.minZ.toFloat()),
            Vector3f(aabb.maxX.toFloat(), aabb.minY.toFloat(), aabb.minZ.toFloat()),
            Vector3f(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.minZ.toFloat()),
            Vector3f(aabb.minX.toFloat(), aabb.minY.toFloat(), aabb.maxZ.toFloat()),
            Vector3f(aabb.minX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat()),
            Vector3f(aabb.maxX.toFloat(), aabb.minY.toFloat(), aabb.maxZ.toFloat()),
            Vector3f(aabb.maxX.toFloat(), aabb.maxY.toFloat(), aabb.maxZ.toFloat())
        )

        var position: Vector4f? = null

        for (vector in vectors) {
            val projectedVector = project(factor, vector.x, vector.y, vector.z) ?: continue

            if (projectedVector.z < 0.0 || projectedVector.z >= 1.0) continue

            position = position?.let {
                Vector4f(
                    minOf(projectedVector.x, it.x),
                    minOf(projectedVector.y, it.y),
                    maxOf(projectedVector.x, it.z),
                    maxOf(projectedVector.y, it.w)
                )
            } ?: Vector4f(projectedVector.x, projectedVector.y, projectedVector.x, projectedVector.y)
        }

        return position ?: Vector4f(0f, 0f, 0f, 0f)
    }

    private fun project(factor: Int, x: Float, y: Float, z: Float): Vector3f? {
        // Implement a simplified projection method
        val mc = MinecraftClient.getInstance()
        val windowHeight = mc.window.height

        // Basic perspective projection
        val projX = x * factor
        val projY = windowHeight - (y * factor)

        return Vector3f(projX, projY, 0f)
    }

    private fun drawESPBox(pos: Vector4f, color: Color) {
        // Draw ESP box
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        // Black outline
        drawRectangle(pos.x, pos.y, pos.z - pos.x, 1.5, Color.BLACK) // Top
        drawRectangle(pos.x, pos.y, 1.5F, pos.w - pos.y + 1.5, Color.BLACK) // Left
        drawRectangle(pos.z, pos.y, 1.5F, pos.w - pos.y + 1.5, Color.BLACK) // Right
        drawRectangle(pos.x, pos.w, pos.z - pos.x, 1.5, Color.BLACK) // Bottom

        // Gradient fills
        drawHorizontalGradient(pos.x + 0.5, pos.y + 0.5, pos.z - pos.x, 0.5,
            color.rgb, color.darker().rgb) // Top
        drawVerticalGradient(pos.x + 0.5, pos.y + 0.5, 0.5, pos.w - pos.y + 0.5,
            color.rgb, color.darker().rgb) // Left
        drawVerticalGradient(pos.z + 0.5, pos.y + 0.5, 0.5, pos.w - pos.y + 0.5,
            color.darker().rgb, color.rgb) // Right
        drawHorizontalGradient(pos.x + 0.5, pos.w + 0.5, pos.z - pos.x, 0.5,
            color.darker().rgb, color.rgb) // Bottom

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun drawRectangle(x: Float, y: Float, width: Float, height: Double, color: Color) {
        GL11.glColor4f(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            color.alpha / 255f
        )
        GL11.glBegin(GL11.GL_QUADS)
        GL11.glVertex2d(x.toDouble(), y.toDouble())
        GL11.glVertex2d((x + width).toDouble(), y.toDouble())
        GL11.glVertex2d((x + width).toDouble(), y + height)
        GL11.glVertex2d(x.toDouble(), y + height)
        GL11.glEnd()
    }

    private fun drawHorizontalGradient(x: Double, y: Double, width: Float, height: Double, leftColor: Int, rightColor: Int) {
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glBegin(GL11.GL_QUADS)

        setGLColor(leftColor)
        GL11.glVertex2d(x, y)
        GL11.glVertex2d(x, y + height)

        setGLColor(rightColor)
        GL11.glVertex2d(x + width, y + height)
        GL11.glVertex2d(x + width, y)

        GL11.glEnd()
        GL11.glShadeModel(GL11.GL_FLAT)
    }

    private fun drawVerticalGradient(x: Double, y: Double, width: Double, height: Double, topColor: Int, bottomColor: Int) {
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glBegin(GL11.GL_QUADS)

        setGLColor(topColor)
        GL11.glVertex2d(x, y)
        GL11.glVertex2d(x + width, y)

        setGLColor(bottomColor)
        GL11.glVertex2d(x + width, y + height)
        GL11.glVertex2d(x, y + height)

        GL11.glEnd()
        GL11.glShadeModel(GL11.GL_FLAT)
    }

    private fun setGLColor(color: Int) {
        val a = (color shr 24 and 0xFF) / 255f
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        GL11.glColor4f(r, g, b, a)
    }

    private fun determineColor(entity: Entity): Color {
        return when {
            rainbowColor -> Render2DEngine.rainbow(2, entity.id)
            entity is PlayerEntity -> playerColor
            entity is LivingEntity && entity !is PlayerEntity -> mobColor
            else -> itemColor
        }
    }

    private fun updateRainbowColors() {
        playerColor = Render2DEngine.rainbow(2, 0)
        mobColor = Render2DEngine.rainbow(2, 1)
        itemColor = Render2DEngine.rainbow(2, 2)

        // Update config colors if rainbow is enabled
        if (rainbowColor) {
            Config.espPlayerColor = playerColor
            Config.espMobColor = mobColor
            Config.espItemColor = itemColor
            Config.markDirty()
        }
    }
}