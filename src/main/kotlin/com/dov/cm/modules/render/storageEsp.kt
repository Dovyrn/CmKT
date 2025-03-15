package com.dov.cm.modules.render

import com.dov.cm.config.Config
import com.mojang.blaze3d.systems.RenderSystem
import com.dov.cm.util.RenderUtils
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.chunk.WorldChunk
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Storage ESP module for highlighting various storage blocks
 */
object StorageESP {
    private val MC = MinecraftClient.getInstance()
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL = 500L // Update positions every 500ms

    // Queue to store blocks for rendering
    private data class StorageBlockInfo(val pos: BlockPos, val color: Color, val type: StorageType)
    private val storageBlocks = ConcurrentLinkedQueue<StorageBlockInfo>()

    // Define storage block types
    enum class StorageType {
        CHEST, ENDER_CHEST, TRAPPED_CHEST, HOPPER, FURNACE, SHULKER
    }

    /**
     * Initialize the Storage ESP module
     */
    fun init() {
        // At the start of init():
        println("StorageESP init - enabled: ${Config.storageEspEnabled}")

        // Register tick event to update storage positions periodically
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (Config.storageEspEnabled && shouldUpdatePositions()) {
                updateStoragePositions()
            }
        }

        // Register render event - use BEFORE_ENTITIES to make ESP render behind blocks
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            if (Config.storageEspEnabled) {
                context.matrixStack()?.let { renderStorageBlocks(it, context.tickCounter().getTickDelta(true)) }
            }
        }

        // Register after translucent event for tracers
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            if (Config.storageEspEnabled && Config.storageEspTracers) {
                context.matrixStack()?.let { renderStorageTracers(it, context.tickCounter().getTickDelta(true)) }
            }
        }
    }

    /**
     * Check if we should update block positions based on interval
     */
    private fun shouldUpdatePositions(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime > UPDATE_INTERVAL) {
            lastUpdateTime = currentTime
            return true
        }
        return false
    }

    /**
     * Update the list of storage blocks to render
     */
    private fun updateStoragePositions() {
        // Clear the existing list
        storageBlocks.clear()

        val player = MC.player ?: return
        val world = MC.world ?: return

        // Get loaded chunks and find storage blocks
        val renderDistance = MC.options.viewDistance.value
        val playerChunkX = player.blockPos.x shr 4
        val playerChunkZ = player.blockPos.z shr 4
        val maxDistance = renderDistance * 16 // Max distance in blocks

        // Process loaded chunks
        for (chunkX in (playerChunkX - renderDistance)..(playerChunkX + renderDistance)) {
            for (chunkZ in (playerChunkZ - renderDistance)..(playerChunkZ + renderDistance)) {
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                    processChunk(world.getChunk(chunkX, chunkZ), player.blockPos, maxDistance)
                }
            }
        }
    }

    /**
     * Process a single chunk to find storage blocks
     */
    private fun processChunk(chunk: WorldChunk, playerPos: BlockPos, maxDistance: Int) {
        val blockEntities = chunk.blockEntities

        blockEntities.entries.forEach { entry ->
            val pos = entry.key

            // Skip if too far away
            if (playerPos.getSquaredDistance(pos) > maxDistance * maxDistance) {
                return@forEach
            }

            // Get the block at this position
            val blockState = chunk.getBlockState(pos)
            val block = blockState.block

            // Determine block type and color
            when {
                // Regular chests and barrels
                (block == Blocks.CHEST || block == Blocks.BARREL) && Config.chestEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.chestEspColor, StorageType.CHEST))
                }

                // Ender chests
                block == Blocks.ENDER_CHEST && Config.enderChestEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.enderChestEspColor, StorageType.ENDER_CHEST))
                }

                // Trapped chests
                block == Blocks.TRAPPED_CHEST && Config.trappedChestEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.trappedChestEspColor, StorageType.TRAPPED_CHEST))
                }

                // Hoppers
                block == Blocks.HOPPER && Config.hopperEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.hopperEspColor, StorageType.HOPPER))
                }

                // Furnaces (all types)
                (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE ||
                        block == Blocks.SMOKER) && Config.furnaceEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.furnaceEspColor, StorageType.FURNACE))
                }

                // Shulker boxes (all colors)
                block is ShulkerBoxBlock && Config.shulkerEspEnabled -> {
                    storageBlocks.add(StorageBlockInfo(pos, Config.shulkerEspColor, StorageType.SHULKER))
                }
            }
        }
    }

    /**
     * Render ESP for all stored storage blocks
     */
    private fun renderStorageBlocks(matrixStack: MatrixStack, partialTicks: Float) {
        // Skip if no blocks to render
        if (storageBlocks.isEmpty()) return

        // Process each storage block for direct drawing
        storageBlocks.forEach { info ->
            try {
                // Create a box for the block
                val pos = info.pos

                // Create the box with an offset to the camera position to render properly
                val cameraPos = RenderUtils.getCameraPos()
                val offsetX = pos.x - cameraPos.x
                val offsetY = pos.y - cameraPos.y
                val offsetZ = pos.z - cameraPos.z

                val expandedBox = Box(
                    offsetX,
                    offsetY,
                    offsetZ,
                    offsetX + 1.001,
                    offsetY + 1.001,
                    offsetZ + 1.001
                )

                // Push matrix and set color
                matrixStack.push()
                RenderSystem.setShaderColor(
                    info.color.red / 255f,
                    info.color.green / 255f,
                    info.color.blue / 255f,
                    Config.storageEspOpacity / 255f
                )

                // Draw the outlined box
                RenderUtils.drawOutlinedBox(expandedBox, matrixStack)

                // Reset color and pop matrix
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                matrixStack.pop()

            } catch (e: Exception) {
                println(e)
            }
        }
    }

    /**
     * Render tracers for all storage blocks
     */
    private fun renderStorageTracers(matrixStack: MatrixStack, partialTicks: Float) {
        // Skip if no blocks to render or tracers disabled
        if (storageBlocks.isEmpty() || !Config.storageEspTracers) return

        // Setup rendering
        RenderUtils.setupRenderWithShader(matrixStack)

        // Get buffer builder
        val buffer = RenderUtils.getBufferBuilder()
        val matrix = matrixStack.peek().positionMatrix

        // Process each storage block
        storageBlocks.forEach { info ->
            try {
                // Draw tracer from screen center to block using RenderUtils
                RenderUtils.drawSingleLine(
                    buffer,
                    matrix,
                    partialTicks,
                    info.pos,
                    info.color.red / 255f,
                    info.color.green / 255f,
                    info.color.blue / 255f,
                    1.0f
                )
            } catch (e: Exception) {
                // Handle silently
            }
        }

        // Draw the buffer and clean up
        RenderUtils.drawBuffer(buffer, matrixStack)
    }
}