package com.dov.cm.modules

import com.dov.cm.config.Config
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.entity.LivingEntity
import net.minecraft.util.ActionResult

class TargetHUD : ClientModInitializer {
    private var target: LivingEntity? = null
    private var lastHitTime: Long = 0
    private val hudConfig = Config

    // Define minimum box width
    private val MIN_BOX_WIDTH = 80

    override fun onInitializeClient() {
        AttackEntityCallback.EVENT.register { _, _, _, entity, _ ->
            if (entity is LivingEntity && entity.isAlive) {
                target = entity
                lastHitTime = System.currentTimeMillis()
            }
            ActionResult.PASS
        }

        ClientTickEvents.END_CLIENT_TICK.register {
            val mc = MinecraftClient.getInstance()
            val player = mc.player ?: return@register

            if (mc.currentScreen is ChatScreen) {
                target = player
                lastHitTime = System.currentTimeMillis()
            } else {
                if (System.currentTimeMillis() - lastHitTime > 2000) {
                    target = null
                }
            }

            if (target != null && !target!!.isAlive) {
                target = null
            }
        }

        HudRenderCallback.EVENT.register { context, _ ->
            if (hudConfig.targetHudToggled && target != null) {
                render(context, target!!, hudConfig.offsetX, hudConfig.offsetY, hudConfig.background)
            }
        }
    }

    private fun render(context: DrawContext, entity: LivingEntity, xOffset: Int, yOffset: Int, opacity: Float) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return
        val textRenderer: TextRenderer = mc.textRenderer

        val health = entity.health.coerceAtLeast(0f) / 2
        val maxHealth = entity.maxHealth / 2
        val playerHealth = player.health / 2
        val healthDiff = playerHealth - health

        val healthColor = interpolateColor(health / maxHealth)
        val (status, statusColor) = when {
            healthDiff > 0 -> "W" to 0x00FF00
            healthDiff < 0 -> "L" to 0xFF0000
            else -> "D" to 0xFFFF00
        }

        // Calculate dimensions
        val nameWidth = textRenderer.getWidth(entity.name.string)
        val statusWidth = textRenderer.getWidth(status)
        val healthText = "%.1f❤".format(health)
        textRenderer.getWidth(healthText)
        val healthDiffText = when {
            healthDiff > 0 -> "+%.1f".format(healthDiff)
            healthDiff < 0 -> "%.1f".format(healthDiff)
            else -> "0.0"
        }
        val healthDiffWidth = textRenderer.getWidth(healthDiffText)

        // Box width: max(name + 10px + status, MIN_BOX_WIDTH)
        val boxWidth = maxOf(nameWidth + 10 + statusWidth, MIN_BOX_WIDTH)

        // Box height: name height + 8px + health text height + 8px + bar height
        val nameHeight = textRenderer.fontHeight
        val healthTextHeight = textRenderer.fontHeight
        val barHeight = 3 // Bar height
        val boxHeight = nameHeight + 6 + healthTextHeight + 3 + barHeight

        val x = mc.window.scaledWidth / 2 + xOffset
        val y = mc.window.scaledHeight / 2 + yOffset

        // Draw the box
        drawRoundedBox(context, x, y, boxWidth, boxHeight, opacity)
        drawRoundedOutline(context, x, y, boxWidth, boxHeight, 0xFFFFFFFF.toInt())

        // Draw the target's name (left-aligned)
        context.drawText(textRenderer, entity.name.string, x + 5, y + 5, 0xFFFFFF, true)

        // Draw the status (right-aligned)
        context.drawText(textRenderer, status, x + boxWidth - statusWidth - 5, y + 5, statusColor, true)

        // Draw the health text (left-aligned, below the name)
        context.drawText(textRenderer, healthText, x + 5, y + nameHeight + 5, healthColor, true)

        // Draw the health difference (right-aligned, below the status)
        context.drawText(textRenderer, healthDiffText, x + boxWidth - healthDiffWidth - 5, y + nameHeight + 5, statusColor, true)

        // Draw the health bar (centered, at the bottom)
        val barWidth = boxWidth - 10 // 5px padding on each side
        val barX = x + 5
        val barY = y + nameHeight + 6 + healthTextHeight
        drawHealthBar(context, barX, barY, health, maxHealth, healthColor, barWidth, barHeight)
    }

    private fun drawRoundedBox(context: DrawContext, x: Int, y: Int, width: Int, height: Int, opacity: Float) {
        val alpha = (opacity * 255).toInt() and 0xFF
        val backgroundColor = (alpha shl 24) or 0x000000
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor)
    }

    private fun drawRoundedOutline(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        context.fill(x + 1, y, x + width - 1, y + 1, color)
        context.fill(x, y + 1, x + 1, y + height - 1, color)
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, color)
    }
    private var animatedHealth: Float = 0f
    private fun drawHealthBar(context: DrawContext, x: Int, y: Int, health: Float, maxHealth: Float, color: Int, width: Int, height: Int) {
        animatedHealth = if (Config.animations) {
            animatedHealth + (health - animatedHealth) * 0.1f
        } else {
            health
        }
        // Smoothly transition the health value
        animatedHealth += (health - animatedHealth) * 0.1f // Adjust the 0.1f for speed (higher = faster)

        // Darken the given color slightly for the background
        val darkerColor = darkenColor(color, 0.6f)

        // Draw the background of the health bar (darker version of the health color)
        context.fill(x, y, x + width, y + height, darkerColor)

        // Draw the filled portion of the health bar (smooth animation)
        val barWidth = ((animatedHealth / maxHealth) * width).toInt()
        context.fill(x, y, x + barWidth, y + height, color)
    }
    // Function to darken a given color
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor

        return (a shl 24) or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF)
    }


    private fun interpolateColor(factor: Float): Int {
        val clampedFactor = factor.coerceIn(0f, 1f) // Ensure factor is between 0 and 1

        // Define colors for gradient (Red -> Orange -> Yellow -> Green)
        val startColors = intArrayOf(0xFFFF0000.toInt(), 0xFFFFA500.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt())
        val positions = floatArrayOf(0f, 0.33f, 0.66f, 1f) // Transition points

        // Find the two closest colors in the gradient
        for (i in 0 until positions.lastIndex) {
            if (clampedFactor in positions[i]..positions[i + 1]) {
                val t = (clampedFactor - positions[i]) / (positions[i + 1] - positions[i])
                return lerpColor(startColors[i], startColors[i + 1], t)
            }
        }
        return startColors.last() // Default to green if something goes wrong
    }

    // Linear interpolation between two colors
    private fun lerpColor(color1: Int, color2: Int, t: Float): Int {
        val a1 = (color1 shr 24) and 0xFF
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF

        val a2 = (color2 shr 24) and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF

        val a = (a1 + (a2 - a1) * t).toInt()
        val r = (r1 + (r2 - r1) * t).toInt()
        val g = (g1 + (g2 - g1) * t).toInt()
        val b = (b1 + (b2 - b1) * t).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

}