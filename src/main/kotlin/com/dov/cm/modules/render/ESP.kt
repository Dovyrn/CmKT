package com.dov.cm.modules.render

import com.dov.cm.modules.UChat
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.block.entity.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.ChestMinecartEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.chunk.WorldChunk
import com.dov.cm.util.BlockEspUtil
import org.lwjgl.glfw.GLFW
import java.awt.Color

/**
 * ChestESP module for highlighting chests and other storage containers
 */
object ChestEsp {
    private val MC = MinecraftClient.getInstance()
    private val WHITE = Color(255, 255, 255, 180)

    private var enabled = false
    private lateinit var toggleKey: KeyBinding

    private val chestPositions = mutableListOf<BlockPos>()
    private val entityBoxes = mutableListOf<Pair<Box, Color>>()

    fun init() {
        // Register keybinding
        toggleKey = KeyBindingHelper.registerKeyBinding(KeyBinding(
            "key.chestesp.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.chestesp"
        ))

        // Register tick event for toggling and updating chest positions
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Handle toggle
            while (toggleKey.wasPressed()) {
                enabled = !enabled
                if (enabled) {
                    UChat.mChat("ChestESP: §aEnabled")
                } else {
                    UChat.mChat("ChestESP: §cDisabled")
                    chestPositions.clear()
                    entityBoxes.clear()
                }
            }

            // Update chest positions if enabled
            if (enabled) {
                updateChestPositions()
            }
        }

        // Register render event
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            if (enabled && chestPositions.isNotEmpty()) {
                renderChests()
            }
        }

        UChat.mChat("ChestESP module initialized")
    }

    /**
     * Updates the list of chest positions and entity boxes to render
     */
    private fun updateChestPositions() {
        chestPositions.clear()
        entityBoxes.clear()

        val world = MC.world ?: return
        val player = MC.player ?: return

        // Get loaded chunks and find chest block entities
        getLoadedChunks().forEach { chunk ->
            chunk.blockEntities.values.forEach { blockEntity ->
                when (blockEntity) {
                    is ChestBlockEntity,
                    is TrappedChestBlockEntity,
                    is EnderChestBlockEntity,
                    is ShulkerBoxBlockEntity,
                    is BarrelBlockEntity,
                    is HopperBlockEntity -> {
                        chestPositions.add(blockEntity.pos)
                    }
                    else -> {}
                }
            }
        }

        // Find chest entities (minecarts, boats)
        world.entities.forEach { entity ->
            when (entity) {
                is ChestMinecartEntity -> {
                    entityBoxes.add(entity.boundingBox to WHITE)
                }
                is ChestBoatEntity -> {
                    entityBoxes.add(entity.boundingBox to WHITE)
                }
            }
        }
    }

    /**
     * Renders all chests and storage entities
     */
    private fun renderChests() {
        // Render block-based chests
        for (pos in chestPositions) {
            BlockEspUtil.renderBlock(pos, WHITE, 5f, true, true, 0.25F)
        }

        // Render entity chests
        for ((box, color) in entityBoxes) {
            BlockEspUtil.renderBox(box, color, 2f, false, true)
        }
    }

    /**
     * Gets a list of all loaded chunks around the player
     */
    private fun getLoadedChunks(): List<WorldChunk> {
        val player = MC.player ?: return emptyList()
        val world = MC.world ?: return emptyList()

        val viewDistance = MC.options.viewDistance.value
        val chunks = mutableListOf<WorldChunk>()

        val playerChunkX = player.blockPos.x shr 4
        val playerChunkZ = player.blockPos.z shr 4

        for (chunkX in (playerChunkX - viewDistance)..(playerChunkX + viewDistance)) {
            for (chunkZ in (playerChunkZ - viewDistance)..(playerChunkZ + viewDistance)) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    chunks.add(world.getChunk(chunkX, chunkZ))
                }
            }
        }

        return chunks
    }
}